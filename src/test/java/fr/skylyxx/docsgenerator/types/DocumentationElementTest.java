package fr.skylyxx.docsgenerator.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentationElementTest {

    @Test
    void setPatternsRemovesSkriptParseMarks() {
        DocumentationElement element = new DocumentationElement();
        element.setPatterns(new String[]{"1¦player has permission", "2¦hello"});

        assertEquals("player has permission", element.getPatterns()[0]);
        assertEquals("hello", element.getPatterns()[1]);
    }
}
