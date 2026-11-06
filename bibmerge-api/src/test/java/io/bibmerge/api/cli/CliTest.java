package io.bibmerge.api.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final Cli cli = new Cli(new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));

    @TempDir
    Path dir;

    private Path write(String name, String bib) throws Exception {
        return Files.writeString(dir.resolve(name), bib);
    }

    @Test
    void matchReportsDuplicatesAcrossFiles() throws Exception {
        Path a = write("a.bib", "@article{dh76, title = {New Directions in Cryptography},"
                + " author = {Whitfield Diffie and Martin Hellman}, year = 1976,"
                + " doi = {10.1109/TIT.1976.1055638}}");
        Path b = write("b.bib", "@article{DiffieH76, title = {{NEW DIRECTIONS IN CRYPTOGRAPHY}},"
                + " author = {Diffie, W. and Hellman, M.}, year = 1976,"
                + " doi = {https://doi.org/10.1109/tit.1976.1055638}}");

        int code = cli.run(new String[] {"match", a.toString(), b.toString()});

        String text = out.toString(StandardCharsets.UTF_8);
        assertEquals(Cli.OK, code);
        assertTrue(text.contains("MERGE"), text);
        assertTrue(text.contains("a.bib:1 dh76"), text);
        assertTrue(text.contains("1 merge, 0 review"), text);
    }

    @Test
    void parseProblemsGoToStderr() throws Exception {
        Path a = write("a.bib", "@article{ok, title = {T}}\n@article{bad, title = {open\n");

        cli.run(new String[] {"match", a.toString()});

        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Skipped malformed"));
    }

    @Test
    void missingFileIsAnIoError() {
        assertEquals(Cli.IO_ERROR, cli.run(new String[] {"match", dir.resolve("nope.bib").toString()}));
    }

    @Test
    void missingFileErrorSaysWhatIsWrong() {
        cli.run(new String[] {"merge", dir.resolve("nope.bib").toString(), "-o",
                dir.resolve("out.bib").toString()});

        String text = err.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("file not found") && text.contains("nope.bib"), text);
    }

    @Test
    void mergeReportsAKeyThatNamesDifferentWorks() throws Exception {
        Path a = write("a.bib", "@article{smith20, author = {John Smith},"
                + " title = {Graph Colouring Made Easy}, year = 2020}");
        Path b = write("b.bib", "@article{smith20, author = {Jane Smith},"
                + " title = {Quantum Error Correction Codes}, year = 2020}");

        int code = cli.run(new String[] {"merge", a.toString(), b.toString(), "-o",
                dir.resolve("out.bib").toString()});

        assertEquals(Cli.OK, code);
        String text = err.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("'smith20' names different works"), text);
    }

    @Test
    void matchReadsReferencesFromAPdf() throws Exception {
        Path bib = write("a.bib", "@article{dh76, title = {New Directions in Cryptography},"
                + " author = {Whitfield Diffie and Martin Hellman}, year = 1976,"
                + " doi = {10.1109/TIT.1976.1055638}}");
        Path pdf = Files.write(dir.resolve("paper.pdf"), io.bibmerge.api.TestPdfs.pdf(java.util.List.of(
                "References",
                "[1] W. Diffie and M. Hellman, \"New directions in cryptography,\" 1976.",
                "    doi:10.1109/TIT.1976.1055638")));

        int code = cli.run(new String[] {"match", bib.toString(), pdf.toString()});

        String text = out.toString(StandardCharsets.UTF_8);
        assertEquals(Cli.OK, code);
        assertTrue(text.contains("paper.pdf:0 paper-ref1") && text.contains("1 merge"), text);
    }

    @Test
    void mergeWithoutOutputIsUsageError() throws Exception {
        Path a = write("a.bib", "@article{k, title = {T}}");

        assertEquals(Cli.USAGE, cli.run(new String[] {"merge", a.toString()}));
    }

    @Test
    void handlesOnlyKnownCommands() {
        assertTrue(Cli.handles(new String[] {"match"}));
        assertTrue(!Cli.handles(new String[] {"--server.port=8091"}));
        assertTrue(!Cli.handles(new String[] {}));
    }
}
