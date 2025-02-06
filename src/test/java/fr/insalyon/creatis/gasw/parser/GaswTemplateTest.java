package fr.insalyon.creatis.gasw.parser;

import fr.insalyon.creatis.gasw.GaswException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
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
            GaswParser.templateParts(template, inputs, null);

        // Then
        assertEquals(4, result.size());
        assertEquals(GaswOutputTemplateType.DIR_AND_NAME,
                     result.get(0).getType());
        assertEquals("input1", result.get(0).getValue());

        assertEquals(GaswOutputTemplateType.STRING, result.get(1).getType());
        assertEquals("/", result.get(1).getValue());

        assertEquals(GaswOutputTemplateType.NAME, result.get(2).getType());
        assertEquals("input2", result.get(2).getValue());

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
            GaswParser.templateParts(template, inputs, null);

        // Then
        assertEquals(6, result.size());

        assertEquals(GaswOutputTemplateType.PREFIX, result.get(0).getType());
        assertEquals("input1", result.get(0).getValue());

        assertEquals(GaswOutputTemplateType.DIR_AND_NAME,
                result.get(1).getType());
        assertEquals("input1", result.get(1).getValue());

        assertEquals(GaswOutputTemplateType.STRING, result.get(2).getType());
        assertEquals("/", result.get(2).getValue());

        assertEquals(GaswOutputTemplateType.NAME, result.get(3).getType());
        assertEquals("input2", result.get(3).getValue());

        assertEquals(GaswOutputTemplateType.STRING, result.get(4).getType());
        assertEquals(".tar.gz", result.get(4).getValue());

        assertEquals(GaswOutputTemplateType.OPTIONS, result.get(5).getType());
        assertEquals("input1", result.get(5).getValue());
    }

    @Test
    @DisplayName("simple template")
    public void simpleTemplate() throws SAXException {
        // Given
        String template = "$dir1/$na1/$na2.tar.gz";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs, null);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "/a/b/c");
        inputsMap.put("input2", "/d/e/f.g");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("/a/b/c/f.g.tar.gz", output);
    }

    @Test
    @DisplayName("full template")
    public void fullTemplate() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs, null);
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
            GaswParser.templateParts(template, inputs, null);
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
            GaswParser.templateParts(template, inputs, null);
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
            GaswParser.templateParts(template, inputs, null);
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
            GaswParser.templateParts(template, inputs, null);
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
            GaswParser.templateParts(template, inputs, null);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "girder:/?opt=val");
        inputsMap.put("input2", "/d/e/f");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("girder:/f.tar.gz?opt=val", output);
    }

    @Test
    @DisplayName("full template, file uri one slash")
    public void fullTemplateFileUriOneSlash() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs, null);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "file:/a/b/c");
        inputsMap.put("input2", "/d/e/f.txt");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("file:/a/b/c/f.txt.tar.gz", output);
    }

    @Test
    @DisplayName("full template, file uri three slashes")
    public void fullTemplateFileUriThreeSlashes() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.tar.gz$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs, null);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "file:///a/b/c");
        inputsMap.put("input2", "/d/e/f.txt");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);

        // Then
        assertEquals("file:/a/b/c/f.txt.tar.gz", output);
    }
    
    @Test
    @DisplayName("full template, file uri three slashes with Strip extension as nii")
    public void fullTemplateFileUriWithStripExtn() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2_brain.out$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        Set<String> stripExtensions= new HashSet<>();
        stripExtensions.add(".nii");
        stripExtensions.add(".nii.gz");
        
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs, stripExtensions);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "file:///a/b/c");
        inputsMap.put("input2", "/d/e/f.nii");
        
        
        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);
        // Then
        assertEquals("file:/a/b/c/f_brain.out", output);
    }
    
    @Test
    @DisplayName("full template, file uri three slashes with Strip extension as nii.gx")
    public void fullTemplateFileUriWithStripExtn2() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2_brain.out$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        Set<String> stripExtensions= new HashSet<>();
        stripExtensions.add(".nii");
        stripExtensions.add(".nii.gz");
        
        List<GaswOutputTemplatePart> templateParts =
            GaswParser.templateParts(template, inputs, stripExtensions);
        Map<String, String> inputsMap = new HashMap<>();
        inputsMap.put("input1", "file:///a/b/c");
        inputsMap.put("input2", "/d/e/f.nii.gz");
        
        
        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);
        // Then
        assertEquals("file:/a/b/c/f_brain.out", output);
    }

    @Test
    @DisplayName("full template, Girder URI with strip extension")
    public void fullTemplateGirderUriWithStripExtn() throws SAXException {
        // Given
        String template = "$prefix1$dir1/$na1/$na2.out$options1";
        List<String> inputs = Arrays.asList("input1", "input2");
        Set<String> stripExtensions= new HashSet<>();
        stripExtensions.add(".nii");

        List<GaswOutputTemplatePart> templateParts =
                GaswParser.templateParts(template, inputs, stripExtensions);
        Map<String, String> inputsMap = new HashMap<>();
        // actual Girder query strings are of the form ?apiurl=https://host/main/api/v1&fileId=...&token=...
        inputsMap.put("input1", "girder:/outputdir/results?fileId=5678");
        inputsMap.put("input2", "girder:/inputdir/file.nii?fileId=1234");

        // When
        String output = GaswParser.parseOutputTemplate(templateParts, inputsMap);
        // Then
        assertEquals("girder:/outputdir/results/file.out?fileId=5678", output);
    }
}
