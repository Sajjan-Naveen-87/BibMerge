# Verification evidence

What we tested, against which acceptance criteria, and what passed. Every row names
the test that checks it, so a reviewer can open the test and re-run it.

Re-run everything with:

```bash
./mvnw verify
```

Last full run: **190 test runs, 0 failures**, with PRs #7–#9 and the review-in-browser
work combined (local branch `demo/local`). CI runs the same suite on every push and pull request
(`.github/workflows/ci.yml`).

## 1. Acceptance criteria from the proposal

| # | Criterion (proposal §6) | Evidence | Result |
|---|---|---|---|
| A1 | Merging several papers' `.bib` files gives one file with no duplicates, matching a hand-checked reference merge | `MergePipelineTest.mergesTheSyntheticCorpus`: 12 records in, 8 entries out, matching the hand-written expectation in `bibmerge-core/src/test/resources/corpus/synthetic/README.md`. `SyntheticCorpusTest.findsExactlyTheExpectedMerges` checks the exact pairs | Pass |
| A2 | Two differently labelled entries for one paper become one entry: DOI first, then fuzzy title/author/year | `SyntheticCorpusTest.mergesHappenForTheRightReason` checks the rule behind each merge: `identifier-agree` (same DOI written two ways; arXiv id), `deterministic-key` (no DOI, DBLP crossref), and `score>=tauHigh` (ePrint vs CRYPTO version) | Pass |
| A3 | Re-importing an already-merged paper adds zero new entries | `MergePipelineTest.sameFileTwiceGivesTheSameEntriesOnce`: the same file imported twice gives the same entries, with the same keys, and nothing for review. `MergePipelineTest.addingAPaperToAnEarlierMergeKeepsKeysAndAliases`: an earlier `merged.bib` plus a new paper keeps every existing key and old alias and adds only the new work | Pass |
| A4 | The output is a valid `.bib` that compiles under BibTeX/biblatex | `BibWriterTest.roundTripKeepsFieldsAndBraceProtection` and `MergePipelineTest.mergesTheSyntheticCorpus` re-parse the output with no diagnostics. A LaTeX compile is a manual check (§3) | Parse: pass. LaTeX: to run |

## 2. Corner cases named in the proposal

| Case (proposal §5) | Test | Expected behaviour | Result |
|---|---|---|---|
| Same DOI, different citation keys | `SyntheticCorpusTest` (dh76 / DiffieH76), `DoiNormalizerTest` | One entry; both old keys kept as `ids` aliases | Pass |
| Title, whitespace, casing, diacritic variants | `TitleNormalizerTest.fullForm`, `LatexDecoderTest`, `HtmlDecoderTest`, `AuthorNormalizerTest` | Variants normalize to the same form | Pass |
| Entries with missing fields | `RecordNormalizerTest.missingFieldsAreEmptyNotNullStrings`, `KeyMinterTest.missingPartsStillGiveAKey`, `FeatureExtractorTest.ruleScorerRenormalizesOverPresentFeatures` | No crash; missing evidence is left out of the score rather than counted as disagreement | Pass |
| Duplicate keys within one file | `BibParserTest.duplicateKeyInOneFileKeepsBothWithWarning` | Both entries kept, with a warning | Pass |
| Same key for **different** works in two files | `MergePipelineTest.sameKeyForDifferentWorksIsNotAnAlias`, `CliTest.mergeReportsAKeyThatNamesDifferentWorks` | Two entries; the key is **not** kept as an alias on either, and a warning names both files | Pass (fixed in this branch) |
| Empty file | `BibParserTest.emptyFileYieldsNothing`, `MergePipelineTest.noInputGivesAnEmptyBibliography` | Empty output, no error | Pass |
| File with text but no entries (wrong file) | `BibParserTest.fileWithTextButNoEntriesIsReported` | Warning: "No BibTeX entries found; is this a .bib file?" | Pass (added in this branch) |
| Malformed entry | `BibParserTest.malformedEntryLosesOnlyItself`, `SyntheticCorpusTest.malformedEntryIsSkippedAndReported` | That entry is skipped with file and line; the rest of the file loads | Pass |
| Non-UTF-8 file | `BibParserTest.latin1FileIsReadWithWarning`, `stripsUtf8ByteOrderMark` | Read as Latin-1, with a warning | Pass |
| Missing input file (CLI) | `CliTest.missingFileIsAnIoError`, `missingFileErrorSaysWhatIsWrong` | Exit code 1, "error: file not found: …" | Pass |
| Empty upload, unknown dialect (web) | `MergeControllerTest.emptyUploadIsBadRequest`, `unknownDialectIsBadRequest` | HTTP 400 with a reason | Pass |
| Wrong file uploaded (web) | `MergeControllerTest.wrongFileComesBackWithAWarning` | 0 entries, and the warning shown on the page | Pass |

## 2b. Reviewer decisions (proposal: "human-confirmation step for borderline matches")

| Case | Test | Expected behaviour | Result |
|---|---|---|---|
| "Different" beats a matching DOI | `HumanDecisionTest.keepSeparateBeatsAMatchingDoi` | DISTINCT, rule `human:keep-separate` | Pass |
| "Same paper" beats a veto (Part I / Part II) | `HumanDecisionTest.mergeBeatsAVeto` | MERGE, rule `human:merge` | Pass |
| Pair order doesn't matter; a later answer replaces an earlier one | `HumanDecisionTest.decisionIsSymmetricAndKeepSeparateWins`, `laterDecisionReplacesEarlierOne` | As described | Pass |
| A chain A–B–C when A and C are "different" | `ClusterResolverTest.keepSeparateCutsAChainAtItsBottleneck` | The weakest link is cut and goes back to review | Pass |
| Decisions change the merged file | `MergePipelineTest.humanDecisionsChangeTheOutput` | Split and forced merge both reflected | Pass |
| Through the web API | `MergeControllerTest.keepSeparateDecisionSplitsTheMerge`, `pairsCarryTheEvidenceForTheReviewer`, `badDecisionsAreBadRequest` | Split applied; evidence returned; bad input gives 400 | Pass |
| In the browser | Manual click-through, 1 Nov: 3 demo files, 14 records → 10 entries, 1 for review. "Same paper" → 9 entries; "Not the same" on Diffie–Hellman → 10; Undo → 9 | As described | Pass |

## 3. Manual checks

Run these on a machine with TeX Live before the demo and record the result here.

**A4, LaTeX compile.** From the repository root:

```bash
java -jar bibmerge-api/target/bibmerge.jar merge demo/*.bib -o /tmp/merged.bib
cd /tmp && printf '\\documentclass{article}\\usepackage[backend=biber]{biblatex}\\addbibresource{merged.bib}\\begin{document}\\nocite{*}\\cite{dh76,DiffieH76}\\printbibliography\\end{document}\n' > t.tex && latexmk -pdf t.tex
```

Pass means: no biber errors, and `\cite{dh76}` and `\cite{DiffieH76}` (both retired keys) resolve to the merged entry.

**Stakeholder data.** The real corpus lives in `corpus.local/` and is never committed
(ADR-14). Record here the number of files and records, entries out, and every wrong
decision found by hand.

## 4. Measured limits

All-pairs blocking compares every pair of records, so time grows with n².

| Input | Pairs compared | Time (`match`, laptop) | Peak memory |
|---|---|---|---|
| Demo, 12 records | 65 | under 1 s | — |
| Synthetic, 500 records | 124,750 | 2.6 s | 0.45 GB |
| Synthetic, 2,000 records | 1,999,000 | 29 s | 1.5 GB |

The synthetic files deliberately draw titles from a 20-word vocabulary, so many pairs
look alike and land in review. This is the worst case for memory, not a typical
library. A handful of papers (hundreds of records) is well inside these limits. The
design target of 10,000 records in under 10 s (NFR4) needs the planned blockers and
is **not met** yet.

Nothing is stored between runs. Adding a paper later works by uploading the earlier
`merged.bib` with the new file (A3). Reviewer decisions last for one browser session,
and persistent storage (design doc §4.10) is not built.
