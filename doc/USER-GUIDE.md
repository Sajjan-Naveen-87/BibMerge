# BibMerge user guide

For anyone who wants to merge the bibliographies of several papers. You don't need to
read the code. If something here doesn't work, see [When something goes wrong](#when-something-goes-wrong).

## What BibMerge does

You give it the `.bib` files of several papers. It finds the entries that are the same
work under different keys or formatting (for example `dh76` and `DiffieH76`), keeps one
clean entry for each work, and gives you a single `merged.bib`.

Your existing `\cite{...}` commands keep working. Each merged entry lists its old keys
in an `ids` field, and biblatex/biber resolves those old keys to the merged entry.

BibMerge never merges when it isn't sure. Uncertain pairs are listed under
**Needs review** and kept as separate entries, so nothing is lost.

## 1. Install Java (once)

BibMerge needs Java 21 or newer. Check what you have:

```bash
java -version
```

If the first line shows 21 or higher, you're done. Otherwise install a JDK:

- **Ubuntu / Debian:** `sudo apt install openjdk-21-jre`
- **macOS (Homebrew):** `brew install openjdk@21`
- **Windows:** install "Temurin 21" from adoptium.net

## 2. Get `bibmerge.jar`

The whole tool is one file, `bibmerge.jar`. Either use the copy the team gave you,
or build it from the repository (this needs JDK 21 and Node 22):

```bash
git clone https://github.com/Sajjan-Naveen-87/BibMerge.git
cd BibMerge
./mvnw -Pweb package
```

The file is then at `bibmerge-api/target/bibmerge.jar`. The `-Pweb` part matters:
without it, the JAR has no web page, only the command line.

## 3. Merge in the browser

Start BibMerge:

```bash
java -jar bibmerge.jar
```

Wait for the line `Started BibMergeApplication`, then open
**http://localhost:8091** in your browser.

1. Click **Choose files** and select the `.bib` files of all the papers. Hold Ctrl
   (Cmd on a Mac) to select several.
2. Pick the output style:
   - **biblatex** (recommended): old keys keep working through `ids`.
   - **classic BibTeX**: no `ids` field, because classic BibTeX doesn't support it.
     Old keys will **not** resolve, so you have to update your `\cite` commands.
3. Click **Merge**.
4. Read the summary: records in, entries out, merges made, pairs for review.
5. Click **Download merged.bib**.

To stop BibMerge, press Ctrl+C in the terminal where it runs.

### Reading the results

| Section | What it means | What to do |
|---|---|---|
| **Merged** | Pairs judged to be the same work. The **Why** column names the rule, e.g. `identifier-agree` (same DOI or arXiv id) or `deterministic-key` (same title, first author and year). | Skim it. A wrong merge is the one thing worth reporting. |
| **Needs review** | Pairs that look alike but not enough to be sure. They are **not** merged. | Check each pair. If two really are the same paper, delete one entry by hand in `merged.bib`. |
| **Warnings** | Problems in your input files, with file name and line. | Fix the input file and merge again (see below). |

## 4. Use the merged file

Replace the old `.bib` files in your LaTeX project with `merged.bib`. With biblatex:

```latex
\usepackage[backend=biber]{biblatex}
\addbibresource{merged.bib}
```

Compile as usual (`latexmk -pdf paper.tex`). Old keys such as `\cite{dh76}` still work.

## 5. Command line (no browser)

Useful for scripts or a quick check. Both commands read files and never change them.

See which entries would be merged, and why:

```bash
java -jar bibmerge.jar match paper1.bib paper2.bib
```

Write a merged file:

```bash
java -jar bibmerge.jar merge paper1.bib paper2.bib paper3.bib -o merged.bib
```

Add `--dialect bibtex` for classic BibTeX output. Warnings go to the terminal.

## When something goes wrong

| What you see | Why | Fix |
|---|---|---|
| `java: command not found`, or `UnsupportedClassVersionError` | Java is missing or older than 21 | Install Java 21 (step 1) |
| `Port 8091 was already in use` | Another program uses that port, or BibMerge is already running | Close the other copy, or start on another port: `java -jar bibmerge.jar --server.port=8092` and open http://localhost:8092 |
| The browser shows "Whitelabel Error Page" or a blank page at http://localhost:8091 | The JAR was built without `-Pweb` | Rebuild with `./mvnw -Pweb package`, or use the command line (step 5) |
| `Skipped malformed entry` with a file and line | That entry has a missing `}` or a similar typo | Fix the entry at that line. The rest of the file was still merged. |
| `Duplicate citation key ... in this file` | One file uses the same key twice | Both entries are kept. Rename one key in that file if they are different works. |
| `0 records` for a file | It isn't a BibTeX file, or it's empty | Export the bibliography as `.bib` again from your reference manager or Overleaf |
| `crossref ... is not defined in this file` | An entry points at a parent entry (usually proceedings) that is in another file | Put the parent entry in the same file, or ignore it: the entry is still merged, with less information |
| `Undefined @string` | The file uses an abbreviation like `journal = jacm` without defining it | Add the `@string{...}` line, or write the full name |
| A pair you know is the same paper is under **Needs review** | BibMerge saw some disagreement (year, venue, missing fields) and chose not to guess | Keep one entry by hand. This is intended: a wrong merge would lose a citation. |
| Two different papers were merged | A false merge, which should not happen | Please report it with both original entries; see below. |
| Old keys don't resolve after compiling | The output was classic BibTeX, or the document uses `bibtex` instead of `biber` | Use the biblatex output with `backend=biber` |

### Reporting a problem

Send the team:

1. the command you ran, or what you clicked;
2. the exact message you saw;
3. the two original entries involved (copy them from your `.bib` files).

## Limits

- Made for the bibliographies of a handful of papers (hundreds of entries). 2,000
  entries take about half a minute.
- Each merge starts fresh: BibMerge doesn't store a library between runs. To add a
  new paper, merge again with **all** the `.bib` files, old and new.
- No internet connection is needed, and nothing you upload leaves your computer.
  The browser page talks only to the copy running on your machine.
