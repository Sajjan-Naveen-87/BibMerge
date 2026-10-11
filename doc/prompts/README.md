# Prompt log

Prompts that produced code, tests or decisions in this repository, kept under
version control next to the code they produced. Each entry names the commit or
PR it led to, so `git log` and this file together answer "which prompt made this
change".

**Original** is what we actually typed, verbatim. **Refined** is the same request
rewritten with the template at the end of this file: the extra lines are the
context, constraints and checks we had to supply later in the conversation.

The template we converged on is at the end. Task specs written for a teammate
(`doc/tasks/*.md`) are prompts too: they are what an assistant or a person is
given to implement a module.

## P1 — Load context before planning (25 Sep 2026)

Original: "[link to handoff doc] see this this contains what earlier i have done - see the next plans what i need to do"

Refined:

> Here is the handoff document with what we have done so far [link]. Read it, compare
> it against the local repository, and tell me what the next steps are.

Led to: repo audit (no code existed; design doc never pushed), PR #1 scaffold.
Why it worked: the assistant compared the plan with the actual repository state
instead of trusting the document.

## P2 — Implement one stage, constrained by the design doc (5 Oct 2026)

Original: "now make the changes in the code base to do some of the left behind commits and reschedule the uncomiplited commits"

Refined:

> Implement my modules from design doc §6.6 (stages 1–6) on a branch, one commit per
> stage. Use the package layout of §6.2 and the domain model of §5. Every stage needs
> unit tests before it is called done. Run the full build after each commit and show
> me the test count. Do not touch Vamshi's modules (stages 7–9).

Led to: PR #2 (model, parse, inheritance, identifiers, normalizers, matcher).

## P3 — Spec-first hand-off to a teammate (6 Oct 2026)

Original: "do thw wors need to be done today" (then, choosing among options the assistant offered: "Write a starter for him")

Refined:

> Do not write Vamshi's modules. Write interfaces that throw TODO(vamshi), one
> @Disabled test per class that states the exact expected behaviour, and a task file
> listing the classes in dependency order. The build must stay green.

Led to: PR #3 and `doc/tasks/vamshi-stages-7-9.md`; Vamshi's 8 commits implement it.

## P4 — Test against real data, not only fixtures (6 Oct 2026)

Original: choosing "Yes, download from arXiv" when the assistant asked how to get real data

Refined:

> Get the stakeholder's real papers (public metadata only, stored in the git-ignored
> corpus.local/), run `match` on them, and explain every MERGE or REVIEW decision.

Led to: commit be07587 — a real IACR ePrint record exposed a veto bypass.

## P5 — Structured bug hunt (11 Oct 2026)

Original: "now pull form main and fix the bugs if any"

Refined:

> Pull main. Build the integrated branch in a separate worktree. Probe these edge
> cases through the CLI: keys colliding across entries, quotes in preambles, a line
> starting with @ inside a field, the real ePrint file. For each failure: reproduce,
> write a failing regression test, fix, rerun the full suite.

Led to: commits d6b9b9b and 929678a, PR #4.

## Template

```
Context:     <file / design-doc section / current state the model must read first>
Task:        <one module or one bug — never several>
Constraints: <language, package, interfaces that must not change, what not to touch>
Acceptance:  <tests that must pass, command to run, expected output>
Report:      <what to show back: diff summary, test counts, open questions>
```
