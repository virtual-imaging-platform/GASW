package fr.insalyon.creatis.gasw.parser;

import fr.insalyon.creatis.gasw.GaswException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GaswParser URI handling tests")
class GaswParserTest {

    @Test
    @DisplayName("URI with 3 slashes")
    public void uriGetPathGetNameTripleSlash() throws URISyntaxException {
        String value = "girder:///control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69";

        URI valueURI = new URI(value);
        String res = new File(valueURI.getPath()).getName();

        assertEquals("control_3DT1.nii", res);
    }

    @Test
    @DisplayName("URI with 1 slash")
    public void uriGetPathGetNameSingleSlash() throws URISyntaxException {
        String value = "girder:/control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69";

        URI valueURI = new URI(value);
        String res = new File(valueURI.getPath()).getName();

        assertEquals("control_3DT1.nii", res);
    }

    @Test
    @DisplayName("URI with 2 slashes has no file")
    public void uriGetPathGetNameDouleSlash() throws URISyntaxException {
        String value = "girder://control_3DT1.nii?apiurl=http://localhost:8080/api/v1&fileId=5ae1a8fc371210092e0d2936&token=TFT2FdxP9hzM7WKsidBjMJMmN69";

        URI valueURI = new URI(value);
        String res = new File(valueURI.getPath()).getName();

        assertEquals("", res);
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
    @DisplayName("Pattern to detect URI")
    public void patternDetectingUri() {
        Pattern uriPattern = GaswParser.uriPattern;

        assertTrue(uriPattern.matcher("girder:///control_3DT1.nii").find());
        assertTrue(uriPattern.matcher("girder:/control_3DT1.nii").find());

        assertFalse(uriPattern.matcher("girder://control_3DT1.nii").find());
        assertFalse(uriPattern.matcher("girder:control_3DT1.nii").find());
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
