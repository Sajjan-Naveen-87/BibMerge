package io.bibmerge.api.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.bibmerge.core.match.Matcher;
import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;
import io.bibmerge.core.normalize.RecordNormalizer;
import io.bibmerge.core.parse.BibParser;
import io.bibmerge.core.pipeline.MergePipeline;
import io.bibmerge.core.pipeline.MergeResult;
import io.bibmerge.core.serialize.Dialect;

/**
 * Headless commands (design doc §3.4). Runs without starting Spring.
 *
 * <pre>
 *   match a.bib b.bib ...                     list duplicate decisions
 *   merge a.bib b.bib ... -o merged.bib       write one merged bibliography
 *         [--dialect bibtex|biblatex]
 * </pre>
 */
public final class Cli {

    public static final int OK = 0;
    public static final int IO_ERROR = 1;
    public static final int USAGE = 2;
    public static final int NOT_IMPLEMENTED = 3;

    private final PrintStream out;
    private final PrintStream err;

    public Cli(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static boolean handles(String[] args) {
        return args.length > 0 && (args[0].equals("match") || args[0].equals("merge"));
    }

    public int run(String[] args) {
        try {
            return switch (args[0]) {
                case "match" -> match(args);
                case "merge" -> merge(args);
                default -> usage("unknown command " + args[0]);
            };
        } catch (IOException e) {
            err.println("error: " + e.getMessage());
            return IO_ERROR;
        }
    }

    private int match(String[] args) throws IOException {
        List<Path> files = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            files.add(Path.of(args[i]));
        }
        if (files.isEmpty()) {
            return usage("match needs at least one .bib file");
        }
        List<ParseResult> inputs = parseAll(files);
        List<SourceRecord> records = inputs.stream().flatMap(p -> p.records().stream()).toList();
        Matcher.Result result = Matcher.withDefaults()
                .match(RecordNormalizer.normalizeAll(records));

        printDecisions("MERGE", result.merges());
        printDecisions("REVIEW", result.reviews());
        printDiagnostics(inputs);
        out.printf(Locale.ROOT, "%d records from %d files, %d pairs compared: %d merge, %d review%n",
                records.size(), files.size(), result.comparisons(), result.merges().size(),
                result.reviews().size());
        return OK;
    }

    private int merge(String[] args) throws IOException {
        List<Path> files = new ArrayList<>();
        Path output = null;
        Dialect dialect = Dialect.BIBLATEX;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-o", "--output" -> {
                    if (++i >= args.length) {
                        return usage("-o needs a file name");
                    }
                    output = Path.of(args[i]);
                }
                case "--dialect" -> {
                    if (++i >= args.length) {
                        return usage("--dialect needs bibtex or biblatex");
                    }
                    try {
                        dialect = Dialect.valueOf(args[i].toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        return usage("unknown dialect " + args[i]);
                    }
                }
                default -> files.add(Path.of(args[i]));
            }
        }
        if (files.isEmpty() || output == null) {
            return usage("merge needs .bib files and -o <output.bib>");
        }
        List<ParseResult> inputs = parseAll(files);

        MergeResult result;
        try {
            result = MergePipeline.withDefaults(dialect).run(inputs);
        } catch (UnsupportedOperationException e) {
            err.println("merge is not available yet: " + e.getMessage());
            err.println("use 'match' to see the duplicate decisions.");
            return NOT_IMPLEMENTED;
        }

        Files.writeString(output, result.bib(), StandardCharsets.UTF_8);
        printDecisions("REVIEW", result.reviews());
        printDiagnostics(inputs);
        out.printf(Locale.ROOT, "%d records -> %d entries (%d merges, %d left for review),"
                + " written to %s%n", result.records(), result.entries().size(),
                result.merges().size(), result.reviews().size(), output);
        return OK;
    }

    private List<ParseResult> parseAll(List<Path> files) throws IOException {
        BibParser parser = new BibParser();
        List<ParseResult> results = new ArrayList<>();
        for (Path file : files) {
            results.add(parser.parse(file));
        }
        return results;
    }

    private void printDecisions(String label, List<PairDecision> decisions) {
        for (PairDecision d : decisions) {
            out.printf(Locale.ROOT, "%-6s %.3f %-32s %s  <->  %s%n", label, d.score(), d.rule(),
                    d.pair().left().source().location(), d.pair().right().source().location());
        }
    }

    private void printDiagnostics(List<ParseResult> inputs) {
        for (ParseResult input : inputs) {
            for (ParseDiagnostic diagnostic : input.diagnostics()) {
                err.println(diagnostic);
            }
        }
    }

    private int usage(String problem) {
        err.println("error: " + problem);
        err.println("usage: bibmerge match <a.bib> <b.bib> ...");
        err.println("       bibmerge merge <a.bib> <b.bib> ... -o <merged.bib>"
                + " [--dialect bibtex|biblatex]");
        return USAGE;
    }
}
