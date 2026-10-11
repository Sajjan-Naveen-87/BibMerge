from reportlab.lib.pagesizes import A4, landscape
from reportlab.pdfgen import canvas
from reportlab.lib import colors

import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "BibMerge-Architecture.pdf")
W, H = landscape(A4)
c = canvas.Canvas(OUT, pagesize=(W, H))
c.setTitle("BibMerge — System Architecture and Data Flow"); c.setAuthor("S Naveen, Gowni Vamshi")
INK = colors.HexColor("#1f2937"); MUTED = colors.HexColor("#6b7280"); ACC = colors.HexColor("#1d4e89")
NAV = (colors.HexColor("#e6effa"), colors.HexColor("#2c5d8f"))      # Naveen
VAM = (colors.HexColor("#e9f6ec"), colors.HexColor("#2e7d32"))      # Vamshi
SHR = (colors.HexColor("#fff4e0"), colors.HexColor("#b8860b"))      # shared
IO = (colors.HexColor("#f3f4f6"), colors.HexColor("#6b7280"))

def box(x, y, w, h, title, lines, pal, tsize=9.6, out=None):
    fill, stroke = pal
    c.setFillColor(fill); c.setStrokeColor(stroke); c.setLineWidth(1.1)
    c.roundRect(x, y, w, h, 6, fill=1, stroke=1)
    c.setFillColor(stroke); c.setFont("Helvetica-Bold", tsize); c.drawString(x + 7, y + h - 14, title)
    c.setFillColor(INK); c.setFont("Helvetica", 7.3)
    for i, l in enumerate(lines):
        c.drawString(x + 7, y + h - 26 - i * 9.4, l)
    if out:
        c.setFillColor(stroke); c.setFont("Helvetica-Oblique", 7.1); c.drawString(x + 7, y + 7, "out: " + out)

def arrow(x1, y1, x2, y2, label=None, lx=0, ly=4):
    c.setStrokeColor(MUTED); c.setFillColor(MUTED); c.setLineWidth(1)
    c.line(x1, y1, x2, y2)
    import math
    a = math.atan2(y2 - y1, x2 - x1); s = 5
    p = c.beginPath(); p.moveTo(x2, y2)
    p.lineTo(x2 - s * math.cos(a - 0.4), y2 - s * math.sin(a - 0.4))
    p.lineTo(x2 - s * math.cos(a + 0.4), y2 - s * math.sin(a + 0.4)); p.close()
    c.drawPath(p, fill=1, stroke=0)
    if label:
        c.setFont("Helvetica-Oblique", 6.9); c.setFillColor(ACC)
        c.drawCentredString((x1 + x2) / 2 + lx, (y1 + y2) / 2 + ly, label)

# Title
c.setFillColor(ACC); c.setFont("Helvetica-Bold", 16); c.drawString(30, H - 38, "BibMerge — system architecture and data flow")
c.setFillColor(MUTED); c.setFont("Helvetica", 8.5)
c.drawString(30, H - 52, "CS5013 mid-evaluation · S Naveen (CS26E010), Gowni Vamshi (CS26E005) · github.com/Sajjan-Naveen-87/BibMerge")

# Row 0: inputs and entry points
box(30, H - 140, 150, 66, "Input", [".bib files from several papers", "(paper-a.bib, paper-b.bib, ...)", "any mix of DBLP, ePrint, Scholar"], IO)
box(225, H - 140, 175, 66, "Web UI  (bibmerge-web)", ["React + TypeScript (Vite)", "upload files -> merge -> download", "shows merges, reviews, warnings"], SHR)
box(445, H - 140, 175, 66, "REST API  (bibmerge-api)", ["Spring Boot 3", "POST /api/merge (multipart)", "GET /api/health"], SHR)
box(665, H - 140, 147, 66, "CLI  (same JAR)", ["bibmerge match a.bib b.bib", "bibmerge merge *.bib -o out.bib", "no Spring startup"], SHR)
arrow(180, H - 107, 225, H - 107)
arrow(400, H - 107, 445, H - 107, "HTTP")
c.setStrokeColor(MUTED); c.setDash(3, 2); c.line(105, H - 140, 105, H - 152); c.line(105, H - 152, 738, H - 152); c.line(738, H - 152, 738, H - 140); c.setDash()
c.setFont("Helvetica-Oblique", 6.9); c.setFillColor(ACC); c.drawString(560, H - 162, "files given directly to the CLI")

# Engine frame
fx, fy, fw, fh = 30, 92, W - 60, H - 270
c.setStrokeColor(ACC); c.setLineWidth(1.4); c.setFillColor(colors.HexColor("#fbfcfe"))
c.roundRect(fx, fy, fw, fh, 8, fill=1, stroke=1)
c.setFillColor(ACC); c.setFont("Helvetica-Bold", 10.5)
c.drawString(fx + 10, fy + fh - 16, "bibmerge-core — merge engine (plain Java 21, no Spring: enforced by Maven enforcer, ADR-1)")
c.setFont("Helvetica", 7.6); c.setFillColor(MUTED)
c.drawString(fx + 10, fy + fh - 28, "MergePipeline.run(files): stateless, deterministic; each stage a pure, separately tested function")
arrow(532, H - 140, 532, fy + fh, "MergePipeline", lx=-32, ly=-4)
arrow(738, H - 152, 738, fy + fh)

bw, bh, gap = 132, 100, 26
r1y = fy + fh - 44 - bh
xs = [fx + 14 + i * (bw + gap) for i in range(5)]
stages1 = [
    ("1  Parse", ["JBibTeX, one entry per chunk", "@string, @preamble, encodings", "LaTeX + HTML decoding", "bad entry -> diagnostic only"], NAV),
    ("1.5  Inherit", ["crossref resolution", "parent title -> booktitle", "(DBLP year/venue)", "containers flagged"], NAV),
    ("2  Normalize", ["DOI / arXiv / IACR ePrint ids", "preprint vs version of record", "title, 4-part author names", "venue, year, pages"], NAV),
    ("4  Block", ["candidate pairs", "all-pairs (MinHash-LSH", "planned for large libraries)", "never parent vs own child"], NAV),
    ("5  Features", ["title Jaro-Winkler, 3-gram", "author F1 / first author", "year, venue, pages, type", "id agree / conflict"], NAV),
]
outs1 = ["SourceRecord", "SourceRecord (+inherited)", "NormalizedRecord", "CandidatePair", "FeatureVector"]
for (t, l, p), x, o in zip(stages1, xs, outs1):
    box(x, r1y, bw, bh, t, l, p, out=o)
for i in range(4):
    arrow(xs[i] + bw, r1y + bh / 2, xs[i + 1], r1y + bh / 2)

r2y = fy + 22
stages2 = [
    ("6  Decide", ["rule score = weighted mean", "vetoes: id conflict, type,", "part number; version policy", "-> MERGE / REVIEW / DISTINCT"], NAV),
    ("7  Cluster", ["union-find over MERGE edges", "path compression,", "union by size", "transitive clusters"], VAM),
    ("8  Golden record", ["field choice: version of record,", "then DOI, then longest", "key = family+year+word,", "never another entry's old key"], VAM),
    ("9  Serialize", ["BibWriter: sorted, fixed", "field order, braces kept", "ids = {old keys} (biblatex)", "round-trips through parser"], VAM),
    ("Output", ["merged.bib", "review list (uncertain pairs)", "parse diagnostics", "counts: records -> entries"], IO),
]
outs2 = ["PairDecision", "Cluster", "CanonicalEntry", ".bib text", None]
for (t, l, p), x, o in zip(stages2, list(reversed(xs)), outs2):
    box(x, r2y, bw, bh, t, l, p, out=o)
# Features (row1 last) -> Decide (row2 first = rightmost)
arrow(xs[4] + bw / 2, r1y, xs[4] + bw / 2, r2y + bh)
rx = list(reversed(xs))
for i in range(4):
    arrow(rx[i], r2y + bh / 2, rx[i + 1] + bw, r2y + bh / 2)

# Legend
ly = 40
for i, (label, pal) in enumerate([("S Naveen — stages 1-6, evaluation", NAV), ("Gowni Vamshi — stages 7-9", VAM),
                                  ("Shared — pipeline, CLI, API, UI", SHR), ("Input / output", IO)]):
    x = 30 + i * 200
    c.setFillColor(pal[0]); c.setStrokeColor(pal[1]); c.roundRect(x, ly, 14, 10, 2, fill=1, stroke=1)
    c.setFillColor(INK); c.setFont("Helvetica", 8); c.drawString(x + 20, ly + 2, label)
c.setFillColor(MUTED); c.setFont("Helvetica", 7.4)
c.drawString(30, 24, "REVIEW pairs are never merged automatically; they are reported for a human. "
             "Tests: 164 (unit per stage, API, golden tables, synthetic two-paper corpus, real IACR ePrint records). Design: design/design.md.")
c.showPage(); c.save(); print("ok")
