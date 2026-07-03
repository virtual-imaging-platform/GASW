package fr.insalyon.creatis.gasw;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GaswUtil tests")
public class GaswUtilTest {

    @Nested
    @DisplayName("isUri()")
    class IsUri {

        @ParameterizedTest(name = "\"{0}\" is a valid URI")
        @DisplayName("recognizes valid URIs with various schemes")
        @ValueSource(strings = {
            "girder:///control_3DT1.nii",
            "shanoir:/control_3DT1.nii",
            "http://example.com/file.txt",
            "https://example.com/file.txt",
            "file:///path/to/file"
        })
        void recognizesValidUris(String uri) {
            assertTrue(GaswUtil.isUri(uri));
        }

        @ParameterizedTest(name = "\"{0}\" is not a valid URI")
        @DisplayName("rejects invalid URIs")
        @ValueSource(strings = {
            "girder:control_3DT1.nii",
            "girder:////control_3DT1.nii",
            "/local/path/file.txt",
            "file.txt",
            "",
            "://path"
        })
        void rejectsInvalidUris(String uri) {
            assertFalse(GaswUtil.isUri(uri));
        }
    }

    @Test
    @DisplayName("extracts base name from files with extensions")
    void extractsBaseName() {
        assertEquals("file", GaswUtil.getBaseName("file.txt"));
        assertEquals("archive.tar", GaswUtil.getBaseName("archive.tar.gz"));
        assertEquals("file", GaswUtil.getBaseName("file"));
        assertEquals("", GaswUtil.getBaseName(".hidden"));
        assertEquals(".hidden", GaswUtil.getBaseName(".hidden.txt"));
    }
}