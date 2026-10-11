import { useState } from 'react'
import './App.css'

type PairView = { left: string; right: string; score: number; rule: string }
type MergeResponse = {
  bib: string
  records: number
  entries: number
  merges: PairView[]
  reviews: PairView[]
  diagnostics: string[]
}

function PairTable({ title, pairs, note }: { title: string; pairs: PairView[]; note: string }) {
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
              <th>Record</th>
              <th>Record</th>
              <th>Score</th>
              <th>Why</th>
            </tr>
          </thead>
          <tbody>
            {pairs.map((p) => (
              <tr key={p.left + p.right}>
                <td>{p.left}</td>
                <td>{p.right}</td>
                <td className="num">{p.score.toFixed(3)}</td>
                <td>
                  <code>{p.rule}</code>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}

function App() {
  const [files, setFiles] = useState<File[]>([])
  const [dialect, setDialect] = useState('biblatex')
  const [result, setResult] = useState<MergeResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function merge() {
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const form = new FormData()
      files.forEach((f) => form.append('files', f))
      form.append('dialect', dialect)
      const res = await fetch('/api/merge', { method: 'POST', body: form })
      if (!res.ok) {
        const body = await res.json().catch(() => null)
        throw new Error(body?.detail ?? body?.message ?? `HTTP ${res.status}`)
      }
      setResult((await res.json()) as MergeResponse)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
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
        Upload the <code>.bib</code> files of several papers. Duplicates are merged into one clean
        bibliography; old citation keys keep working as <code>ids</code> aliases.
      </p>

      <div className="controls">
        <input
          type="file"
          accept=".bib"
          multiple
          aria-label="BibTeX files"
          onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
        />
        <label>
          Output{' '}
          <select value={dialect} onChange={(e) => setDialect(e.target.value)}>
            <option value="biblatex">biblatex (keeps old keys as ids)</option>
            <option value="bibtex">classic BibTeX</option>
          </select>
        </label>
        <button onClick={merge} disabled={files.length === 0 || busy}>
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

          <PairTable
            title="Merged"
            pairs={result.merges}
            note="Judged to be the same work and combined into one entry."
          />
          <PairTable
            title="Needs review"
            pairs={result.reviews}
            note="Uncertain: kept as separate entries, never merged automatically."
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
