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
 * ----------------
 * Goal: hit ONE shared instance of the service from many threads at the
 * same time and check whether it is actually thread-safe:
 *
 *   1. Short-code uniqueness under contention
 *        Incremental_Strategy uses a plain `static Integer ournumber` with
 *        `ournumber++`. That is a read-modify-write on a non-atomic type -
 *        NOT thread-safe. Under concurrent load two threads can generate
 *        the SAME short code for two DIFFERENT original URLs (lost update).
 *
 *   2. Read-your-write consistency
 *        Url_Shortner_Repository writes to URL_Master synchronously but
 *        replicates to URL_Slave via CompletableFuture.runAsync(...) with
 *        no join/await. get_url() only ever reads from URL_Slave. So a
 *        thread that creates a URL and immediately tries to read it back
 *        can race the async replication and fail even though the URL was
 *        just created successfully.
 *
 * Run:
 *   javac -d out Controller/*.java Repository/*.java Strategy/*.java service/*.java ConcurrencyTest.java
 *   java -cp out ConcurrencyTest [threads] [opsPerThread]
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
        int threadCount = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        int opsPerThread = args.length > 1 ? Integer.parseInt(args[1]) : 500;
        int totalOps = threadCount * opsPerThread;

        System.out.println("============= CONCURRENCY TEST =============");
        System.out.println("Threads          : " + threadCount);
        System.out.println("Ops per thread   : " + opsPerThread);
        System.out.println("Total create ops : " + totalOps);
        System.out.println("==============================================\n");

        // ONE shared service instance - this is the whole point.
        Url_Shortner_Repository storage = new Url_Shortner_Repository();
        Incremental_Strategy strategy = new Incremental_Strategy();
        Url_Shortner_Service service = new Url_Shortner_Service(storage, strategy);
        Url_Shortner_Controller controller = new Url_Shortner_Controller(service);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1); // fire all threads at once
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        Set<String> allShortUrls = ConcurrentHashMap.newKeySet();
        AtomicInteger totalCreateAttempts = new AtomicInteger(0);
        AtomicInteger createExceptions = new AtomicInteger(0);
        AtomicInteger immediateReadFailures = new AtomicInteger(0);
        AtomicInteger immediateReadMismatches = new AtomicInteger(0);
        List<String> collisionLog = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    startGate.await(); // all threads block here, then release together
                    for (int i = 0; i < opsPerThread; i++) {
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
        startGate.countDown(); // release all threads at once
        doneGate.await();      // wait for all threads to finish
        long end = System.nanoTime();
        pool.shutdown();

        double seconds = (end - start) / 1_000_000_000.0;
        double throughput = totalOps / seconds;

        // ---------- Raw numbers ----------
        System.out.println("------------------ Results ------------------");
        System.out.printf("Wall time                 : %.3f s%n", seconds);
        System.out.printf("Throughput                : %.1f creates/sec (across %d threads)%n", throughput, threadCount);
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