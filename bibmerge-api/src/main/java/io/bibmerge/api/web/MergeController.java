package io.bibmerge.api.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.parse.BibParser;
import io.bibmerge.core.pipeline.MergePipeline;
import io.bibmerge.core.pipeline.MergeResult;
import io.bibmerge.core.serialize.Dialect;

/** Upload {@code .bib} files, get the merged bibliography and the decisions behind it. */
@RestController
class MergeController {

    /** One pair decision as the UI shows it. */
    record PairView(String left, String right, double score, String rule) {

        static PairView of(PairDecision d) {
            return new PairView(d.pair().left().source().location(),
                    d.pair().right().source().location(), d.score(), d.rule());
        }
    }

    record MergeResponse(String bib, int records, int entries, List<PairView> merges,
            List<PairView> reviews, List<String> diagnostics) {
    }

    @PostMapping(path = "/api/merge", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MergeResponse merge(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "dialect", defaultValue = "biblatex") String dialect)
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

        BibParser parser = new BibParser();
        List<ParseResult> inputs = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename() == null ? "upload.bib" : file.getOriginalFilename();
            inputs.add(parser.parse(name, file.getBytes()));
        }

        MergeResult result = MergePipeline.withDefaults(target).run(inputs);
        return new MergeResponse(result.bib(), result.records(), result.entries().size(),
                result.merges().stream().map(PairView::of).toList(),
                result.reviews().stream().map(PairView::of).toList(),
                result.diagnostics().stream().map(Object::toString).toList());
    }
}
