package fr.insalyon.creatis.gasw.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GaswParser bash escaping tests")
class GaswParserBashEscapingTest {

    @Test
    @DisplayName("Escaping of many different special chars")
    public void escapingDifferentChars() {
        // When
        String result1 = GaswParser.escapeSpecialBashCharacters("abc\\d$&;'`\"");
        String result2 = GaswParser.escapeSpecialBashCharacters(" \t\n");

        // Then
        assertEquals("abc\\\\d\\$\\&\\;\\'\\`\\\"", result1);
        assertEquals("\\ \\\t\\\n", result2);
    }
}
