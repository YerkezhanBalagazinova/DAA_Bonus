import java.util.ArrayList;
import java.util.List;

public class KMP {
    public static List<Integer> search(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty())
            throw new IllegalArgumentException("Text cannot be null and pattern must not be empty");
        List<Integer> matches = new ArrayList<>();
        int[] lps = new int[pattern.length()];
        int len = 0;
        for (int i = 1; i < pattern.length();) {
            if (pattern.charAt(i) == pattern.charAt(len)) lps[i++] = ++len;
            else if (len > 0) len = lps[len - 1];
            else lps[i++] = 0;
        }
        int i = 0, j = 0;
        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    matches.add(i - j);
                    j = lps[j - 1];
                }
            } else if (j > 0) j = lps[j - 1];
            else i++;
        }
        return matches;
    }
}
