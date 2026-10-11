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
