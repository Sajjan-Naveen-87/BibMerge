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
| **Needs review** | Pairs that look alike but not enough to be sure. They are **not** merged until you decide. | Click **Same paper** or **Different** on each pair. |
| **Merged** | Pairs judged to be the same work. The **Why** column says why in plain words, e.g. "same DOI, arXiv or ePrint id". | Skim it. If a pair is wrong, click **Not the same**. |
| **Warnings** | Problems in your input files, with file name and line. | Fix the input file and merge again (see below). |

**Show evidence** under any pair shows how similar the two entries are on title,
authors, year, venue and so on, from 0 (different) to 1 (identical). "No information"
means one entry lacked that field, so it didn't count either way.

### Correcting BibMerge

Every click re-runs the merge at once, and `merged.bib` changes to match. Your answers
are listed under **Your decisions**, where **Undo** takes one back. They apply for as
long as the page stays open. A decision is about the entries' content, not their keys,
so it still applies if you upload the same entry again from another file.

If you mark two entries as **Not the same** but each still matches a third entry, the
weakest of those links is undone too and that pair moves to **Needs review**, so you
can decide it as well.

## 4. Use the merged file

Replace the old `.bib` files in your LaTeX project with `merged.bib`. With biblatex:

```latex
\usepackage[backend=biber]{biblatex}
\addbibresource{merged.bib}
```

Compile as usual (`latexmk -pdf paper.tex`). Old keys such as `\cite{dh76}` still work.

### Only have a paper's PDF?

You can upload a PDF instead of a `.bib`. BibMerge reads the paper's reference list
(the section headed *References* or *Bibliography*) and turns each reference into an
entry. It works with the common styles: numbered `[1]`, labelled `[BR93]` and `1.`.

Text read from a PDF is approximate, so BibMerge is stricter with it:

- a PDF reference merges with an entry from a `.bib` only if they share a DOI, arXiv or
  ePrint id, or have exactly the same title, first author and year;
- anything less certain goes to **Needs review** for you to decide;
- when a `.bib` entry and a PDF reference merge, the `.bib` values are kept;
- PDF references have no citation keys of their own, so none are added to `ids`.

Scanned PDFs (pictures of pages, with no selectable text) cannot be read.

### Adding a paper later

Upload your earlier `merged.bib` **together with** the new paper's `.bib` and merge.
Entries already in `merged.bib` keep their keys and their old aliases, the new paper's
references are matched onto them, and only genuinely new works are added.

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
| `No reference list found in this PDF` | The PDF has no *References* / *Bibliography* heading BibMerge recognises | Use the paper's `.bib` file instead (Overleaf or arXiv source) |
| `No text found in this PDF` | A scanned PDF: the pages are images | Use the `.bib` file, or a PDF with selectable text |
| A PDF reference you know is a duplicate is under **Needs review** | PDF references never merge on similarity alone | Click **Same paper** |
| `crossref ... is not defined in this file` | An entry points at a parent entry (usually proceedings) that is in another file | Put the parent entry in the same file, or ignore it: the entry is still merged, with less information |
| `Undefined @string` | The file uses an abbreviation like `journal = jacm` without defining it | Add the `@string{...}` line, or write the full name |
| A pair you know is the same paper is under **Needs review** | BibMerge saw some disagreement (year, venue, missing fields) and chose not to guess | Click **Same paper**. This is intended: a wrong merge would lose a citation. |
| Two different papers were merged | A false merge | Click **Not the same**, and please report it with both original entries (see below) |
| Old keys don't resolve after compiling | The output was classic BibTeX, or the document uses `bibtex` instead of `biber` | Use the biblatex output with `backend=biber` |

### Reporting a problem

Send the team:

1. the command you ran, or what you clicked;
2. the exact message you saw;
3. the two original entries involved (copy them from your `.bib` files).

## Limits

- Made for the bibliographies of a handful of papers (hundreds of entries). 2,000
  entries take about half a minute.
- BibMerge doesn't store anything between visits. To add a paper, upload your earlier
  `merged.bib` with the new file. Your **Same paper / Different** answers last until
  you close or reload the page.
- No internet connection is needed, and nothing you upload leaves your computer.
  The browser page talks only to the copy running on your machine.
