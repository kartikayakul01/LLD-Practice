import Controller.Url_Shortner_Controller;
import Repository.Url_Shortner_Repository;
import Strategy.Incremental_Strategy;
import service.Url_Shortner_Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LOAD TEST
 * ---------
 * Goal: hammer the shortener with a large, sequential volume of requests
 * (single thread, no concurrency involved) and check whether it stays
 * correct and fast as N grows large - the way one busy client hitting the
 * service repeatedly would.
 *
 * This does NOT test thread-safety (see ConcurrencyTest.java for that).
 *
 * Run:
 *   javac -d out Controller/*.java Repository/*.java Strategy/*.java service/*.java LoadTest.java
 *   java -cp out LoadTest [numRequests]
 */
public class LoadTest {

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
        int totalRequests = args.length > 0 ? Integer.parseInt(args[0]) : 50_000;

        System.out.println("================ LOAD TEST ================");
        System.out.println("Requests to run  : " + totalRequests);
        System.out.println("Mode             : single-threaded, sequential");
        System.out.println("=============================================\n");

        Url_Shortner_Repository storage = new Url_Shortner_Repository();
        Incremental_Strategy strategy = new Incremental_Strategy();
        Url_Shortner_Service service = new Url_Shortner_Service(storage, strategy);
        Url_Shortner_Controller controller = new Url_Shortner_Controller(service);

        Set<String> seenShortUrls = new HashSet<>();
        String[] createdShortUrls = new String[totalRequests];
        String[] originalUrls = new String[totalRequests];

        int duplicateShortUrls = 0;
        int createFailures = 0;

        // ---------- Phase 1: CREATE load ----------
        long createStart = System.nanoTime();
        for (int i = 0; i < totalRequests; i++) {
            String original = "https://example.com/resource/" + i;
            originalUrls[i] = original;
            try {
                String shortUrl = controller.create_url(original);
                createdShortUrls[i] = shortUrl;
                if (!seenShortUrls.add(shortUrl)) {
                    duplicateShortUrls++;
                }
            } catch (Exception e) {
                createFailures++;
                createdShortUrls[i] = null;
            }
        }
        long createEnd = System.nanoTime();

        double createSeconds = (createEnd - createStart) / 1_000_000_000.0;
        double createThroughput = totalRequests / createSeconds;
        double createAvgLatencyMs = (createSeconds * 1000.0) / totalRequests;

        // ---------- Phase 2: READ load (read back everything we just created) ----------
        int readFailures = 0;
        int readMismatches = 0;

        long readStart = System.nanoTime();
        for (int i = 0; i < totalRequests; i++) {
            if (createdShortUrls[i] == null) continue; // skip failed creates
            try {
                String resolved = controller.read_url(createdShortUrls[i]);
                if (!originalUrls[i].equals(resolved)) {
                    readMismatches++;
                }
            } catch (Exception e) {
                readFailures++;
            }
        }
        long readEnd = System.nanoTime();

        double readSeconds = (readEnd - readStart) / 1_000_000_000.0;
        double readThroughput = totalRequests / readSeconds;
        double readAvgLatencyMs = (readSeconds * 1000.0) / totalRequests;

        // ---------- Raw numbers ----------
        System.out.println("---------------- CREATE phase ----------------");
        System.out.printf("Total time         : %.3f s%n", createSeconds);
        System.out.printf("Throughput         : %.1f creates/sec%n", createThroughput);
        System.out.printf("Avg latency        : %.5f ms/op%n", createAvgLatencyMs);
        System.out.println("Create failures    : " + createFailures);
        System.out.println("Duplicate shortURLs: " + duplicateShortUrls);

        System.out.println("\n----------------- READ phase ------------------");
        System.out.printf("Total time         : %.3f s%n", readSeconds);
        System.out.printf("Throughput         : %.1f reads/sec%n", readThroughput);
        System.out.printf("Avg latency        : %.5f ms/op%n", readAvgLatencyMs);
        System.out.println("Read failures      : " + readFailures);
        System.out.println("Read mismatches    : " + readMismatches);

        // ---------- Named test cases ----------
        System.out.println("\n---------------- TEST CASES ----------------");
        check("TC1: all creates succeeded (no exceptions)",
                createFailures == 0,
                createFailures + " / " + totalRequests + " create() calls threw");

        check("TC2: every short URL generated is unique",
                duplicateShortUrls == 0,
                duplicateShortUrls + " short URLs were reused for a different original URL");

        check("TC3: every created URL is readable immediately after",
                readFailures == 0,
                readFailures + " / " + totalRequests + " reads-after-write failed");

        check("TC4: every read resolves to the correct original URL",
                readMismatches == 0,
                readMismatches + " reads returned the WRONG original URL");

        check("TC5: create throughput is at least 500 ops/sec",
                createThroughput >= 500,
                String.format("measured %.1f creates/sec, expected >= 500", createThroughput));

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
                ? "VERDICT: PASS - system stayed correct under sequential load"
                : "VERDICT: FAIL - " + failed.size() + " test case(s) failed under load");
        System.out.println("==============================================");

        if (!failed.isEmpty()) System.exit(1);
    }
}