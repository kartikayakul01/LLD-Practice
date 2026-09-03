import Controller.Url_Shortner_Controller;
import Repository.Url_Shortner_Repository;
import Strategy.Incremental_Strategy;
import service.Url_Shortner_Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CONCURRENCY TEST
 */
public class ConcurrencyTest {

    // ---------- tiny test-case scoreboard ----------
    private static final List<String> passed = new ArrayList<>();
    private static final List<String> failed = new ArrayList<>();

    private static void check(String name, boolean condition, String detailIfFailed) {
        if (condition) {
            passed.add(name);
            System.out.println("  [PASS] " + name);
        } else {
            failed.add(name + " -> " + detailIfFailed);
            System.out.println("  [FAIL] " + name + "  (" + detailIfFailed + ")");
        }
    }

    public static void main(String[] args) throws Exception {
        int simulatedUsers = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        int opsPerUser = args.length > 1 ? Integer.parseInt(args[1]) : 500;
        int totalOps = simulatedUsers * opsPerUser;

        // --- EDIT 1: Capped Thread Pool Size ---
        int POOL_SIZE = 100;

        System.out.println("============= CONCURRENCY TEST =============");
        System.out.println("Worker Thread Pool Size: " + POOL_SIZE);
        System.out.println("Simulated Virtual Users: " + simulatedUsers);
        System.out.println("Ops per user           : " + opsPerUser);
        System.out.println("Total create ops       : " + totalOps);
        System.out.println("==============================================\n");

        Url_Shortner_Repository storage = new Url_Shortner_Repository();
        Incremental_Strategy strategy = new Incremental_Strategy();
        Url_Shortner_Service service = new Url_Shortner_Service(storage, strategy);
        Url_Shortner_Controller controller = new Url_Shortner_Controller(service);

        // --- EDIT 2: Use fixed 100-thread pool ---
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        CountDownLatch startGate = new CountDownLatch(1);

        // --- EDIT 3: Wait for all simulated user tasks to complete ---
        CountDownLatch doneGate = new CountDownLatch(simulatedUsers);

        Set<String> allShortUrls = ConcurrentHashMap.newKeySet();
        AtomicInteger totalCreateAttempts = new AtomicInteger(0);
        AtomicInteger createExceptions = new AtomicInteger(0);
        AtomicInteger immediateReadFailures = new AtomicInteger(0);
        AtomicInteger immediateReadMismatches = new AtomicInteger(0);
        List<String> collisionLog = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < simulatedUsers; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < opsPerUser; i++) {
                        String original = "https://example.com/thread" + threadId + "/item" + i;
                        totalCreateAttempts.incrementAndGet();
                        try {
                            String shortUrl = controller.create_url(original);

                            boolean isNew = allShortUrls.add(shortUrl);
                            if (!isNew) {
                                collisionLog.add("COLLISION on " + shortUrl
                                        + " while creating " + original);
                            }

                            try {
                                String resolved = controller.read_url(shortUrl);
                                if (!original.equals(resolved)) {
                                    immediateReadMismatches.incrementAndGet();
                                }
                            } catch (Exception readEx) {
                                immediateReadFailures.incrementAndGet();
                            }

                        } catch (Exception createEx) {
                            createExceptions.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown();
        doneGate.await();
        long end = System.nanoTime();
        pool.shutdown();

        double seconds = (end - start) / 1_000_000_000.0;
        double throughput = totalOps / seconds;

        // ---------- Raw numbers ----------
        System.out.println("------------------ Results ------------------");
        System.out.printf("Wall time                 : %.3f s%n", seconds);
        System.out.printf("Throughput                : %.1f creates/sec (across %d thread pool)%n", throughput, POOL_SIZE);
        System.out.println("Total create attempts     : " + totalCreateAttempts.get());
        System.out.println("Create exceptions         : " + createExceptions.get());
        System.out.println("Unique short URLs produced: " + allShortUrls.size());
        System.out.println("Short-code collisions     : " + collisionLog.size());
        System.out.println("Immediate read failures   : " + immediateReadFailures.get());
        System.out.println("Immediate read mismatches : " + immediateReadMismatches.get());

        if (!collisionLog.isEmpty()) {
            System.out.println("\nSample collisions (first 5):");
            collisionLog.stream().limit(5).forEach(c -> System.out.println("  " + c));
        }

        // ---------- Named test cases ----------
        System.out.println("\n---------------- TEST CASES ----------------");
        check("TC1: no exceptions thrown while creating under concurrency",
                createExceptions.get() == 0,
                createExceptions.get() + " / " + totalCreateAttempts.get() + " create() calls threw");

        check("TC2: no two threads generated the same short code (counter is thread-safe)",
                collisionLog.isEmpty(),
                collisionLog.size() + " collision(s) - static counter increment is racy");

        check("TC3: unique short URLs produced == total successful creates",
                allShortUrls.size() == totalCreateAttempts.get() - createExceptions.get(),
                allShortUrls.size() + " unique vs " + (totalCreateAttempts.get() - createExceptions.get()) + " expected");

        check("TC4: a URL is immediately readable right after its own thread created it",
                immediateReadFailures.get() == 0,
                immediateReadFailures.get() + " / " + totalCreateAttempts.get()
                        + " read-your-write checks failed - master/slave replication race");

        check("TC5: reads that succeed return the correct original URL",
                immediateReadMismatches.get() == 0,
                immediateReadMismatches.get() + " reads returned the WRONG original URL");

        // ---------- Scoreboard ----------
        int total = passed.size() + failed.size();
        System.out.println("\n=============== TEST SUMMARY ===============");
        System.out.println("Passed : " + passed.size() + " / " + total);
        System.out.println("Failed : " + failed.size() + " / " + total);
        if (!failed.isEmpty()) {
            System.out.println("\nFailed test cases:");
            for (String f : failed) System.out.println("  - " + f);
        }
        System.out.println("==============================================");
        System.out.println(failed.isEmpty()
                ? "VERDICT: PASS - service is thread-safe under concurrent load"
                : "VERDICT: FAIL - " + failed.size() + " test case(s) failed - service is NOT thread-safe");
        System.out.println("==============================================");

        if (!failed.isEmpty()) System.exit(1);
    }
}