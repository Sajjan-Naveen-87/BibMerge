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
    void unknownDialectIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/merge").file(bib("a.bib", "@misc{k, title = {T}}"))
                .param("dialect", "latex2"))
                .andExpect(status().isBadRequest());
    }
}
