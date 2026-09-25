package io.bibmerge.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.bibmerge.core.BibMergeCore;

@SpringBootApplication
public class BibMergeApplication {

    static final String USAGE = """
            BibMerge %s -- deduplicate and merge BibTeX references across papers.

            Usage:
              java -jar bibmerge.jar            start the web app on http://localhost:8080
              java -jar bibmerge.jar --help     show this message

            Planned (design doc §3.4):
              java -jar bibmerge.jar merge <a.bib> <b.bib> ... -o <merged.bib>
            """.formatted(BibMergeCore.VERSION);

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        if (argList.contains("--help") || argList.contains("-h")) {
            System.out.print(USAGE);
            return;
        }
        SpringApplication.run(BibMergeApplication.class, args);
    }
}
