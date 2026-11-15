import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Test {
    static int passed = 0, total = 0;

    static void check(String name, boolean correct) {
        total++;
        if (correct) passed++;
        System.out.println((correct ? "PASS: " : "FAIL: ") + name);
    }

    static boolean matches(String text, List<String> patterns, String expected) {
        AhoCorasick aho = new AhoCorasick(patterns);
        List<AhoCorasick.Match> result = aho.search(text);
        List<String> actual = new ArrayList<>();
        List<List<Integer>> byPattern = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) byPattern.add(new ArrayList<>());
        for (AhoCorasick.Match match : result) {
            actual.add(match.toString());
            byPattern.get(match.patternId).add(match.index);
        }
        Collections.sort(actual);
        for (int i = 0; i < patterns.size(); i++) {
            if (!byPattern.get(i).equals(KMP.search(text, patterns.get(i)))) return false;
        }
        return String.join(", ", actual).equals(expected);
    }

    static boolean rejectsEmptyPattern() {
        try {
            new AhoCorasick(Arrays.asList("a", ""));
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        check("Single match", matches("xxabcxx", Arrays.asList("abc"), "0@2"));
        check("No match", matches("abcdef", Arrays.asList("xyz"), ""));
        check("Overlapping matches", matches("ababa", Arrays.asList("aba", "ba"), "0@0, 0@2, 1@1, 1@3"));
        check("Failure and suffix matches", matches("ushers", Arrays.asList("he", "she", "his", "hers"), "0@2, 1@1, 3@2"));
        check("Nested patterns", matches("aaaaa", Arrays.asList("a", "aa", "aaa"), "0@0, 0@1, 0@2, 0@3, 0@4, 1@0, 1@1, 1@2, 1@3, 2@0, 2@1, 2@2"));
        check("Duplicate patterns", matches("aaa", Arrays.asList("aa", "aa"), "0@0, 0@1, 1@0, 1@1"));
        check("Empty text", matches("", Arrays.asList("a", "ab"), ""));
        check("No patterns", matches("abc", Collections.emptyList(), ""));
        check("Patterns sharing prefixes", matches("abcd", Arrays.asList("ab", "abc", "abcd"), "0@0, 1@0, 2@0"));
        check("Mixed case and punctuation", matches("A!a!", Arrays.asList("A!", "a!", "!"), "0@0, 1@2, 2@1, 2@3"));
        check("Unicode characters", matches("aБaБ", Arrays.asList("Б", "aБ"), "0@1, 0@3, 1@0, 1@2"));
        check("Pattern longer than text", matches("ab", Arrays.asList("abc"), ""));
        check("Reject empty patterns", rejectsEmptyPattern());
        System.out.println("Passed: " + passed + "/" + total);
        if (passed != total) throw new AssertionError("Some tests failed");
    }
}
