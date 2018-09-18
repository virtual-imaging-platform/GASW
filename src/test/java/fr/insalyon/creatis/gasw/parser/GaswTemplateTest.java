package fr.insalyon.creatis.gasw.parser;

import fr.insalyon.creatis.gasw.GaswException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GaswParser template tests")
class GaswTemplateTest {

    @Test
    @DisplayName("simple template extraction")
    public void simpleTemplateExtraction() throws SAXException {
        // Given
        String template = "$dir1/$na1/$na2.tar.gz";
        List<String> inputs = Arrays.asList("input1", "input2");

        // When
        List<GaswOutputTemplatePart> result =
            GaswParser.templateParts(template, inputs);

        // Then
        assertEquals(4, result.size());
        assertEquals(GaswOutputTemplateType.DIR_AND_NAME,
                     result.get(0).getType());
        assertEquals("input1", result.get(0).getValue());
        assertEquals(GaswOutputTemplateType.STRING, result.get(1).getType());
        assertEquals(".tar.gz", result.get(3).getValue());
    }

    @Test
    @DisplayName("full template extraction")
    public void fullTemplateExtraction() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");

        // When
        List<GaswOutputTemplatePart> result =
            GaswParser.templateParts(template, inputs);

        // Then
        assertEquals(6, result.size());
    }

    @Test
    @DisplayName("simple template")
    public void simpleTemplate() throws SAXException {
        // Given
        String template = "$dir1/$na1/$na2.tar.gz";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "/a/b/c");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("/a/b/c/f.tar.gz", output);
    }

    @Test
    @DisplayName("full template")
    public void fullTemplate() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "girder://host/a/b/c?opt=val");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("girder://host/a/b/c/f.tar.gz?opt=val", output);
    }

    @Test
    @DisplayName("full template, no host")
    public void fullTemplateNoPrefixOptions() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "/a/b/c");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("/a/b/c/f.tar.gz", output);
    }

    @Test
    @DisplayName("full template, no host")
    public void fullTemplateNoHost() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "girder:/a/b/c?opt=val");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("girder:/a/b/c/f.tar.gz?opt=val", output);
    }

    @Test
    @DisplayName("full template, no scheme")
    public void fullTemplateNoScheme() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "//host/a/b/c?opt=val");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("//host/a/b/c/f.tar.gz?opt=val", output);
    }

    @Test
    @DisplayName("full template, empty path, with name")
    public void fullTemplateEmptyPath() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/output.txt$options1";
        List<String> inputs = Arrays.asList("input1");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "girder:/Private?apiurl=http://brain");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("girder:/Private/output.txt?apiurl=http://brain", output);
    }

    @Test
    @DisplayName("full template, no host, no path")
    public void fullTemplateNoHostNoPath() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "girder:/?opt=val");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("girder:/f.tar.gz?opt=val", output);
    }
}
