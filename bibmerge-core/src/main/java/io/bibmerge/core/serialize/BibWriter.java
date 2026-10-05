package io.bibmerge.core.serialize;

import java.util.List;

import io.bibmerge.core.model.CanonicalEntry;

/**
 * Writes canonical entries as a {@code .bib} file.
 *
 * <p>TODO(vamshi): implement {@link #write}. Requirements:
 * <ul>
 *   <li>preambles first, each as {@code @preamble{"..."}};</li>
 *   <li>entries sorted by key; fields in a fixed order (author, title, then
 *       alphabetical), each {@code  name = {value},} on its own line;</li>
 *   <li>values written verbatim inside braces: they are raw BibTeX already, so brace
 *       protection such as {@code {RSA}} must survive unchanged;</li>
 *   <li>under {@link Dialect#BIBLATEX}, a non-empty retired-key list becomes
 *       {@code ids = {k1,k2}};</li>
 *   <li>round trip: parsing the output with BibParser gives back the same fields.</li>
 * </ul>
 */
public final class BibWriter {

    public String write(List<CanonicalEntry> entries, List<String> preambles, Dialect dialect) {
        throw new UnsupportedOperationException("TODO(vamshi): BibWriter.write");
    }
}
