
# Aho–Corasick Algorithm — Multiple Pattern Matching

## 1. Project Overview

In this assignment, I implemented the Aho–Corasick algorithm in Java and compared its performance with KMP.

The main purpose was to understand how searching for many patterns at once differs from searching for each pattern separately. KMP is already efficient when we have one pattern, but if we have hundreds or thousands of patterns, we need to run it repeatedly.

Aho–Corasick solves this problem by building a shared structure for all patterns and then searching through the text only once.

The project includes the algorithm implementation, correctness tests, performance experiments, and a comparison of the results.


**AhoCorasick.java** contains the main algorithm, including trie construction, failure links, and searching for multiple patterns.

**KMP.java** contains the KMP algorithm, which is used for the performance comparison.

**Tests.java** checks whether the algorithms return the correct matching positions.

**Experiment.java** generates input data, measures execution time, and saves the results to a CSV file.

**results.csv** contains the experimental results.

The project uses standard Java libraries and does not require any additional dependencies.

## 3. How Aho–Corasick Works

Aho–Corasick is a string-matching algorithm that searches for multiple patterns in a text at the same time.

Instead of processing each pattern separately, it combines all patterns into one data structure.

### Trie Construction

The first step is building a trie.

A trie is a tree where every node represents part of a pattern. Each edge represents a character. Patterns with the same beginning share nodes, so we do not need to store the same prefix several times.

For example, consider these patterns:

```text
he
hers
his
```

All three patterns begin with `h`. Their trie uses the same first node and then branches into different paths.

Each node contains transitions to other nodes and a list of patterns that end at that position.

In my implementation, the patterns are identified by their IDs. This also makes it possible to handle duplicate patterns correctly.

### Failure Links

The next step is constructing failure links.

A failure link tells the algorithm where to continue when the next character does not match the current path in the trie.

Without failure links, we would have to start the search from the beginning after a mismatch. With them, we can reuse part of the characters we have already processed.

Failure links are built using BFS (Breadth-First Search).

The root's failure link points to itself. The direct children of the root also have failure links pointing to the root.

For deeper nodes, the algorithm uses the parent's failure link to find the longest suitable suffix that is also a prefix of some pattern.

In my implementation, missing transitions are also prepared during this step, making the actual search easier.

### Output Handling

Finding a pattern is not always as simple as reaching the end of one path.

For example:

```text
Patterns: he, she
Text:     she
```

When the algorithm finds `she`, it must also report `he`, because the last two letters match that pattern.

My implementation handles this by adding the output IDs from a node's failure state to its own output list.

This works as an alternative to separate output links and allows the algorithm to report all patterns ending at the current position.

### Searching the Text

After the trie and failure links are ready, the algorithm starts reading the text from left to right.

For each character, it moves to the next state using the prepared transitions.

If the current state contains output IDs, a match has been found.

The starting index of each match is calculated using:

```text
start = currentIndex - patternLength + 1
```

For example:

```text
Text:     ababa
Patterns: aba, ba
```

The algorithm finds:

```text
aba -> 0, 2
ba  -> 1, 3
```

This example also shows that the algorithm supports overlapping matches.

The important part is that the entire text is scanned only once, even though we are searching for several patterns.

## 4. Correctness Testing

I created a separate `Tests.java` class to check different situations, not only simple successful searches.

The tests include:

- A single successful match
- A pattern that is not present in the text
- Overlapping matches
- Failure links and suffix matches
- Nested patterns
- Duplicate patterns
- An empty text
- An empty list of patterns
- Patterns with shared prefixes
- Uppercase characters and punctuation
- Unicode characters
- A pattern longer than the text
- Rejection of an empty pattern

The implementation uses a specific rule for empty inputs: an empty list of patterns is allowed, but an empty pattern string is rejected with `IllegalArgumentException`.

For the valid test cases, I also compared the matching positions with the KMP implementation to make sure both algorithms produced the same results.

**Test result: 13/13 passed.**

## 5. Experimental Setup

The assignment requires different numbers of patterns and different text lengths.

I tested all nine combinations:

| Number of patterns | Text length |
|---|---|
| 10 | 1,000 |
| 10 | 10,000 |
| 10 | 100,000 |
| 100 | 1,000 |
| 100 | 10,000 |
| 100 | 100,000 |
| 1,000 | 1,000 |
| 1,000 | 10,000 |
| 1,000 | 100,000 |

The input data was generated randomly using a fixed seed of `42`.

I used the alphabet `abcd`, and each generated pattern has a length of 6 characters.

Using a small alphabet makes matches more likely, which helps test the searching algorithms on actual occurrences.

Some randomly generated patterns may be identical. They are treated as separate pattern IDs, and both algorithms handle them consistently.

The same text and patterns were used for both algorithms in each experiment.

### Execution Time Measurement

Execution time was measured using:

```java
System.nanoTime()
```

Each experiment was repeated **5 times**, and the average execution time was calculated.

For Aho–Corasick, the measured time includes building the trie, constructing failure links, and searching the text.

For KMP, the measured time includes building the LPS array and searching the text separately for every pattern.

Input generation, correctness verification, console output, and CSV writing were excluded from the timed sections.

The experimental results were saved to `results/results.csv`.

## 6. Experimental Results

The following values are the average execution times from five runs, measured in milliseconds.

| Patterns | Text Length | Aho–Corasick (ms) | KMP (ms) | Matches |
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

These measurements were obtained using OpenJDK 21 on Linux. The results may vary on another computer.

### Results Analysis

The first thing I noticed was that Aho–Corasick was not always faster for small inputs.

For example, with 10 patterns and a text length of 10,000 characters, KMP was slightly faster. This makes sense because building a trie and failure links takes additional time, while running KMP for only a few short patterns is relatively simple.

However, the difference became much more noticeable when the number of patterns increased.

With 100 patterns and 100,000 characters, Aho–Corasick took about 10.93 ms, while KMP needed about 64.45 ms.

The largest difference appeared with 1,000 patterns and 100,000 characters. Aho–Corasick took approximately 4.20 ms, compared with 671.78 ms for KMP.

This happened because KMP scanned the text separately for every pattern, while Aho–Corasick used one combined structure.

Some Aho–Corasick times were not perfectly consistent. For example, adding more patterns did not always produce a higher measured time. This does not mean the algorithm becomes faster whenever more patterns are added. JVM warm-up, garbage collection, and other system activity can affect short measurements.

Since the assignment required five runs, I kept the real five-run averages rather than changing the values to make the results look smoother.

Both algorithms reported the same matching positions in the correctness checks.

## 7. Time and Space Complexity

The complexity depends on the size of the text, the total length of the patterns, and the number of matches.

Let:

- `n` = length of the text
- `P` = number of patterns
- `L` = total length of all patterns
- `Q` = number of trie nodes
- `sigma` = number of characters in the pattern alphabet
- `H` = total number of stored output IDs after propagation
- `Z` = number of reported matches

### Aho–Corasick Complexity

Building the trie requires processing every character of every pattern.

**Trie construction: O(L)** expected time.

Constructing failure links and preparing transitions requires visiting the trie nodes and processing characters from the alphabet.

For this implementation:

**Preprocessing: O(L + Q × sigma + H)** expected time.

Once preprocessing is finished, searching scans the text once and reports every match.

**Searching: O(n + Z)** expected time.

The combined complexity is:

```text
O(L + Q × sigma + H + n + Z)
```

With a fixed small alphabet and limited output propagation, this is close to the usual Aho–Corasick complexity:

```text
O(L + n + Z)
```

The automaton uses:

```text
O(Q × sigma + H)
```

additional space for transitions and outputs. The returned matches require O(Z) more space.

The exact complexity depends on how transitions and outputs are stored. This implementation uses hash maps and copies inherited output IDs into each state.

### KMP Complexity

For one pattern of length `m`, KMP requires:

```text
Time:  O(n + m)
Space: O(m)
```

But in this assignment, KMP is executed separately for every pattern.

For `P` patterns with total length `L`, the total running time is:

```text
O(P × n + L + Z)
```

This explains why the KMP approach becomes more expensive when the number of patterns increases.

### Complexity Comparison

| Algorithm | Time | Main Space Requirement |
|---|---|---|
| Aho–Corasick | O(L + Q × sigma + H + n + Z) | O(Q × sigma + H), excluding matches |
| Repeated KMP | O(P × n + L + Z) | O(maximum pattern length), excluding matches |

## 8. Advantages and Limitations

### Aho–Corasick

The main advantage of Aho–Corasick is that it can search for many patterns in a single pass through the text.

It is particularly useful when the number of patterns is large or when the same set of patterns is used for several texts.

It also supports overlapping occurrences, shared prefixes, and patterns that end inside other patterns.

However, building the trie and failure links requires additional time and memory. For a small number of patterns, this preprocessing may not be worth it.

### KMP

KMP is easier to understand and implement when searching for one pattern.

It uses an LPS array to avoid repeating unnecessary comparisons and has a reliable linear time complexity for individual searches.

Its main limitation in this comparison is that it must scan the text separately for each pattern.

As the number of patterns grows, this repeated work becomes expensive.





The CSV contains the number of patterns, text length, algorithm name, average execution time in nanoseconds and milliseconds, match count, and number of runs.

There are 18 data rows: one for Aho–Corasick and one for KMP for each of the nine experimental configurations.

The project can also be opened and executed in IntelliJ IDEA.

## 10. Conclusion

This assignment helped me understand why searching for multiple patterns is a different problem from searching for only one.

At first, I expected Aho–Corasick to be faster in every experiment, but the measurements showed that this is not always the case. With a small number of patterns, KMP can be just as good or even slightly faster.

The advantage of Aho–Corasick becomes clearer when the number of patterns increases. Instead of repeating the same search process hundreds of times, it prepares one trie and uses failure links to process all patterns together.

Overall, I think Aho–Corasick is the better choice when we need to search for many patterns, while KMP remains a simpler and efficient solution for individual patterns.
