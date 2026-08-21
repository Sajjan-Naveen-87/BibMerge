# BibMerge

**Automated deduplication and merging of BibTeX references across papers.**

BibMerge is a web application that imports the `.bib` bibliographies from
multiple academic papers, automatically detects references that point to the
same work (even when they use different citation keys or inconsistent
formatting), and merges them into a single, clean bibliography. When new
papers are imported later, their references are mapped onto the existing
canonical entries to prevent duplication.

CS5013 course project, IIT Madras.

## The problem

Researchers routinely combine references from many papers — for a survey, a
thesis, or a shared group library. The same cited work often appears more
than once under different citation keys and formatting, and reconciling these
duplicates by hand is slow and error-prone. BibMerge automates that.

## Features

- Import and parse multiple `.bib` files.
- Detect duplicate references — DOI-first, then fuzzy matching on
  title / author / year.
- Merge duplicates into a single canonical entry.
- Incrementally import new papers and map their references onto existing
  entries.
- Review uncertain matches before merging; export a clean merged `.bib`.

## Tech stack

| Layer     | Technology                                             |
|-----------|--------------------------------------------------------|
| Frontend  | React + TypeScript (Vite)                              |
| Backend   | Java, Spring Boot (Spring Web REST API)               |
| Build     | Maven (backend), npm + Vite (frontend)                |
| Key libs  | JBibTeX, Apache Commons Text, JUnit 5, Axios          |

## Team

| Member       | Roll No.  | Dept. | GitHub            |
|--------------|-----------|-------|-------------------|
| S Naveen     | CS26E010  | CSE   | Sajjan-Naveen-87  |
| Gowni Vamshi | CS26E005  | CSE   | vamshiG24         |

**Stakeholder:** Ashrujit Ghoshal, Assistant Professor, Department of
Computer Science and Engineering, IIT Madras.

## Milestones

- **11 Sep 2026 (Design doc):** scope locked; frontend and backend
  scaffolded; single-file parsing and matching strategy prototyped.
- **09 Oct 2026 (Mid-demo):** core upload → merge → download workflow running
  end-to-end on real input.
- **06 Nov 2026 (Final):** stakeholder can use the deployed web app to merge
  papers and incrementally import new ones.

## Repository structure

```
.
├── README.md
└── doc/
    ├── proposal.tex          # Project proposal (LaTeX source)
    ├── stakeholder-email.pdf # Stakeholder acknowledgement (Appendix A)
    └── proposal-template.pdf # Course-provided template
```

## Building the proposal PDF

The proposal lives in `doc/proposal.tex` and embeds
`doc/stakeholder-email.pdf` as Appendix A. Compile with any LaTeX toolchain
(both files must be in the same folder), e.g.:

```bash
cd doc
latexmk -pdf proposal.tex
```

or paste `proposal.tex` and `stakeholder-email.pdf` into an
[Overleaf](https://www.overleaf.com) project.
