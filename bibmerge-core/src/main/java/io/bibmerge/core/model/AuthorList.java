package io.bibmerge.core.model;

import java.util.List;
import java.util.Optional;

/**
 * Parsed authors of one record.
 *
 * @param names     authors in order, never containing the {@code others} marker
 * @param truncated true when the list ended in {@code and others} or {@code et al.}
 */
public record AuthorList(List<PersonName> names, boolean truncated) {

    public static final AuthorList EMPTY = new AuthorList(List.of(), false);

    public AuthorList {
        names = List.copyOf(names);
    }

    public boolean isEmpty() {
        return names.isEmpty();
    }

    public Optional<PersonName> first() {
        return names.isEmpty() ? Optional.empty() : Optional.of(names.get(0));
    }
}
