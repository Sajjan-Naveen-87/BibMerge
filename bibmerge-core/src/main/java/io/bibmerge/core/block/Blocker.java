package io.bibmerge.core.block;

import java.util.List;

import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.NormalizedRecord;

/** Proposes the pairs worth comparing (design doc Stage 4). */
public interface Blocker {

    List<CandidatePair> candidates(List<NormalizedRecord> records);
}
