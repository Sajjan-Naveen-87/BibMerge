import { Fragment, useState } from 'react'
import './App.css'

type RecordView = { location: string; hash: string; title: string; authors: string; year: string }
type PairView = {
  left: string
  right: string
  score: number
  rule: string
  a: RecordView
  b: RecordView
  features: Record<string, number>
  idAgree: string
}
type MergeResponse = {
  bib: string
  records: number
  entries: number
  merges: PairView[]
  reviews: PairView[]
  diagnostics: string[]
}
type Choice = 'merge' | 'keep-separate'
/** A reviewer's answer. The server only needs a, b and choice; the rest is for display. */
type HumanDecision = { a: string; b: string; choice: Choice; aLabel: string; bLabel: string }

/** Plain-language reasons for the rule names the engine records on each decision. */
const RULES: Record<string, string> = {
  'identifier-agree': 'same DOI, arXiv or ePrint id',
  'identifier-agree-title-mismatch': 'same id, but the titles differ',
  'deterministic-key': 'same title, first author and year',
  'score>=tauHigh': 'very similar title, authors, year and venue',
  'tauLow<=score<tauHigh': 'similar, but not enough to be sure',
  'thin-evidence': 'too little information to be sure',
  'human:merge': 'you said these are the same paper',
  'cap:year-gap': 'years are 3 or more apart',
  'cap:conference-vs-journal': 'conference paper vs journal version',
  'cut:keep-separate': 'only joined through a pair you kept separate',
}

const FEATURES: [string, string][] = [
  ['TITLE_JW', 'Title wording'],
  ['TITLE_JACCARD', 'Title letters'],
  ['TITLE_LEN_RATIO', 'Title length'],
  ['AUTHOR_SET_F1', 'Authors'],
  ['AUTHOR1_EXACT', 'First author'],
  ['YEAR_DELTA', 'Year'],
  ['VENUE_SIM', 'Venue'],
  ['PAGE_VOL_AGREE', 'Pages / volume'],
  ['TYPE_COMPAT', 'Entry type'],
]

function explain(rule: string): string {
  return rule
    .split('+')
    .map((part) => RULES[part] ?? part)
    .join('; ')
}

const samePair = (d: HumanDecision, a: string, b: string) =>
  (d.a === a && d.b === b) || (d.a === b && d.b === a)

function Paper({ r }: { r: RecordView }) {
  return (
    <div className="paper">
      <div className="title">{r.title || <em>(no title)</em>}</div>
      <div className="meta">
        {r.authors || 'no authors'}
        {r.year && ` · ${r.year}`}
      </div>
      <div className="where">{r.location}</div>
    </div>
  )
}

function Evidence({ p }: { p: PairView }) {
  return (
    <div className="evidence">
      <div className="id">
        Identifiers: <strong>{p.idAgree.toLowerCase()}</strong>
      </div>
      {FEATURES.map(([key, label]) => {
        const v = p.features[key]
        return (
          <div className="bar-row" key={key}>
            <span>{label}</span>
            {v === undefined ? (
              <span className="absent">no information</span>
            ) : (
              <span className="track">
                <span className="fill" style={{ width: `${v * 100}%` }} />
              </span>
            )}
            <span className="num">{v === undefined ? '' : v.toFixed(2)}</span>
          </div>
        )
      })}
    </div>
  )
}

function PairTable({
  title,
  pairs,
  note,
  actions,
  onDecide,
  busy,
}: {
  title: string
  pairs: PairView[]
  note: string
  actions: { choice: Choice; label: string }[]
  onDecide: (p: PairView, choice: Choice) => void
  busy: boolean
}) {
  const [open, setOpen] = useState<string | null>(null)
  return (
    <section>
      <h2>
        {title} <span className="count">{pairs.length}</span>
      </h2>
      <p className="note">{note}</p>
      {pairs.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Entry</th>
              <th>Entry</th>
              <th className="num">Score</th>
              <th>Why</th>
              <th>Your call</th>
            </tr>
          </thead>
          <tbody>
            {pairs.map((p) => {
              const id = p.a.hash + p.b.hash + p.left + p.right
              return (
                <Fragment key={id}>
                  <tr>
                    <td>
                      <Paper r={p.a} />
                    </td>
                    <td>
                      <Paper r={p.b} />
                    </td>
                    <td className="num">{p.score.toFixed(3)}</td>
                    <td>
                      {explain(p.rule)}
                      <button className="link" onClick={() => setOpen(open === id ? null : id)}>
                        {open === id ? 'Hide evidence' : 'Show evidence'}
                      </button>
                    </td>
                    <td className="actions">
                      {actions.map((a) => (
                        <button key={a.choice} disabled={busy} onClick={() => onDecide(p, a.choice)}>
                          {a.label}
                        </button>
                      ))}
                    </td>
                  </tr>
                  {open === id && (
                    <tr className="evidence-row">
                      <td colSpan={5}>
                        <Evidence p={p} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      )}
    </section>
  )
}

function App() {
  const [files, setFiles] = useState<File[]>([])
  const [dialect, setDialect] = useState('biblatex')
  const [decisions, setDecisions] = useState<HumanDecision[]>([])
  const [result, setResult] = useState<MergeResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function merge(withDecisions: HumanDecision[]) {
    setBusy(true)
    setError(null)
    try {
      const form = new FormData()
      files.forEach((f) => form.append('files', f))
      form.append('dialect', dialect)
      form.append(
        'decisions',
        JSON.stringify(withDecisions.map(({ a, b, choice }) => ({ a, b, choice }))),
      )
      const res = await fetch('/api/merge', { method: 'POST', body: form })
      if (!res.ok) {
        const body = await res.json().catch(() => null)
        throw new Error(body?.detail ?? body?.message ?? `HTTP ${res.status}`)
      }
      setResult((await res.json()) as MergeResponse)
      setDecisions(withDecisions)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  function decide(p: PairView, choice: Choice) {
    const next = decisions.filter((d) => !samePair(d, p.a.hash, p.b.hash))
    next.push({ a: p.a.hash, b: p.b.hash, choice, aLabel: p.a.location, bLabel: p.b.location })
    merge(next)
  }

  function download() {
    if (!result) return
    const url = URL.createObjectURL(new Blob([result.bib], { type: 'text/x-bibtex' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'merged.bib'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <main>
      <h1>BibMerge</h1>
      <p className="lede">
        Upload the <code>.bib</code> files of several papers, or their PDFs. Duplicates are merged into one clean
        bibliography; old citation keys keep working as <code>ids</code> aliases.
      </p>
      <p className="note">
        Adding a new paper later? Upload your earlier <code>merged.bib</code> together with the new
        paper&apos;s <code>.bib</code>. Existing entries keep their keys.
      </p>
      <p className="note">
        Only have a PDF? Upload it: BibMerge reads its reference list. Text read from a PDF is
        approximate, so a PDF reference merges only on a shared DOI, arXiv or ePrint id or an exact
        match, and otherwise waits for you under <em>Needs review</em>.
      </p>

      <div className="controls">
        <input
          type="file"
          accept=".bib,.pdf"
          multiple
          aria-label="BibTeX or PDF files"
          onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
        />
        <label>
          Output{' '}
          <select value={dialect} onChange={(e) => setDialect(e.target.value)}>
            <option value="biblatex">biblatex (keeps old keys as ids)</option>
            <option value="bibtex">classic BibTeX</option>
          </select>
        </label>
        <button onClick={() => merge(decisions)} disabled={files.length === 0 || busy}>
          {busy ? 'Merging…' : `Merge ${files.length || ''} file${files.length === 1 ? '' : 's'}`}
        </button>
      </div>

      {error && <p className="error">Merge failed: {error}</p>}

      {result && (
        <>
          <div className="summary">
            <div>
              <strong>{result.records}</strong> records in
            </div>
            <div>
              <strong>{result.entries}</strong> entries out
            </div>
            <div>
              <strong>{result.merges.length}</strong> merges
            </div>
            <div>
              <strong>{result.reviews.length}</strong> for review
            </div>
            <button onClick={download}>Download merged.bib</button>
          </div>

          {decisions.length > 0 && (
            <section>
              <h2>
                Your decisions <span className="count">{decisions.length}</span>
              </h2>
              <p className="note">
                Applied to every merge from now on. Undo one to let BibMerge decide again.
              </p>
              <ul className="decisions">
                {decisions.map((d) => (
                  <li key={d.a + d.b}>
                    <span className={d.choice === 'merge' ? 'tag merge' : 'tag separate'}>
                      {d.choice === 'merge' ? 'Same paper' : 'Different papers'}
                    </span>
                    <code>{d.aLabel}</code> and <code>{d.bLabel}</code>
                    <button
                      className="link"
                      disabled={busy}
                      onClick={() => merge(decisions.filter((x) => x !== d))}
                    >
                      Undo
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <PairTable
            title="Needs review"
            pairs={result.reviews}
            note="Uncertain: kept as separate entries until you decide."
            actions={[
              { choice: 'merge', label: 'Same paper' },
              { choice: 'keep-separate', label: 'Different' },
            ]}
            onDecide={decide}
            busy={busy}
          />
          <PairTable
            title="Merged"
            pairs={result.merges}
            note="Judged to be the same work and combined into one entry."
            actions={[{ choice: 'keep-separate', label: 'Not the same' }]}
            onDecide={decide}
            busy={busy}
          />
          {result.diagnostics.length > 0 && (
            <section>
              <h2>
                Warnings <span className="count">{result.diagnostics.length}</span>
              </h2>
              <ul className="diagnostics">
                {result.diagnostics.map((d) => (
                  <li key={d}>
                    <code>{d}</code>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <section>
            <h2>merged.bib</h2>
            <pre>{result.bib}</pre>
          </section>
        </>
      )}
    </main>
  )
}

export default App
