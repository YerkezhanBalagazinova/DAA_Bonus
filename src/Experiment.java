import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Experiment {
    static final int[] PATTERN_COUNTS = {10, 100, 1000};
    static final int[] TEXT_LENGTHS = {1000, 10000, 100000};
    static final int PATTERN_LENGTH = 6;
    static final int RUNS = 5;
    static final String ALPHABET = "abcd";

    static class Result {
        long timeNs;
        int matches;

        Result(long timeNs, int matches) {
            this.timeNs = timeNs;
            this.matches = matches;
        }
    }

    static String randomString(Random random, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return result.toString();
    }

    static List<String> randomPatterns(Random random, int count) {
        List<String> patterns = new ArrayList<>();
        for (int i = 0; i < count; i++) patterns.add(randomString(random, PATTERN_LENGTH));
        return patterns;
    }

    static Result runAho(String text, List<String> patterns) {
        long start = System.nanoTime();
        AhoCorasick aho = new AhoCorasick(patterns);
        int count = aho.search(text).size();
        return new Result(System.nanoTime() - start, count);
    }

    static Result runKMP(String text, List<String> patterns) {
        long start = System.nanoTime();
        int count = 0;
        for (String pattern : patterns) count += KMP.search(text, pattern).size();
        return new Result(System.nanoTime() - start, count);
    }

    static void verify(String text, List<String> patterns) {
        List<AhoCorasick.Match> ahoMatches = new AhoCorasick(patterns).search(text);
        List<List<Integer>> byPattern = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) byPattern.add(new ArrayList<>());
        for (AhoCorasick.Match match : ahoMatches) {
            byPattern.get(match.patternId).add(match.index);
        }
        for (int i = 0; i < patterns.size(); i++) {
            if (!byPattern.get(i).equals(KMP.search(text, patterns.get(i)))) {
                throw new IllegalStateException("Aho-Corasick and KMP results differ for pattern " + i);
            }}
        }

static void save(PrintWriter file, int patternCount, int textLength, String algorithm, double averageNs, int matches) {
    file.printf(Locale.US, "%d,%d,%s,%.0f,%.4f,%d,%d%n",
            patternCount, textLength, algorithm, averageNs, averageNs / 1000000.0, matches, RUNS);
}

public static void main(String[] args) throws FileNotFoundException {
    Random random = new Random(42);
    List<String> texts = new ArrayList<>();
    List<List<String>> patternSets = new ArrayList<>();
    for (int length : TEXT_LENGTHS) texts.add(randomString(random, length));
    for (int count : PATTERN_COUNTS) patternSets.add(randomPatterns(random, count));
    new File("results").mkdirs();
    try (PrintWriter file = new PrintWriter("results/results.csv")) {
        file.println("patternCount,textLength,algorithm,averageTimeNs,averageTimeMs,matches,runs");
        for (int p = 0; p < PATTERN_COUNTS.length; p++) {
            for (int t = 0; t < TEXT_LENGTHS.length; t++) {
                List<String> patterns = patternSets.get(p);
                String text = texts.get(t);
                verify(text, patterns);
                long totalAho = 0, totalKmp = 0;
                int ahoMatches = -1, kmpMatches = -1;
                for (int run = 0; run < RUNS; run++) {
                    Result a, k;
                    if (run % 2 == 0) {
                        a = runAho(text, patterns);
                        k = runKMP(text, patterns);
                    } else {
                        k = runKMP(text, patterns);
                        a = runAho(text, patterns);
                    }
                    if (a.matches != k.matches) throw new IllegalStateException("Different match counts");
                    totalAho += a.timeNs;
                    totalKmp += k.timeNs;
                    ahoMatches = a.matches;
                    kmpMatches = k.matches;
                }
                save(file, PATTERN_COUNTS[p], TEXT_LENGTHS[t], "Aho-Corasick", totalAho / (double) RUNS, ahoMatches);
                save(file, PATTERN_COUNTS[p], TEXT_LENGTHS[t], "KMP", totalKmp / (double) RUNS, kmpMatches);
                System.out.printf(Locale.US, "Patterns: %4d | Text: %6d | Aho: %8.3f ms | KMP: %8.3f ms | Matches: %d%n",
                        PATTERN_COUNTS[p], TEXT_LENGTHS[t], totalAho / (RUNS * 1000000.0),
                        totalKmp / (RUNS * 1000000.0), ahoMatches);
            }
        }
    }
    System.out.println("Saved: results/results.csv");
}
}

