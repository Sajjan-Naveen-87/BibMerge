package io.bibmerge.core.golden;

import java.util.Set;

import io.bibmerge.core.model.NormalizedRecord;

/**
 * Mints {@code family + year + firstTitleWord} keys, e.g. {@code bellare1993random},
 * with a letter suffix ({@code a}, {@code b}, ...) on collision (design doc Stage 8).
 *
 * <p>TODO(vamshi): implement {@link #mint}.
 */
public final class KeyMinter {

    /**
     * @param representative record whose first author, year and title form the key
     * @param taken          keys already issued; the result must not be one of them
     */
    public String mint(NormalizedRecord representative, Set<String> taken) {
        throw new UnsupportedOperationException("TODO(vamshi): KeyMinter.mint");
    }
}
