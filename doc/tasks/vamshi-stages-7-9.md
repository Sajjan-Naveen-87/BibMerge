# Stages 7–9 for the mid-demo — Vamshi

**Owner:** Gowni Vamshi (design doc §6.6). **Due:** Wed 7 Oct, 19:00 IST, so the
end-to-end dry run can use it. **Mid-demo:** Fri 9 Oct.

Everything around these classes already exists: `match` finds the duplicates, and
`MergePipeline` and the `merge` CLI command call your classes in order. Until they
are implemented, `merge` exits with `TODO(vamshi): ...`.

## How to work

1. `git switch stage7-9-starter && git switch -c vamshi/stages-7-9`
2. Implement one class at a time, in the order below. For each one, delete the
   `@Disabled` line on its test class and run `./mvnw -pl bibmerge-core verify`.
3. Commit each class separately **from your own GitHub account**
   (`git config user.name` / `user.email` set to yours). Push and open a PR;
   Naveen reviews.
4. When all four are done, enable `MergePipelineTest` and run
   `java -jar bibmerge-api/target/bibmerge.jar merge bibmerge-core/src/test/resources/corpus/synthetic/*.bib -o merged.bib`.

## The classes

| # | Class | Test | What it does |
|---|---|---|---|
| 1 | `cluster.UnionFind` | `UnionFindTest` | Disjoint sets with path compression and union by size. `components()` is deterministic: each set ascending, sets ordered by smallest element. Must handle a 100 000-long chain (no recursion in `find`). |
| 2 | `cluster.ClusterResolver` | `ClusterResolverTest` | Index the records by position (identity, not `equals`), `union` each MERGE pair, return one `Cluster` per component, ordered by first member. |
| 3 | `golden.KeyMinter` | `KeyMinterTest` | `family + year + first non-stopword title word`, all from the `NormalizedRecord` (already folded and lower-cased). Missing family → `anon`, missing year → `0000`. On collision append `a`, `b`, ... until free. |
| 4 | `golden.GoldenRecordBuilder` | `GoldenRecordBuilderTest` | Per field, over every member's `source().effectiveFields()`: a member with a DOI (`ids().doi() != null`) wins; else the longest non-blank value; ties go to the earliest member. Entry type: the `VERSION_OF_RECORD` member's type if the cluster mixes a preprint and a published version, otherwise the most common type. Drop `crossref` from the output. Key: `KeyMinter`, then add it to `taken`. Retired keys: every member's key, in member order. |
| 5 | `serialize.BibWriter` | `BibWriterTest` | Preambles first; entries sorted by key; fields `author`, `title`, then alphabetical; `  name = {value},` per line, values verbatim; `ids = {k1,k2}` under `BIBLATEX` when retired keys are non-empty. Must round-trip through `BibParser`. |

## What you must be able to explain at the viva

- Why union-find gives transitive clusters, and what path compression buys.
- Why a cluster's key is minted once and never changes (ADR-8), and what `ids`
  does for the stakeholder's existing `.tex` files (Stage 9, route 1).
- What happens when two records disagree on a field, and why the DOI record wins.

## Not needed for the mid-demo (gap list)

Stage 7.5 cohesion guards, Kruskal bottleneck repair, alias-mode export, the
retired-key table, incremental import (§4.10).
