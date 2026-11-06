package io.bibmerge.api.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bibmerge.core.match.Constraints;
import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;
import io.bibmerge.core.normalize.Text;
import io.bibmerge.core.parse.InputParser;
import io.bibmerge.core.pipeline.MergePipeline;
import io.bibmerge.core.pipeline.MergeResult;
import io.bibmerge.core.serialize.Dialect;

/** Upload {@code .bib} files, get the merged bibliography and the decisions behind it. */
@RestController
class MergeController {

    /** One record of a pair, as the reviewer sees it. */
    record RecordView(String location, String hash, String title, String authors, String year) {

        static RecordView of(SourceRecord r) {
            return new RecordView(r.location(), r.contentHash(),
                    Text.decode(r.field("title").orElse("")),
                    Text.decode(r.field("author").or(() -> r.field("editor")).orElse("")),
                    r.field("year").orElse(""));
        }
    }

    /**
     * One pair decision as the UI shows it, with the evidence behind it (NFR5).
     * {@code left} and {@code right} stay as plain locations for older clients.
     */
    record PairView(String left, String right, double score, String rule, RecordView a,
            RecordView b, Map<String, Double> features, String idAgree) {

        static PairView of(PairDecision d) {
            Map<String, Double> features = new LinkedHashMap<>();
            d.features().values().forEach((f, v) -> features.put(f.name(), v));
            SourceRecord a = d.pair().left().source();
            SourceRecord b = d.pair().right().source();
            return new PairView(a.location(), b.location(), d.score(), d.rule(),
                    RecordView.of(a), RecordView.of(b), features, d.features().idAgree().name());
        }
    }

    /** A reviewer's answer about two records, by content hash. */
    record HumanDecision(String a, String b, String choice) {
    }

    record MergeResponse(String bib, int records, int entries, List<PairView> merges,
            List<PairView> reviews, List<String> diagnostics) {
    }

    private final ObjectMapper json;

    MergeController(ObjectMapper json) {
        this.json = json;
    }

    /**
     * @param decisions optional JSON array of {@code {"a": hash, "b": hash, "choice":
     *                  "merge" | "keep-separate"}}, applied in order
     */
    @PostMapping(path = "/api/merge", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MergeResponse merge(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "dialect", defaultValue = "biblatex") String dialect,
            @RequestParam(name = "decisions", defaultValue = "") String decisions)
            throws IOException {
        if (files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "upload at least one .bib file");
        }
        Dialect target;
        try {
            target = Dialect.valueOf(dialect.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown dialect " + dialect);
        }
        Constraints constraints = constraints(decisions);

        List<ParseResult> inputs = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename() == null ? "upload.bib" : file.getOriginalFilename();
            inputs.add(InputParser.parse(name, file.getBytes()));
        }

        MergeResult result = MergePipeline.withDefaults(target).run(inputs, constraints);
        return new MergeResponse(result.bib(), result.records(), result.entries().size(),
                result.merges().stream().map(PairView::of).toList(),
                result.reviews().stream().map(PairView::of).toList(),
                result.diagnostics().stream().map(Object::toString).toList());
    }

    private Constraints constraints(String decisions) {
        if (decisions.isBlank()) {
            return Constraints.NONE;
        }
        List<HumanDecision> list;
        try {
            list = json.readValue(decisions, new TypeReference<List<HumanDecision>>() { });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decisions is not valid JSON");
        }
        Constraints constraints = Constraints.NONE;
        for (HumanDecision d : list) {
            if (d.a() == null || d.b() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a decision needs a and b");
            }
            constraints = switch (String.valueOf(d.choice())) {
                case "merge" -> constraints.merge(d.a(), d.b());
                case "keep-separate" -> constraints.keepSeparate(d.a(), d.b());
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "choice must be merge or keep-separate, got " + d.choice());
            };
        }
        return constraints;
    }
}
