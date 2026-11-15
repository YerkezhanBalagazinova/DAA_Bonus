import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class AhoCorasick {
    static class Node {
        Map<Character, Node> next = new HashMap<>();
        Node fail;
        List<Integer> output = new ArrayList<>();
    }

    public static class Match {
        public final int patternId;
        public final int index;

        public Match(int patternId, int index) {
            this.patternId = patternId;
            this.index = index;
        }

        public String toString() {
            return patternId + "@" + index;
        }
    }

    private final Node root = new Node();
    private final List<String> patterns;
    private final Set<Character> alphabet = new HashSet<>();

    public AhoCorasick(List<String> patterns) {
        if (patterns == null) throw new IllegalArgumentException("Patterns cannot be null");
        this.patterns = new ArrayList<>(patterns);
        for (int id = 0; id < patterns.size(); id++) {
            String pattern = patterns.get(id);
            if (pattern == null || pattern.isEmpty()) throw new IllegalArgumentException("Patterns must not be empty");
            Node current = root;
            for (int j = 0; j < pattern.length(); j++) {
                char c = pattern.charAt(j);
                alphabet.add(c);
                Node next = current.next.get(c);
                if (next == null) {
                    next = new Node();
                    current.next.put(c, next);
                }
                current = next;
            }
            current.output.add(id);
        }
        buildFailureLinks();
    }

    private void buildFailureLinks() {
        Queue<Node> queue = new ArrayDeque<>();
        root.fail = root;
        for (char c : alphabet) {
            Node child = root.next.get(c);
            if (child == null) root.next.put(c, root);
            else {
                child.fail = root;
                queue.add(child);
            }
        }
        while (!queue.isEmpty()) {
            Node current = queue.remove();
            for (char c : alphabet) {
                Node child = current.next.get(c);
                if (child != null) {
                    child.fail = current.fail.next.get(c);
                    child.output.addAll(child.fail.output);
                    queue.add(child);
                } else {
                    current.next.put(c, current.fail.next.get(c));
                }
            }
        }
    }

    public List<Match> search(String text) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null");
        List<Match> matches = new ArrayList<>();
        Node current = root;
        for (int i = 0; i < text.length(); i++) {
            Node next = current.next.get(text.charAt(i));
            current = next == null ? root : next;
            for (int id : current.output) {
                matches.add(new Match(id, i - patterns.get(id).length() + 1));
            }
        }
        return matches;
    }
}
