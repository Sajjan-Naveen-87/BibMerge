package io.bibmerge.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.bibmerge.api.cli.Cli;
import io.bibmerge.core.BibMergeCore;

@SpringBootApplication
public class BibMergeApplication {

    static final String USAGE = """
            BibMerge %s -- deduplicate and merge BibTeX references across papers.

            Usage:
              java -jar bibmerge.jar            start the web app on http://localhost:8080
              java -jar bibmerge.jar --help     show this message

              java -jar bibmerge.jar match <a.bib> <b.bib> ...
                                                list duplicate decisions
              java -jar bibmerge.jar merge <a.bib> <b.bib> ... -o <merged.bib>
                    [--dialect bibtex|biblatex] write one merged bibliography
            """.formatted(BibMergeCore.VERSION);

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        if (argList.contains("--help") || argList.contains("-h")) {
            System.out.print(USAGE);
            return;
        }
        if (Cli.handles(args)) {
            System.exit(new Cli(System.out, System.err).run(args));
        }
        SpringApplication.run(BibMergeApplication.class, args);
    }
}
