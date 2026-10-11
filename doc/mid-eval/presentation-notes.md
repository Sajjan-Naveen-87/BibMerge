# Mid-evaluation: demo and presentation notes

## Before the slot (5 minutes)

```bash
./mvnw -Pweb -DskipTests package          # builds the React UI into the JAR
java -jar bibmerge-api/target/bibmerge.jar  # http://localhost:8091
```

Fallback without the browser:
`java -jar bibmerge-api/target/bibmerge.jar merge demo/*.bib -o merged.bib`

Open `doc/mid-eval/BibMerge-Architecture.pdf` (the diagram they ask for).

## 2-minute overview (about 250 words)

1. **Problem (15 s).** Researchers combine `.bib` files from many papers; the same
   work appears under different keys and formatting. Our stakeholder, Prof. Ashrujit
   Ghoshal (cryptography), merges these by hand.
2. **Architecture (45 s), point at the diagram.** Three entry points (web UI, REST API,
   CLI) all call one engine, `bibmerge-core`, plain Java with no Spring. It is a
   pipeline: parse, resolve crossref, normalize (DOI, arXiv, IACR ePrint ids, titles,
   four-part author names), propose candidate pairs, compute similarity features, and
   decide MERGE, REVIEW or DISTINCT through an ordered list of rules with vetoes. MERGE
   pairs become clusters through union-find; each cluster becomes one golden record with
   a stable new key; old keys survive as biblatex `ids` aliases so existing `\cite`s keep
   working.
3. **Live run (40 s).** Upload `demo/paper-a.bib` and `demo/paper-b.bib`: 12 records
   become 8 entries; show one merge and its rule (identifier-agree, deterministic-key);
   show the warning for the malformed entry; download `merged.bib`.
4. **Evidence (20 s).** 162 tests; real IACR ePrint records of the stakeholder give 0
   false merges in 153 pairs; bugs found on real data are in the Principles document.

## Where things live (for the live modification)

| If asked to change... | Go to |
|---|---|
| Merge / review thresholds | `match/DecisionConfig.defaults()` |
| A decision rule or veto | `match/DecisionPolicy.decide()` (rules are numbered in comments) |
| A similarity feature or its weight | `match/FeatureExtractor.extract()`, weights in `model/Feature` |
| Stopwords, title markers | `normalize/TitleNormalizer` (`STOPWORDS`, `MARKER`) |
| Author name parsing | `normalize/AuthorNormalizer.parse()` |
| DOI / arXiv / ePrint recognition | `normalize/DoiNormalizer`, `ArxivNormalizer`, `IacrNormalizer` |
| Venue cleanup, entry-type aliases | `normalize/FieldNormalizers` |
| LaTeX accents / symbols | `parse/LatexDecoder` (`ACCENTS`, `SYMBOLS`) |
| Parsing / diagnostics | `parse/BibParser`, crossref in `parse/InheritanceResolver` |
| Clustering | `cluster/ClusterResolver`, `cluster/UnionFind` |
| Which value wins a field | `golden/GoldenRecordBuilder.beats()` |
| Citation key format | `golden/KeyMinter.mint()` |
| Output field order / format | `serialize/BibWriter` (`LEADING_FIELDS`) |
| CLI commands / flags | `bibmerge-api/.../cli/Cli` |
| REST endpoint | `bibmerge-api/.../web/MergeController` |
| UI | `bibmerge-web/src/App.tsx` |

Every package above is under `bibmerge-core/src/main/java/io/bibmerge/core/`. Each has
a matching test class; change the test first, then the code, then run
`./mvnw -pl bibmerge-core verify` (about 10 s).

Owners: Naveen — parse, normalize, block, match; Vamshi — cluster, golden, serialize.
The evaluation is per student, and each may be asked about the other's modules.
Only Gemini Pro is allowed during the evaluation.
