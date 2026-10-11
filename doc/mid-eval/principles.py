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
        "Five principles, each learned from a prompt that failed first:"),
      *B(["<b>Context before task.</b> Point the model at the design-doc section, the existing interfaces and the current repository state "
          "before asking for code (P1, P2). A handoff document said the design doc existed; the repository audit showed it had never been pushed.",
          "<b>One unit of work per prompt.</b> One pipeline stage or one bug per prompt, one commit per answer. Large prompts produced code we could not review.",
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
          "PRs merge only on green, with merge commits so each member's history stays visible (<font face='Courier'>git shortlog</font>: 18 / 8).",
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
             ["Documentation", "Design doc (architecture, ADRs, test plan) in repo; Javadoc on every public type stating <i>why</i>; README with build/run; task specs; synthetic-corpus README; PR descriptions list deviations and known gaps."]],
            [3.6 * cm, W - 3.6 * cm]),
      P("<b>Known gaps</b> (stated, not hidden): blocking is all-pairs (fine for a few papers, not for 10k records); venue abbreviation dictionary and review UI "
        "not built; BibTeX/biber compile check not yet automated in CI.")]

s += [P("Appendix — sample prompts (full log: doc/prompts/README.md)", h2),
      P("<b>A. Implement one stage under design constraints</b> → PR #2"),
      P("Implement my modules from design doc §6.6 (stages 1–6) on a branch, one commit per stage. Use the package layout of §6.2 and the "
        "domain model of §5. Every stage needs unit tests before it is called done. Run the full build after each commit and show me the test "
        "count. Do not touch Vamshi's modules (stages 7–9).", mono),
      P("<b>B. Spec-first hand-off</b> → PR #3, then the partner's 8 commits"),
      P("Do not write Vamshi's modules. Write interfaces that throw TODO(vamshi), one @Disabled test per class that states the exact expected "
        "behaviour, and a task file listing the classes in dependency order. The build must stay green.", mono),
      P("<b>C. Verify against real data</b> → commit be07587"),
      P("Get the stakeholder's real papers (public metadata only, stored in the git-ignored corpus.local/), run `match` on them, and explain "
        "every MERGE or REVIEW decision.", mono),
      P("<b>D. Structured bug hunt</b> → commits d6b9b9b, 929678a (PR #4)"),
      P("Pull main. Build the integrated branch in a separate worktree. Probe: keys colliding across entries, quotes in preambles, a line "
        "starting with @ inside a field, the real ePrint file. For each failure: reproduce, write a failing regression test, fix, rerun the suite.", mono),
      P("<b>E. Template</b>"),
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
