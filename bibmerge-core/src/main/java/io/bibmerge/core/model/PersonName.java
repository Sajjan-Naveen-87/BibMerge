package io.bibmerge.core.model;

/**
 * One author in the four-part BibTeX grammar {@code {given, von, family, suffix}}.
 *
 * <p>Parts are canonical: LaTeX decoded, diacritics folded, lower-cased, punctuation
 * removed. Display forms come from the {@link SourceRecord}, never from here.
 *
 * @param given     given names, e.g. {@code "martin luther"}
 * @param von       particle, e.g. {@code "van der"}
 * @param family    family name, e.g. {@code "king"}
 * @param suffix    e.g. {@code "jr"}
 * @param corporate true for a brace-wrapped name such as {@code {The MathWorks}}
 */
public record PersonName(String given, String von, String family, String suffix,
        boolean corporate) {

    public static PersonName corporate(String name) {
        return new PersonName("", "", name, "", true);
    }

    /** First letter of the given name, or empty. */
    public String firstInitial() {
        return given.isEmpty() ? "" : given.substring(0, 1);
    }
}
