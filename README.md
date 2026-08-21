# BibMerge

**Automated deduplication and merging of BibTeX references across papers.**

BibMerge is a web application that imports the `.bib` bibliographies from
multiple academic papers, automatically detects references that point to the
same work (even when they use different citation keys or inconsistent
formatting), and merges them into a single, clean bibliography. When new
papers are imported later, their references are mapped onto the existing
canonical entries to prevent duplication.

CS5013 (Programming with AI) course project, Department of Computer Science
and Engineering, IIT Madras.

## Motivation

Researchers routinely combine references from many papers — for a survey, a
thesis, or a shared group library. Each paper ships its own `.bib` file, and
the same cited work frequently reappears under a different citation key, with
fields formatted or ordered differently, or with small inconsistencies in the
title, author list, or venue. Today the only way to reconcile these is by
hand: open several `.bib` files side by side, visually spot that two
differently labelled entries are the same paper, merge them, and repeat the
whole check every time a new paper is added.

This is tedious, slow, and error-prone — a missed duplicate produces a
bibliography with the same reference listed twice under two keys, and a
mistaken merge silently drops a citation. The manual effort also does not
scale: it grows with every new paper and must be redone from scratch each
time. BibMerge exists to turn this recurring manual chore into a one-click
operation and to keep a stable, canonical set of references as new papers
arrive.

## Stakeholder

**Ashrujit Ghoshal** — Assistant Professor, Department of Computer Science
and Engineering, IIT Madras (aRtCS / CyStar; research in cryptography).

As a research-active faculty member, Dr. Ghoshal regularly writes and
combines papers and directly experiences the pain of managing overlapping
bibliographies. We first spoke with him on **18 August 2026**, drafted a
formal problem statement from that conversation, and he confirmed it on
**20 August 2026**. He agreed that an automated tool to parse, deduplicate,
and merge `.bib` files — and to keep a stable canonical entry as further
papers are imported — would be useful to him and to his group. His
acknowledgement is attached as Appendix A of the proposal
(`doc/stakeholder-email.pdf`).

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
