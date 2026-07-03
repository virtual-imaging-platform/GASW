package fr.insalyon.creatis.gasw.parser;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GaswParser URI handling tests")
class GaswParserTest {

    @ParameterizedTest
    @DisplayName("URI test with different number of slashes")
    @CsvSource({
            "girder:///control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69, control_3DT1.nii",
            "girder:/control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69, control_3DT1.nii",
            "girder://control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69, ''"
    })
    void uriPathExtraction(String value, String expected) throws URISyntaxException {
        URI uri = new URI(value);

        String path = uri.getPath();
        String result = (path == null) ? "" : new java.io.File(path).getName();

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("URI with no slash is not an uri")
    public void uriGetPathGetNameNoSlash() throws URISyntaxException {
        String value = "girder:control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69";

        URI valueURI = new URI(value);

        assertThrows(NullPointerException.class,
                     () -> new File(valueURI.getPath()).getName());
    }

    @Test
    @DisplayName("Extracting info from URI")
    public void extractInfoFromUri() throws URISyntaxException {
        String s = "lfn://localhost:8080/un/chemin/fichier.txt?arg1=1&arg2=deux";
        URI uri = new URI(s);

        assertEquals("/un/chemin/fichier.txt", uri.getPath());
        assertEquals("/un/chemin/fichier.txt", uri.getRawPath());
        assertEquals("lfn", uri.getScheme());
        assertEquals("//localhost:8080/un/chemin/fichier.txt?arg1=1&arg2=deux",
                     uri.getSchemeSpecificPart());
        assertEquals(s, uri.toString());
    }
}
