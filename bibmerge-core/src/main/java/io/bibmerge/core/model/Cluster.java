package io.bibmerge.core.model;

import java.util.List;

/**
 * Records judged to be one work (design doc Stage 7). A record with no MERGE edge
 * is a cluster of one.
 *
 * @param members records in input order
 */
public record Cluster(List<NormalizedRecord> members) {

    public Cluster {
        if (members.isEmpty()) {
            throw new IllegalArgumentException("a cluster has at least one member");
        }
        members = List.copyOf(members);
    }
}
