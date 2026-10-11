from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
                                KeepTogether)

import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "BibMerge-Principles-and-Practices.pdf")
ss = getSampleStyleSheet()
INK = colors.HexColor("#1f2937"); ACCENT = colors.HexColor("#1d4e89"); RULE = colors.HexColor("#c7d2e0")
body = ParagraphStyle("b", parent=ss["Normal"], fontName="Helvetica", fontSize=8.9, leading=11.4, textColor=INK, spaceAfter=3)
bullet = ParagraphStyle("bl", parent=body, leftIndent=10, bulletIndent=2, spaceAfter=1.5)
h1 = ParagraphStyle("h1", parent=ss["Title"], fontName="Helvetica-Bold", fontSize=14.5, leading=17, textColor=ACCENT, spaceAfter=1, alignment=0)
sub = ParagraphStyle("sub", parent=body, fontSize=8.6, textColor=colors.HexColor("#4b5563"), spaceAfter=5)
h2 = ParagraphStyle("h2", parent=ss["Heading2"], fontName="Helvetica-Bold", fontSize=10.6, leading=13, textColor=ACCENT, spaceBefore=6, spaceAfter=2.5, keepWithNext=1)
cell = ParagraphStyle("c", parent=body, fontSize=8.1, leading=10, spaceAfter=0)
cellb = ParagraphStyle("cb", parent=cell, fontName="Helvetica-Bold")
mono = ParagraphStyle("m", parent=body, fontName="Courier", fontSize=7.6, leading=9.3, leftIndent=6, textColor=colors.HexColor("#111827"),
                      backColor=colors.HexColor("#f3f6fa"), borderPadding=(3, 4, 3, 4), spaceBefore=2, spaceAfter=5)

def P(t, s=body): return Paragraph(t, s)
def B(items): return [Paragraph(i, bullet, bulletText="•") for i in items]
def table(rows, widths):
    t = Table([[P(c, cellb if r == 0 else cell) for c in row] for r, row in enumerate(rows)], colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e8eef6")),
        ("GRID", (0, 0), (-1, -1), 0.4, RULE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 2.2), ("BOTTOMPADDING", (0, 0), (-1, -1), 2.2),
        ("LEFTPADDING", (0, 0), (-1, -1), 3.5), ("RIGHTPADDING", (0, 0), (-1, -1), 3.5)]))
    return t

W = A4[0] - 3.4 * cm
s = []
s += [P("BibMerge — Principles &amp; Practices", h1),
      P("CS5013 Programming with AI · Mid-evaluation deliverable · S Naveen (CS26E010) and Gowni Vamshi (CS26E005) · "
        "github.com/Sajjan-Naveen-87/BibMerge · 11 Oct 2026", sub),
      P("BibMerge merges the <font face='Courier'>.bib</font> files of several papers into one bibliography, recognising the same "
        "work under different keys and formatting. The engine is plain Java 21 (9 pipeline stages), served by a Spring Boot API and CLI. "
        "Every practice below is something we actually did; the evidence is in the repository: 5 merged PRs, 26 commits by both members, "
        "164 tests, and the prompt log in <font face='Courier'>doc/prompts/</font>.")]

s += [P("1. Prompt engineering", h2),
      P("Our prompts improved most when we stopped asking for features and started giving the model the same inputs a teammate would get. "
        "Five principles we arrived at during the project (originals vs refined prompts in the Appendix):"),
      *B(["<b>Context before task.</b> Point the model at the design-doc section, the existing interfaces and the current repository state "
          "before asking for code (P1, P2). A handoff document said the design doc existed; the repository audit showed it had never been pushed.",
          "<b>One unit of work per prompt.</b> One pipeline stage or one bug per prompt, one commit per answer, so every answer can be reviewed and reverted on its own.",
          "<b>Constraints and non-goals stated explicitly.</b> Language, package, interfaces that must not change, and what not to touch: "
          "\"do not write Vamshi's modules\" (P3) kept ownership honest.",
          "<b>Executable acceptance criteria.</b> Every prompt names the tests that must pass and the command to run; we ask for test counts back, not \"done\".",
          "<b>Make the model verify against reality.</b> \"Run it on the stakeholder's real papers and explain every decision\" (P4) found a bug no fixture had."]),
      P("Template we now use: <i>Context → Task → Constraints → Acceptance → Report</i> (Appendix). Each logged prompt names the commit it produced.")]

s += [P("2. Debugging", h2),
      P("Our loop is <b>reproduce → shrink to a failing test → hypothesise → fix → full suite → keep the test</b>. We never fix a bug we cannot reproduce in a test, "
        "and every fix lands with its regression test in the same commit."),
      table([["Symptom (how found)", "Root cause", "Fix + guard"],
             ["One unclosed brace lost every later entry (parser probe)", "JBibTeX consumes to EOF on unbalanced braces", "Parse per entry chunk; test <i>malformedEntryLosesOnlyItself</i>"],
             ["Accented names lost their accents: Erd\\H{o}s became \"Erdos\" (golden table test)","Library LaTeX printer silently drops \\H and \\i", "Own decoder; 17-case parameterised table"],
             ["\"DOI: 10.1109/…\" produced no DOI (table test)", "Value cut at first space before prefix strip", "Reordered; 16 DOI variants tested"],
             ["Part I vs Part II scored 0.976 (printed every pair's score)", "Title similarity cannot see part numbers", "Part-number veto + synthetic corpus case"],
             ["Two different ePrint papers sent to review (real data)", "ePrint record carried its published DOI, bypassing the id veto", "Different preprint ids always veto; 4 tests"],
             ["\\cite{oldkey} could resolve to another paper (edge probe)", "Minted key equal to another entry's retired key", "Block other entries' keys; pipeline test"]],
            [5.2 * cm, 5.4 * cm, W - 10.6 * cm]),
      P("Two process lessons: print the evidence (scores, rule fired, features) for every decision rather than only pass/fail, which is how the Part I/II "
        "bug surfaced; and probe module <i>boundaries</i> after integration, because each module passed its own tests while the key collision only "
        "existed between clustering and golden-record building.")]

s += [P("3. Version control (Git) for code and prompts", h2),
      *B(["<b>Branch per unit, PR per branch, CI on every PR.</b> GitHub Actions runs <font face='Courier'>mvn verify</font> and the frontend lint/build; "
          "we merge on green, with merge commits so each member's history stays visible (<font face='Courier'>git shortlog</font>: 18 / 8).",
          "<b>One commit per stage with a why-message</b> (e.g. \"Resolve crossref inheritance (Stage 1.5)\" explains the DBLP case it fixes). "
          "AI-written commits carry a <font face='Courier'>Co-Authored-By</font> trailer, so AI involvement is auditable in <font face='Courier'>git log</font>.",
          "<b>Prompts are versioned like code.</b> <font face='Courier'>doc/prompts/README.md</font> logs each significant prompt with the commit or PR it produced; "
          "task specs (<font face='Courier'>doc/tasks/</font>) and the design doc are prompts too and live beside the code.",
          "<b>Ownership through Git.</b> Partner modules were handed over as interfaces + <font face='Courier'>@Disabled</font> spec tests, implemented from the partner's own account.",
          "<b>Data hygiene.</b> The stakeholder's real corpus is git-ignored (<font face='Courier'>corpus.local/</font>, ADR-14).",
          "<b>Mistake we fixed:</b> a stacked PR (#3) merged into its base branch after that base had already merged, so <font face='Courier'>main</font> silently missed "
          "work. We now check <font face='Courier'>git rev-list main..branch</font> for every branch; PR #4 brought everything in."])]

s += [P("4. Testing, refactoring and documentation", h2),
      table([["Practice", "How we apply it"],
             ["Tests per stage", "23 test classes, 164 tests, none skipped. Normalizers use parameterised golden tables of real-world variants (DOI, LaTeX, author names)."],
             ["End-to-end + real data", "Synthetic two-paper corpus whose README lists each duplicate class and expected outcome; acceptance test: 12 records → 8 entries, 4 merges. Real IACR ePrint records: 0 false merges in 153 pairs."],
             ["Spec tests as contracts", "Interfaces shipped with disabled tests stating exact behaviour; enabling them was the definition of done."],
             ["Architecture enforced", "Maven enforcer bans Spring from the engine module (ADR-1), so logic cannot leak into controllers."],
             ["Refactoring with a safety net", "Replaced the library LaTeX printer, reordered DOI normalization, added decision-ladder rules — each only with the full suite green before and after."],
             ["Documentation", "Design doc (architecture, ADRs, test plan) in repo; Javadoc on the engine and API types stating <i>why</i>; README with build/run; task specs; synthetic-corpus README; PR descriptions list deviations and known gaps."]],
            [3.6 * cm, W - 3.6 * cm]),
      P("<b>Known gaps</b> (stated, not hidden): blocking is all-pairs (fine for a few papers, not for 10k records); venue abbreviation dictionary and review UI "
        "not built; BibTeX/biber compile check not yet automated in CI.")]

orig = ParagraphStyle("o", parent=cell, fontName="Helvetica-Oblique", textColor=colors.HexColor("#374151"))
refd = ParagraphStyle("r", parent=cell, fontName="Courier", fontSize=7.3, leading=8.9)
def pair_table(rows):
    data = [[P("Our original prompt (verbatim)", cellb), P("Refined with the template (what we write now)", cellb), P("Led to", cellb)]]
    for o, r, led in rows:
        data.append([P(o, orig), P(r, refd), P(led, cell)])
    t = Table(data, colWidths=[4.3 * cm, W - 6.6 * cm, 2.3 * cm], repeatRows=1)
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e8eef6")),
        ("BACKGROUND", (1, 1), (1, -1), colors.HexColor("#f3f6fa")),
        ("GRID", (0, 0), (-1, -1), 0.4, RULE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 2.5), ("BOTTOMPADDING", (0, 0), (-1, -1), 2.5),
        ("LEFTPADDING", (0, 0), (-1, -1), 3.5), ("RIGHTPADDING", (0, 0), (-1, -1), 3.5)]))
    return t

s += [P("Appendix — sample prompts (full log: doc/prompts/README.md)", h2),
      P("Left: prompts exactly as we first typed them (typos included). Right: the same request rewritten with our template; every added "
        "line is a constraint or check we had to supply later in the conversation, which is the lesson. We now write the right-hand form up front."),
      pair_table([
        ("now make the changes in the code base to do some of the left behind commits and reschedule the uncomiplited commits",
         "Context: design doc §6.6, §6.2, §5; branch from main.<br/>Task: my modules, stages 1–6.<br/>"
         "Constraints: one commit per stage; do not touch stages 7–9 (Vamshi's).<br/>"
         "Acceptance: unit tests per stage; full build green after each commit.<br/>Report: test count per commit.",
         "PR #2"),
        ("do thw wors need to be done today",
         "Context: Vamshi owns stages 7–9 and has no commits yet.<br/>Task: starter for his modules.<br/>"
         "Constraints: do NOT implement them; stubs that throw TODO(vamshi); one @Disabled spec test per class; "
         "task file in dependency order.<br/>Acceptance: build stays green.",
         "PR #3; partner's 8 commits"),
        ("now pull form main and fix the bugs if any",
         "Context: main + vamshi/stages-7-9, never integrated.<br/>Task: integrate, then hunt bugs at module boundaries.<br/>"
         "Constraints: separate worktree; probe colliding keys, quotes in preambles, @ at line start, real ePrint file.<br/>"
         "Acceptance: failing regression test first, then fix, then full suite.",
         "PR #4 (2 fixes)"),
      ]),
      Spacer(1, 4),
      P("<b>Template</b>"),
      P("Context: &lt;design-doc section / files / current state to read first&gt;<br/>Task: &lt;one module or one bug&gt;<br/>"
        "Constraints: &lt;language, package, interfaces that must not change, what not to touch&gt;<br/>"
        "Acceptance: &lt;tests that must pass, command, expected output&gt;<br/>Report: &lt;diff summary, test counts, open questions&gt;", mono)]

def footer(c, d):
    c.saveState(); c.setFont("Helvetica", 7.5); c.setFillColor(colors.HexColor("#6b7280"))
    c.drawString(1.7 * cm, 1.0 * cm, "BibMerge · Principles & Practices · CS5013 mid-evaluation")
    c.drawRightString(A4[0] - 1.7 * cm, 1.0 * cm, f"Page {d.page}"); c.restoreState()

doc = SimpleDocTemplate(OUT, pagesize=A4, leftMargin=1.7 * cm, rightMargin=1.7 * cm, topMargin=1.5 * cm, bottomMargin=1.5 * cm,
                        title="BibMerge — Principles & Practices", author="S Naveen, Gowni Vamshi")
doc.build(s, onFirstPage=footer, onLaterPages=footer)
from pypdf import PdfReader
print("pages:", len(PdfReader(OUT).pages))
