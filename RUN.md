# Running and demoing BibMerge (macOS)

A step-by-step sheet for getting BibMerge up on a Mac and showing it to someone.
Do the one-time setup, pick **one** way to run it (A, B or C), then follow the
[demo script](#demo-script).

Every option ends with the web app at **http://localhost:8091**.

## Terminals

Use the **Terminal** app. Open the first window, then **Cmd+T** for each extra tab.
Every command below is marked with the terminal (tab) to run it in.

| Label | What it's for |
|---|---|
| **T1** | Setup, then the BibMerge server. Leave it running for the whole demo. |
| **T2** | Everything else while the server runs: health check, command-line demo, tests. In Option C it runs the front end instead. |
| **T3** | Only in Option C: health check and command-line demo. |

After the setup step, every terminal must be in the project folder:

```bash
cd ~/BibMerge
```

(Change the path if you cloned it somewhere else.)

---

## One-time setup on the Mac

### 1. Get the code

**T1:** if the project isn't on the Mac yet:

```bash
cd ~ && git clone https://github.com/Sajjan-Naveen-87/BibMerge.git && cd BibMerge
```

If it's already there, update it instead:

```bash
cd ~/BibMerge && git checkout main && git pull
```

The built JAR and the saved Docker image (`bibmerge-final.tar.gz`) are **not** in git,
so you build them on the Mac (below). Building on the Mac also means everything runs
natively on Apple Silicon.

### 2. Install the tools

You need **either** Java 21 + Node 22 (Options A and C) **or** Docker Desktop (Option B).
Doing both gives you a fallback during the demo.

**T1:** install Homebrew first if `brew --version` fails (see https://brew.sh).

For Options A and C, **T1:**

```bash
brew install --cask temurin@21
```

```bash
brew install node@22
```

`node@22` is not put on your PATH automatically. **T1:** add it, then reload:

```bash
echo 'export PATH="$(brew --prefix node@22)/bin:$PATH"' >> ~/.zshrc && source ~/.zshrc
```

**T1:** check both. Java's first line must say 21 or higher, Node must say v22:

```bash
java -version && node -v
```

For Option B, **T1:**

```bash
brew install --cask docker
```

Then open **Docker** from Applications once and wait until the whale icon in the menu
bar says it's running. Docker Desktop must be running every time you use Option B.

### 3. Check the port

**T1:** check that port 8091 is free (no output means free):

```bash
lsof -nP -iTCP:8091 -sTCP:LISTEN
```

If something is listed, stop it or use another port (see each option).

---

## Option A: the JAR (recommended, needs Java 21 + Node 22)

**T1:** build it once (a few minutes the first time; downloads Maven and npm packages).
The `-Pweb` flag is what puts the web page inside the JAR:

```bash
./mvnw -Pweb package -DskipTests
```

**T1:** start the server:

```bash
java -jar bibmerge-api/target/bibmerge.jar
```

Wait for `Started BibMergeApplication`, then open http://localhost:8091 in Safari or Chrome.
Stop it with **Ctrl+C** (Control, not Cmd) in T1. Other port: add `--server.port=9000`.

---

## Option B: Docker (needs only Docker Desktop)

**T1:** build the image on the Mac (a few minutes the first time):

```bash
docker build -t bibmerge:final .
```

Don't use `bibmerge-final.tar.gz` from the Linux machine on an Apple Silicon Mac: it is
an Intel (amd64) image and would run slowly under emulation.

**T1:** run it:

```bash
docker run --rm -p 8091:8091 bibmerge:final
```

Open http://localhost:8091. Stop with **Ctrl+C** in T1.
Other port: `-p 9000:8091`, then open http://localhost:9000.

---

## Option C: developer mode with hot reload

Needs the JAR from Option A built first.

**T1:** the backend:

```bash
java -jar bibmerge-api/target/bibmerge.jar
```

(After changing Java code, stop T1, rebuild with `./mvnw package -DskipTests`, start again.)

**T2:** the front end:

```bash
cd bibmerge-web && npm install && npm run dev
```

Open http://localhost:5173. Vite forwards `/api` calls to the backend on 8091.
Stop each with **Ctrl+C** in its own terminal. Use **T3** wherever the rest of
this sheet says T2.

---

## Quick check that it's working

**T2** (T3 in Option C), while the server runs in T1:

```bash
curl http://localhost:8091/api/health
```

Expected: `{"status":"ok","version":"0.1.0-SNAPSHOT"}`

---

## Demo script

About 5 minutes. Uses the two sample files in `demo/`: `paper-a.bib` and
`paper-b.bib` (12 entries in total, written to cover the tricky duplicate cases).
The server stays running in **T1** throughout.

### 1. The problem (30 s)

**T2:** open both sample files side by side:

```bash
open -a TextEdit demo/paper-a.bib demo/paper-b.bib
```

Point out that the same papers appear under different keys and formatting, e.g.
`dh76` vs `DiffieH76` (Diffie-Hellman) and `vaswani2017attention` vs
`NIPS2017_attention`. Merging these by hand is what BibMerge replaces.

### 2. Merge in the browser (2 min)

1. Open http://localhost:8091.
2. **Choose files**, go to the project's `demo/` folder and select both files
   (**Cmd+click** the second one).
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

Click **Download merged.bib**. It lands in `~/Downloads`. **T2:** open it:

```bash
open -a TextEdit ~/Downloads/merged.bib
```

Each merged entry has an `ids = {...}` field with the old keys, so existing
`\cite{dh76}` commands keep working with biblatex/biber.

### 5. Optional extras

- **PDF input:** upload a paper's PDF instead of a `.bib`; BibMerge reads its
  reference list. (Have a text-based PDF ready; scanned PDFs can't be read.)
- **Adding a paper later:** upload `merged.bib` together with a new paper's
  `.bib` to map the new references onto the existing entries.
- **Command line** (no server needed), in **T2** (T3 in Option C):

  ```bash
  java -jar bibmerge-api/target/bibmerge.jar match demo/paper-a.bib demo/paper-b.bib
  ```

  ```bash
  java -jar bibmerge-api/target/bibmerge.jar merge demo/*.bib -o merged.bib
  ```

  With Docker instead, also in **T2**:

  ```bash
  docker run --rm bibmerge:final match demo/paper-a.bib demo/paper-b.bib
  ```

- **Tests**, in **T2**: `./mvnw verify` runs the whole test suite.

---

## Before the demo day

Do a full dry run on the Mac a day early: build, start, merge the demo files, download.
The first build downloads packages, so do it on good Wi-Fi, not at the venue.

---

## If something goes wrong

| Problem | Fix |
|---|---|
| `Port 8091 was already in use` (in T1) | Run `lsof -nP -iTCP:8091 -sTCP:LISTEN` to see what holds it, stop it, or use `--server.port=9000` (JAR) / `-p 9000:8091` (Docker). |
| `zsh: permission denied: ./mvnw` | Run `chmod +x mvnw` once. |
| `npm: command not found` or `exec ... npm` fails during the build | Node isn't on PATH. Redo the `node@22` PATH step and open a new terminal. |
| `Unable to locate a Java Runtime` or `UnsupportedClassVersionError` | Java 21 isn't installed or isn't the default. Run `brew install --cask temurin@21`, then open a new terminal. |
| `Cannot connect to the Docker daemon` | Docker Desktop isn't running. Open it from Applications and wait for it to start. |
| Page shows a white-label error / 404 at `/` | The JAR was built without the UI. Stop T1, rebuild with `./mvnw -Pweb package -DskipTests`, start again. |
| Browser can't connect | Wait for `Started BibMergeApplication` in T1 first. |
| macOS asks to allow incoming connections for `java` | Click **Allow** (or Deny: localhost works either way). |

More detail for end users: [doc/USER-GUIDE.md](doc/USER-GUIDE.md).
