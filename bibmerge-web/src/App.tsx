import { useEffect, useState } from 'react'
import './App.css'

type Health = { status: string; version: string }

function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json() as Promise<Health>
      })
      .then(setHealth)
      .catch((err: Error) => setError(err.message))
  }, [])

  return (
    <main>
      <h1>BibMerge</h1>
      <p>Deduplicate and merge BibTeX references across papers.</p>
      <p className="status">
        Backend:{' '}
        {health
          ? `${health.status} (v${health.version})`
          : error
            ? `unreachable (${error})`
            : 'checking…'}
      </p>
    </main>
  )
}

export default App
