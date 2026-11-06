package io.bibmerge.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MergeControllerTest {

    @Autowired
    private MockMvc mvc;

    private static MockMultipartFile bib(String name, String content) {
        return new MockMultipartFile("files", name, "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void mergesDuplicatesAcrossUploadedFiles() throws Exception {
        mvc.perform(multipart("/api/merge")
                .file(bib("a.bib", "@article{dh76, title = {New Directions in Cryptography},"
                        + " author = {Whitfield Diffie and Martin Hellman}, year = 1976,"
                        + " doi = {10.1109/TIT.1976.1055638}}"))
                .file(bib("b.bib", "@article{DiffieH76, title = {{NEW DIRECTIONS IN CRYPTOGRAPHY}},"
                        + " author = {Diffie, W. and Hellman, M.}, year = 1976,"
                        + " doi = {https://doi.org/10.1109/tit.1976.1055638}}\n"
                        + "@article{broken, title = {never closed\n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").value(2))
                .andExpect(jsonPath("$.entries").value(1))
                .andExpect(jsonPath("$.merges[0].rule").value("identifier-agree"))
                .andExpect(jsonPath("$.bib", Matchers.containsString("ids = {")))
                .andExpect(jsonPath("$.diagnostics[0]", Matchers.containsString("b.bib")));
    }

    @Test
    void emptyUploadIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/merge").file(bib("empty.bib", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongFileComesBackWithAWarning() throws Exception {
        mvc.perform(multipart("/api/merge").file(bib("notes.bib", "Remember to cite DH76.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").value(0))
                .andExpect(jsonPath("$.diagnostics[0]",
                        Matchers.containsString("No BibTeX entries found")));
    }

    private static final String SAME_DOI_A = "@article{dh76, title = {New Directions in Cryptography},"
            + " author = {Whitfield Diffie and Martin Hellman}, year = 1976,"
            + " doi = {10.1109/TIT.1976.1055638}}";
    private static final String SAME_DOI_B = "@article{DiffieH76, title = {{NEW DIRECTIONS IN CRYPTOGRAPHY}},"
            + " author = {Diffie, W. and Hellman, M.}, year = 1976,"
            + " doi = {https://doi.org/10.1109/tit.1976.1055638}}";

    private static String hashOf(String bib) {
        return new io.bibmerge.core.parse.BibParser().parse("x.bib", bib).records().get(0)
                .contentHash();
    }

    @Test
    void pairsCarryTheEvidenceForTheReviewer() throws Exception {
        mvc.perform(multipart("/api/merge").file(bib("a.bib", SAME_DOI_A)).file(bib("b.bib", SAME_DOI_B)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merges[0].a.title").value("New Directions in Cryptography"))
                .andExpect(jsonPath("$.merges[0].b.hash").value(hashOf(SAME_DOI_B)))
                .andExpect(jsonPath("$.merges[0].features.TITLE_JW").value(1.0))
                .andExpect(jsonPath("$.merges[0].idAgree").value("AGREE"));
    }

    @Test
    void keepSeparateDecisionSplitsTheMerge() throws Exception {
        String decisions = "[{\"a\":\"" + hashOf(SAME_DOI_A) + "\",\"b\":\"" + hashOf(SAME_DOI_B)
                + "\",\"choice\":\"keep-separate\"}]";

        mvc.perform(multipart("/api/merge").file(bib("a.bib", SAME_DOI_A)).file(bib("b.bib", SAME_DOI_B))
                .param("decisions", decisions))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").value(2))
                .andExpect(jsonPath("$.merges").isEmpty());
    }

    @Test
    void badDecisionsAreBadRequest() throws Exception {
        mvc.perform(multipart("/api/merge").file(bib("a.bib", SAME_DOI_A)).param("decisions", "not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/merge").file(bib("a.bib", SAME_DOI_A))
                .param("decisions", "[{\"a\":\"x\",\"b\":\"y\",\"choice\":\"maybe\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pdfUploadIsReadAndMerged() throws Exception {
        byte[] pdf = io.bibmerge.api.TestPdfs.pdf(java.util.List.of("References",
                "[1] W. Diffie and M. Hellman, \"New directions in cryptography,\" 1976.",
                "    doi:10.1109/TIT.1976.1055638"));

        mvc.perform(multipart("/api/merge").file(bib("a.bib", SAME_DOI_A))
                .file(new MockMultipartFile("files", "paper.pdf", "application/pdf", pdf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").value(1))
                .andExpect(jsonPath("$.merges[0].rule").value("identifier-agree"))
                .andExpect(jsonPath("$.diagnostics[0]", Matchers.containsString("Read 1 references")));
    }

    @Test
    void unknownDialectIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/merge").file(bib("a.bib", "@misc{k, title = {T}}"))
                .param("dialect", "latex2"))
                .andExpect(status().isBadRequest());
    }
}
