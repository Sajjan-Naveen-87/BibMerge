package io.bibmerge.core.match;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import io.bibmerge.core.model.AuthorList;
import io.bibmerge.core.model.Feature;
import io.bibmerge.core.model.FeatureVector;
import io.bibmerge.core.model.IdAgreement;
import io.bibmerge.core.model.Identifiers;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PersonName;

/**
 * Stage 5: every feature is a symmetric pure function of two records returning a
 * value in [0, 1], or ABSENT (left out of the vector) when either side lacks the
 * evidence.
 *
 * <p>{@link Feature#TITLE_IDF_OVERLAP} is not computed yet: it needs the frozen IDF
 * table (ADR-11). The scorer renormalizes over present features, so it is simply
 * absent until then.
 */
public final class FeatureExtractor {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

    public FeatureVector extract(NormalizedRecord a, NormalizedRecord b) {
        Map<Feature, Double> values = new EnumMap<>(Feature.class);

        if (!a.title().isEmpty() && !b.title().isEmpty()) {
            values.put(Feature.TITLE_JW, JARO_WINKLER.apply(a.title(), b.title()));
            values.put(Feature.TITLE_JACCARD, jaccard(shingles(a.title()), shingles(b.title())));
            values.put(Feature.TITLE_LEN_RATIO, (double) Math.min(a.title().length(),
                    b.title().length()) / Math.max(a.title().length(), b.title().length()));
        }
        if (!a.authors().isEmpty() && !b.authors().isEmpty()) {
            values.put(Feature.AUTHOR_SET_F1, authorSetScore(a.authors(), b.authors()));
            values.put(Feature.AUTHOR1_EXACT, firstAuthorScore(a.authors().first().orElseThrow(),
                    b.authors().first().orElseThrow()));
        }
        if (a.year() != null && b.year() != null) {
            int delta = Math.abs(a.year() - b.year());
            values.put(Feature.YEAR_DELTA, delta == 0 ? 1.0 : delta == 1 ? 0.5 : 0.0);
        }
        if (!a.venue().isEmpty() && !b.venue().isEmpty()) {
            values.put(Feature.VENUE_SIM, JARO_WINKLER.apply(a.venue(), b.venue()));
        }
        if (a.firstPage() != null && b.firstPage() != null) {
            values.put(Feature.PAGE_VOL_AGREE, a.firstPage().equals(b.firstPage()) ? 1.0 : 0.0);
        } else if (!a.volume().isEmpty() && !b.volume().isEmpty()) {
            values.put(Feature.PAGE_VOL_AGREE, a.volume().equals(b.volume()) ? 1.0 : 0.0);
        }
        values.put(Feature.TYPE_COMPAT, TypeCompatibility.score(a.entryType(), b.entryType()));

        return new FeatureVector(values, idAgreement(a.ids(), b.ids()));
    }

    static IdAgreement idAgreement(Identifiers a, Identifiers b) {
        boolean agree = same(a.doi(), b.doi()) || same(a.arxiv(), b.arxiv())
                || same(a.iacr(), b.iacr());
        if (agree) {
            return IdAgreement.AGREE;
        }
        boolean conflict = differ(a.doi(), b.doi()) || differ(a.arxiv(), b.arxiv())
                || differ(a.iacr(), b.iacr());
        return conflict ? IdAgreement.CONFLICT : IdAgreement.UNKNOWN;
    }

    private static boolean same(String x, String y) {
        return x != null && x.equals(y);
    }

    private static boolean differ(String x, String y) {
        return x != null && y != null && !x.equals(y);
    }

    /** Character 3-gram shingles, padded so short titles still produce some. */
    static Set<String> shingles(String text) {
        String padded = " " + text + " ";
        Set<String> out = new HashSet<>();
        for (int i = 0; i + 3 <= padded.length(); i++) {
            out.add(padded.substring(i, i + 3));
        }
        return out;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        int union = a.size() + b.size() - intersection.size();
        return (double) intersection.size() / union;
    }

    /**
     * F1 over family-name sets; containment |A∩B| / min(|A|,|B|) when either list is
     * truncated, so "Smith and others" is not penalised for the missing co-authors.
     */
    static double authorSetScore(AuthorList a, AuthorList b) {
        Set<String> x = families(a);
        Set<String> y = families(b);
        Set<String> common = new HashSet<>(x);
        common.retainAll(y);
        if (a.truncated() || b.truncated()) {
            return (double) common.size() / Math.min(x.size(), y.size());
        }
        return 2.0 * common.size() / (x.size() + y.size());
    }

    private static Set<String> families(AuthorList list) {
        return list.names().stream().map(PersonName::family).collect(Collectors.toSet());
    }

    /**
     * Graded first-author agreement: exact 1.0, same family and initial 0.9, one side
     * has no given name 0.6, conflicting initials 0.2. Given and family are also tried
     * swapped, for names whose order is ambiguous (Xiang Li vs Li Xiang).
     */
    static double firstAuthorScore(PersonName a, PersonName b) {
        double direct = graded(a.family(), a.given(), b.family(), b.given());
        double swapped = graded(a.family(), a.given(), b.given(), b.family());
        return Math.max(direct, swapped == 1.0 ? 0.9 : 0.0);
    }

    private static double graded(String familyA, String givenA, String familyB, String givenB) {
        if (familyA.isEmpty() || !familyA.equals(familyB)) {
            return 0.0;
        }
        if (givenA.isEmpty() || givenB.isEmpty()) {
            return 0.6;
        }
        if (givenA.equals(givenB)) {
            return 1.0;
        }
        return Objects.equals(givenA.substring(0, 1), givenB.substring(0, 1)) ? 0.9 : 0.2;
    }
}
