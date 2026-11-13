# Running and demoing BibMerge

A step-by-step sheet for getting BibMerge up and showing it to someone.
Pick **one** way to run it (A, B or C), then follow the [demo script](#demo-script).

Every option ends with the web app at **http://localhost:8091**.

---

## Before you start

Check that port 8091 is free (no output means free):

```bash
ss -ltn | grep 8091
```

If something is already there, either stop it or use another port (see each option).

---

## Option A: the ready-built JAR (fastest, needs Java 21)

Check Java:

```bash
java -version
```

The first line must say 21 or higher. If not: `sudo apt install openjdk-21-jre`.

Build the JAR once (skip this if `bibmerge-api/target/bibmerge.jar` already exists).
This needs JDK 21 and Node 22, and the `-Pweb` flag is what puts the web page inside:

```bash
./mvnw -Pweb package -DskipTests
```

Start it from the project folder:

```bash
java -jar bibmerge-api/target/bibmerge.jar
```

Wait for `Started BibMergeApplication`, then open http://localhost:8091.
Stop it with **Ctrl+C**. Other port: add `--server.port=9000`.

---

## Option B: Docker (no Java or Node needed)

The image `bibmerge:final` is already built on this machine. Check:

```bash
docker images bibmerge
```

If it is missing, either load the saved image that ships with the project:

```bash
docker load -i bibmerge-final.tar.gz
```

or build it from source (takes a few minutes the first time):

```bash
docker build -t bibmerge:final .
```

Run it:

```bash
docker run --rm -p 8091:8091 bibmerge:final
```

Open http://localhost:8091. Stop with **Ctrl+C**.
Other port: `-p 9000:8091`, then open http://localhost:9000.

---

## Option C: developer mode with hot reload

Two terminals. Terminal 1, the backend:

```bash
java -jar bibmerge-api/target/bibmerge.jar
```

(After changing Java code, rebuild first with `./mvnw package -DskipTests`.)

Terminal 2, the front end:

```bash
cd bibmerge-web && npm install && npm run dev
```

Open http://localhost:5173. Vite forwards `/api` calls to the backend on 8091.

---

## Quick check that it's working

```bash
curl http://localhost:8091/api/health
```

Expected: `{"status":"ok","version":"0.1.0-SNAPSHOT"}`

---

## Demo script

About 5 minutes. Uses the two sample files in `demo/`: `paper-a.bib` and
`paper-b.bib` (12 entries in total, written to cover the tricky duplicate cases).

### 1. The problem (30 s)

Open `demo/paper-a.bib` and `demo/paper-b.bib` side by side. Point out that the
same papers appear under different keys and formatting, e.g. `dh76` vs `DiffieH76`
(Diffie-Hellman) and `vaswani2017attention` vs `NIPS2017_attention`.
Merging these by hand is what BibMerge replaces.

### 2. Merge in the browser (2 min)

1. Open http://localhost:8091.
2. **Choose files**, select both files in `demo/` (Ctrl+click for the second).
3. Keep output style **biblatex**, click **Merge**.
4. Show the summary. Expected: **12 records → 8 entries, 4 merges, 0 for review,
   1 warning.**

Walk through the **Merged** list and its **Why** column:

| Pair | Why it merged |
|---|---|
| `dh76` ↔ `DiffieH76` | same DOI, different key and formatting |
| `BR93` ↔ `DBLP:conf/ccs/BellareR93` | no DOI on one side, matched via the DBLP key |
| `cryptoeprint:2020/123` ↔ `ExampleS21` | ePrint preprint vs its conference version, by similarity score |
| `vaswani2017attention` ↔ `NIPS2017_attention` | same arXiv id, even though one author list is cut short |

Click **Show evidence** on one pair to show the per-field scores (title, authors, year, venue).

Then point out what it correctly did **not** merge:

- `BR93` and `BR93entity`: same authors, same year, different papers.
- `Sieve-I` and `Sieve-II`: Part I vs Part II of a paper.

And the **Warnings** section: the deliberately broken entry in `paper-b.bib`
(line 28) is skipped and reported with file and line, not silently dropped.

### 3. Human in the loop (1 min)

On any merged pair click **Not the same**. The merge re-runs instantly and the
pair splits. It shows under **Your decisions**; click **Undo** to take it back.
Uncertain pairs never merge automatically, they wait under **Needs review**.

### 4. Download and use (30 s)

Click **Download merged.bib** and open it. Each merged entry has an
`ids = {...}` field with the old keys, so existing `\cite{dh76}` commands keep
working with biblatex/biber.

### 5. Optional extras

- **PDF input:** upload a paper's PDF instead of a `.bib`; BibMerge reads its
  reference list. (Have a text-based PDF ready; scanned PDFs can't be read.)
- **Adding a paper later:** upload `merged.bib` together with a new paper's
  `.bib` to map the new references onto the existing entries.
- **Command line** (no server):

  ```bash
  java -jar bibmerge-api/target/bibmerge.jar match demo/paper-a.bib demo/paper-b.bib
  ```

  ```bash
  java -jar bibmerge-api/target/bibmerge.jar merge demo/*.bib -o merged.bib
  ```

  With Docker instead:

  ```bash
  docker run --rm bibmerge:final match demo/paper-a.bib demo/paper-b.bib
  ```

- **Tests:** `./mvnw verify` runs the whole test suite.

---

## If something goes wrong

| Problem | Fix |
|---|---|
| `Port 8091 was already in use` | Stop the other program, or use `--server.port=9000` (JAR) / `-p 9000:8091` (Docker). |
| Page shows a white-label error / 404 at `/` | The JAR was built without the UI. Rebuild with `./mvnw -Pweb package -DskipTests`. |
| `UnsupportedClassVersionError` | Java is older than 21. Install JDK 21. |
| `docker: permission denied` | Run with `sudo`, or add yourself to the `docker` group. |
| Browser can't connect | Wait for `Started BibMergeApplication` in the terminal first. |

More detail for end users: [doc/USER-GUIDE.md](doc/USER-GUIDE.md).
