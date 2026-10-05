# Synthetic corpus

Hand-written fixtures exercising the duplicate classes in design doc §10.2.
Entries for well-known papers are written from memory to look like real exports;
the "Widget" and "Lattice Sieving" papers are fictitious. Not authoritative
bibliographic data. The real stakeholder corpus lives in `corpus.local/` and is
never committed (ADR-14).

| Case | paper-a.bib | paper-b.bib | Expected |
|---|---|---|---|
| Same DOI, different key and formatting | `dh76` | `DiffieH76` | MERGE (identifier) |
| No DOI on one side, DBLP crossref on the other | `BR93` | `DBLP:conf/ccs/BellareR93` | MERGE (deterministic key) |
| ePrint preprint vs proceedings version | `cryptoeprint:2020/123` | `ExampleS21` | MERGE (score), DISTINCT under KEEP_DISTINCT |
| arXiv preprint (truncated authors) vs NeurIPS | `vaswani2017attention` | `NIPS2017_attention` | MERGE (identifier) |
| Same authors, same year, different paper | `BR93`, `BR93entity` | | DISTINCT |
| Part I vs Part II | `Sieve-I` | `Sieve-II` | DISTINCT |
| Proceedings container vs its own child | | `DBLP:conf/ccs/1993` | never compared |
| Malformed entry | | `broken` | skipped with a diagnostic |
