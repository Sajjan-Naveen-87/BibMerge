# Final-demo tasks — Vamshi

**Owner:** Gowni Vamshi (design doc §6.6: clustering, serialize, API, CLI, front end).
**Final demo:** Thu 5 Nov, 17:00 IST, run by Prof. Ghoshal. **IITM submission:** 6 Nov.

Four PRs, each small enough for one evening. They are independent: do them in any
order. None is needed for the demo to work, but each closes a line of the design doc
that the viva can ask about.

## How to work

1. Start from the latest `main`: `git switch main && git pull && git switch -c vamshi/<task>`.
   Tasks 2 and 3 need Naveen's "human decisions" PR merged first. Check that
   `ClusterResolver.Resolution` exists on `main`.
2. Write the failing test first, then the code. Run `./mvnw verify`, which must stay green.
3. Commit **from your own GitHub account** (`git config user.name` / `user.email`).
   Push and open the PR with `--reviewer Sajjan-Naveen-87`. Naveen reviews and merges.
4. Add your test names to `doc/VERIFICATION.md` in the same PR.

## The tasks

### 1. CLI exit codes as the design says (`vamshi/exit-codes`)

Design doc §3.4: `0` clean, `1` merged with review items pending, `2` input error,
`3` internal error. Today `1` means a file error and pending reviews return `0`.

- `api/cli/Cli.java`: `match` and `merge` return `1` when there are REVIEW pairs.
  A missing or unreadable file and bad arguments both return `2`. Any unexpected
  exception returns `3` with a one-line message (no stack trace).
- `CliTest`: one test per code. Update `missingFileIsAnIoError` to expect `2`.
- Update the exit-code line in `doc/USER-GUIDE.md` §5.

### 2. Cluster validation guards, Stage 7.5 (`vamshi/cluster-guards`)

Design doc Stage 7.5. One false merge can chain two correct clusters together.

- In `cluster/ClusterResolver`, after the keep-separate repair, check each cluster:
  at most `8` members, and a year span of at most `2` (ignore records with no year).
- A failing cluster is cut at its weakest edge with the same bottleneck routine,
  and that edge goes to `Resolution.cut()`, so it shows under **Needs review**.
  Repeat until every cluster passes.
- Tests in `ClusterResolverTest`: a 9-record chain gets cut; a cluster spanning
  2018–2021 gets cut; a 2019–2020 cluster stays whole.
- Viva: why connected components are dangerous (transitive chaining), and why this
  costs at most |edges| rounds.

### 3. Classic BibTeX output that compiles (`vamshi/bibtex-dialect`)

Design doc Stage 9, "Dialect is explicit". Under `--dialect bibtex`, biblatex-only
fields break a classic `bst`.

- `serialize/BibWriter`, under `Dialect.BIBTEX` only:
  `date = {2017-06}` → `year = {2017}` and `month = {jun}` (if no year/month already);
  `journaltitle` → `journal`; `location` → `address`; `@online` → `@misc`, with
  the `url` kept.
- `BIBLATEX` output is unchanged. `BibWriterTest` must still pass.
- New tests in `BibWriterTest` for each mapping, plus a round trip through `BibParser`.

### 4. Downloadable JAR on GitHub Releases (`vamshi/release-workflow`)

So Prof. Ghoshal can download `bibmerge.jar` instead of building it.

- `.github/workflows/release.yml`: on a pushed tag `v*`, set up JDK 21 and Node 22,
  run `./mvnw -B -Pweb package`, and attach `bibmerge-api/target/bibmerge.jar` to a
  GitHub Release for that tag (`gh release create "$GITHUB_REF_NAME" ...` with
  `permissions: contents: write`).
- Test it by pushing a tag on your fork or a throwaway tag, then delete the test release.
- `doc/USER-GUIDE.md` §2: add "download `bibmerge.jar` from the Releases page" as the
  first option.

## What you must be able to explain at the viva

- The keep-separate repair Naveen added in `ClusterResolver` (you own that class):
  Kruskal order, the bottleneck edge, and why human merges are inserted first.
- How a reviewer's answer reaches the engine: the `decisions` JSON →
  `Constraints` → rules 1–2 in `DecisionPolicy`.
