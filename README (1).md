# Multiple Pattern Matching with Aho–Corasick

## 1. Project overview

This project implements the **Aho–Corasick algorithm in Java** and compares it with running **Knuth–Morris–Pratt (KMP) separately for every pattern**. Both methods find the positions where patterns appear inside a text, including overlapping occurrences.

The main question I wanted to explore was simple: **what changes when we search for 10 patterns instead of 1,000?** KMP is already efficient for one pattern, but it needs a separate search through the text for every pattern. Aho–Corasick combines the patterns into one structure, then searches the text only once.

The implementation includes a trie, failure links, matched-pattern outputs, correctness tests, and experiments with the exact pattern counts and text lengths from the assignment.

## 2. Project files

```text
AhoCorasick_Assignment/
├── AhoCorasick.java
├── KMP.java
├── Tests.java
├── Experiment.java
├── results/
│   └── results.csv
└── README.md
```

- **AhoCorasick.java:** builds the trie and failure links, then finds all matches for all patterns in one scan.
- **KMP.java:** searches for one pattern at a time; used as the comparison algorithm.
- **Tests.java:** checks expected results and compares Aho–Corasick with KMP on the same inputs.
- **Experiment.java:** generates reproducible inputs, runs both approaches five times, and saves average times.
- **results/results.csv:** raw measurements, match counts, and run counts.

No external libraries are needed.

## 3. How Aho–Corasick works

### Step 1: Build a trie

A trie is a tree that stores words one character at a time. Patterns that start with the same letters share the same beginning instead of making separate copies of it.

For example, if the patterns are `he`, `hers`, and `his`, all three begin with `h`. The trie starts with one `h` branch, which then splits into the different endings. Each node has a map named `next`, where a character leads to another node.

When a pattern ends at a node, its **pattern ID** is added to that node's `output` list. IDs matter because two patterns can contain the same text but still be separate entries in the input list.

### Step 2: Build failure links

A failure link tells the algorithm where to continue if the current sequence cannot be extended by the next character. Instead of starting from the very beginning, the algorithm can reuse a suffix of what it already matched if that suffix is also the beginning of another pattern.

The code builds these links with **BFS (breadth-first search)**, using a queue. The root is its own failure state, and the root's children fail back to the root. Deeper nodes get their failure links from the already processed states above them.

This implementation also fills in missing transitions for every character that occurs in the patterns. As a result, each node already knows where to go on a character during searching. If a text character is not used by any pattern, the search returns to the root.

### Step 3: Keep all matching outputs

A match may finish at a node even if the node itself is not the end of the original inserted pattern. For instance, `she` ends with `he`. If both patterns exist, finishing `she` also means that `he` has been found.

To handle this, the implementation copies the matched-pattern IDs from a node's failure state into its own `output` list. This is **equivalent to following output links**: after reaching a node, all patterns that end there are already listed together. It also handles patterns that share endings and repeated patterns with different IDs.

### Step 4: Scan the text once

The search reads the text from left to right. For each character, it follows the prepared transition and checks the current node's `output` list. Every ID in this list gives a match. Its starting index is calculated as:

```text
start index = current text index - pattern length + 1
```

The result is a list of matches. Each match contains the **pattern ID** and the **starting index**. The program reports all occurrences, including overlapping ones.

For a small example:

```text
Text:     ababa
Patterns: aba, ba
```

The results are `aba` at positions 0 and 2, and `ba` at positions 1 and 3. Nothing is skipped just because two matches overlap.

## 4. Why KMP is a useful comparison

KMP finds all occurrences of **one** pattern in O(n + m) time, where `n` is the text length and `m` is the pattern length. It first creates an LPS array for that pattern and then searches the text.

To search for many patterns with KMP, the program repeats that work for each pattern:

```java
for (String pattern : patterns) {
    KMP.search(text, pattern);
}
```

This is correct, but the text is scanned separately for every pattern. With 1,000 patterns, the comparison program performs 1,000 KMP searches. Aho–Corasick instead prepares one automaton from all 1,000 patterns and scans the text once.

## 5. Correctness tests

The tests are in `Tests.java`. The expected occurrences are checked, not just the total number of matches. Every valid test also checks that the positions reported by Aho–Corasick agree with KMP for each individual pattern.

| Test | What it checks |
|---|---|
| Single match | One pattern appears once at a known position |
| No match | A pattern is absent from the text |
| Overlapping matches | Different occurrences may share characters |
| Failure and suffix matches | `he`, `she`, and `hers` in `ushers` |
| Nested patterns | `a`, `aa`, and `aaa` in a repeated-letter text |
| Duplicate patterns | Identical pattern strings still have separate IDs |
| Empty text | No positive-length pattern can match |
| No patterns | An empty pattern list produces no results |
| Shared prefixes | Patterns `ab`, `abc`, and `abcd` |
| Mixed case and punctuation | Patterns are not limited to lowercase letters |
| Unicode characters | The Java implementation handles other characters |
| Pattern longer than text | No match is returned |
| Empty pattern rejected | Empty pattern strings are explicitly disallowed |

**Test output:** `Passed: 13/13` with OpenJDK 21.

**Edge-case rule:** This implementation accepts an empty *list* of patterns but rejects an empty *pattern string*. There is no useful word to insert into the trie for an empty pattern, so the constructor throws `IllegalArgumentException` instead of silently assigning it positions.

## 6. Experimental setup

The experiments use **all nine combinations** required by the assignment:

| Number of patterns | Text lengths (characters) |
|---:|---|
| 10 | 1,000; 10,000; 100,000 |
| 100 | 1,000; 10,000; 100,000 |
| 1,000 | 1,000; 10,000; 100,000 |

The input generator uses a **fixed seed of 42** and the alphabet `abcd`. Every pattern has length **6**. This small alphabet creates a useful number of matches, so the algorithms are tested on actual occurrences rather than only unsuccessful searches. Randomly generated patterns can repeat; the implementation treats them as different pattern IDs, and both algorithms count them consistently.

The same generated text and the same list of patterns are passed to both algorithms in each comparison. The text for each length is reused across pattern-count configurations, and each pattern set is reused across text lengths.

For every configuration:

1. The program checks all reported positions against KMP **before timing**. If any pattern has different positions, the experiment stops with an error.
2. It measures Aho–Corasick **trie construction, failure-link construction, and searching together**.
3. It measures KMP **LPS construction and searching separately for each pattern**, including all patterns in the set.
4. It repeats each approach **five times** with `System.nanoTime()` and reports the arithmetic average.
5. It writes the measured times to `results/results.csv` **outside the timed regions**.

Text and pattern generation, result verification, console output, and writing the CSV file are not included in the measured time. The execution order of the two algorithms alternates between repetitions to reduce the effect of always running the same algorithm first.

The included CSV was generated by running the program with **OpenJDK 21 on Linux**. Short timings can vary because of JVM warm-up, garbage collection, CPU scheduling, and other background activity. Re-running the experiments on another computer will give different timings while keeping the same test inputs.

## 7. Experimental results

The table below uses the real measurements saved in [`results/results.csv`](results/results.csv). Each entry is the average of **five timed runs**, converted to milliseconds.

| Patterns | Text length | Aho–Corasick (ms) | KMP for all patterns (ms) | Matches |
|---:|---:|---:|---:|---:|
| 10 | 1,000 | 0.8267 | 0.9531 | 2 |
| 10 | 10,000 | 2.1604 | 1.8195 | 29 |
| 10 | 100,000 | 3.1538 | 6.4192 | 247 |
| 100 | 1,000 | 0.5451 | 0.4614 | 22 |
| 100 | 10,000 | 0.9594 | 6.1688 | 237 |
| 100 | 100,000 | 10.9302 | 64.4527 | 2,367 |
| 1,000 | 1,000 | 3.9840 | 4.5567 | 247 |
| 1,000 | 10,000 | 1.0299 | 71.1729 | 2,425 |
| 1,000 | 100,000 | 4.2023 | 671.7821 | 24,222 |

### What the results show

With **10 patterns**, the difference is small. KMP is even faster for the 10,000-character text. This is reasonable: building a trie and all its transitions has a cost, while running KMP for only a few short patterns can be cheap.

When the number of patterns grows to **100**, Aho–Corasick becomes much more useful, especially with longer texts. At 100 patterns and 100,000 characters, it took around **10.93 ms**, compared with around **64.45 ms** for repeated KMP.

With **1,000 patterns**, the advantage is much larger. For the 100,000-character text, Aho–Corasick took around **4.20 ms**, while KMP took around **671.78 ms**. That is roughly **160 times faster in this particular run**. The reason is that Aho–Corasick scans the text once, while the KMP baseline scans it once for every pattern.

Some Aho–Corasick times do not increase smoothly. For instance, the measured time with 1,000 patterns and a 100,000-character text is lower than one of the 100-pattern results. This should not be interpreted as a theoretical speedup from adding patterns. It is ordinary measurement variation from very short JVM benchmarks. A larger benchmark with more warm-up and repetitions would be useful for cleaner timing curves, but the assignment requests five runs, so I kept the actual five-run averages.

The match counts are the same for both algorithms because the program verifies the exact match positions for every pattern before measuring the times.

## 8. Time and space complexity

Let:

- `P` be the number of patterns.
- `L` be the sum of all pattern lengths.
- `n` be the text length.
- `Q` be the number of trie states, at most `L + 1`.
- `σ` be the number of distinct characters used by the patterns.
- `H` be the total number of pattern IDs stored in all output lists after failure-output propagation.
- `Z` be the total number of matches reported in the text.

### Aho–Corasick

**Trie insertion:** O(L) expected time using hash-map transitions.

**Failure links and prepared transitions:** O(Q × σ + H) expected time. This implementation fills transitions for every character used in the patterns and copies inherited output IDs, so both the alphabet size and the output propagation matter.

**Searching:** O(n + Z) expected time. Each text character uses a prepared transition, and reporting each real match takes additional work.

**Total preprocessing and search:**

```text
O(L + Q × σ + H + n + Z)
```

**Space:** O(L + Q × σ + H + Z), including the input pattern references, automaton transitions, stored output IDs, and returned matches. More precisely, the automaton itself takes O(Q × σ + H) space. With a fixed small alphabet and few inherited outputs, this behaves close to the usual linear Aho–Corasick bound in the total pattern length plus text length and matches.

The output IDs are copied along failure links to make searching simple. A different implementation using separate output links could reduce repeated output storage. The trade-off here is slightly more preprocessing and memory in exchange for a straightforward search loop.

### Repeated KMP

For one pattern of length `m`, KMP takes O(n + m) time and O(m) auxiliary memory, excluding the matches.

For `P` patterns with total length `L`, running KMP separately takes:

```text
O(P × n + L + Z) time
```

The KMP searches run one after another, so their reusable auxiliary memory is O(maximum pattern length), aside from output lists. The KMP code returns each pattern's matches separately; `Experiment.java` counts these results for the comparison.

### Summary

| Approach | Main idea | Search time | Main limitation |
|---|---|---|---|
| Aho–Corasick | One trie and one text scan for all patterns | O(n + Z) after preprocessing | Trie, transitions, and outputs use additional memory |
| Repeated KMP | Run a separate KMP search for each pattern | O(P × n + L + Z) total | Re-scans the text many times |

## 9. Advantages and limitations

**Aho–Corasick is a good choice** when many patterns must be searched in the same text, or when the same pattern set will be reused on multiple texts. After building the automaton, the text only needs one pass. It can report several patterns at the same position, including shared endings and overlaps.

Its main disadvantage is the extra work and memory needed to build the trie, failure links, and transition/output information. For only a few short patterns, this setup cost may not pay off. The current implementation also rebuilds the automaton for each timed run; reusing one constructed automaton across many texts could make Aho–Corasick even more attractive.

**KMP is simpler for one pattern** and needs only a small LPS array. It has a reliable linear bound even when a pattern has many repeated characters. However, when searching for hundreds or thousands of patterns, running KMP once per pattern repeats almost the entire text scan each time.

## 10. How to run

Use **Java 17 or later**. The project was checked using **Java 21**. All four `.java` files are in the project root and do not require a Java package declaration.

From the project folder, compile and run the correctness tests:

```bash
javac -encoding UTF-8 *.java
java Tests
```

Expected last line:

```text
Passed: 13/13
```

Run the measurements:

```bash
java Experiment
```

This creates or overwrites:

```text
results/results.csv
```

The CSV columns are:

| Column | Meaning |
|---|---|
| `patternCount` | Number of searched patterns |
| `textLength` | Number of characters in the text |
| `algorithm` | Aho–Corasick or KMP |
| `averageTimeNs` | Average execution time in nanoseconds |
| `averageTimeMs` | The same average in milliseconds |
| `matches` | Number of pattern occurrences found |
| `runs` | Number of measured repetitions, always 5 |

In IntelliJ IDEA, open this folder as a separate Java project. Run `Tests.main()` first and then `Experiment.main()`. Keep all four source files in the same source directory and make sure the working directory points to the project folder, so `results/results.csv` appears in the correct place.

The full experiment writes **18 data rows**: two algorithms for each of the nine required input configurations. All data are generated from the same fixed random seed, so the algorithms see identical inputs on repeated executions.

## 11. Conclusion

This project helped show why a multiple-pattern algorithm is different from simply repeating a fast single-pattern algorithm. KMP is efficient when there are only one or a few patterns, but its total work grows with the number of patterns because it searches the text again each time.

Aho–Corasick spends extra time creating a trie and failure links, then makes use of that structure to find all the patterns in one scan. The tests confirm that it reports overlapping matches, patterns that end inside other patterns, and duplicate pattern entries correctly. The experiments show that its advantage becomes especially noticeable with 100 and 1,000 patterns.

The important takeaway is not that Aho–Corasick always has the lowest measured time. It is that **for large groups of patterns, combining them into one search structure avoids repeating the same scan of the text**. The measured results and the complexity analysis both support that conclusion.
