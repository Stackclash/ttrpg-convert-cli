package dev.ebullient.convert.tools.dnd5e;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SpellComponentParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testParseComponents() throws Exception {
        String componentsJson = """
                {
                  "v": true,
                  "s": true,
                  "m": "a tiny ball of bat guano and sulfur"
                }
                """;

        JsonNode components = mapper.readTree(componentsJson);

        // Test verbal component
        boolean hasVerbal = components.get("v") != null && components.get("v").asBoolean();
        assertTrue(hasVerbal);

        // Test somatic component
        boolean hasSomatic = components.get("s") != null && components.get("s").asBoolean();
        assertTrue(hasSomatic);

        // Test material component
        JsonNode materialNode = components.get("m");
        assertNotNull(materialNode);
        String material = materialNode.asText();
        assertEquals("a tiny ball of bat guano and sulfur", material);
    }

    @Test
    void testParseDamageTypes() throws Exception {
        String damageJson = """
                ["fire", "cold"]
                """;

        JsonNode damageInflict = mapper.readTree(damageJson);
        assertTrue(damageInflict.isArray());

        List<String> damageTypes = mapper.convertValue(damageInflict,
                mapper.getTypeFactory().constructCollectionType(List.class, String.class));

        assertEquals(List.of("fire", "cold"), damageTypes);
    }

    @Test
    void testParseSavingThrows() throws Exception {
        String savingThrowJson = """
                ["dexterity", "constitution"]
                """;

        JsonNode savingThrow = mapper.readTree(savingThrowJson);
        assertTrue(savingThrow.isArray());

        List<String> saves = mapper.convertValue(savingThrow,
                mapper.getTypeFactory().constructCollectionType(List.class, String.class));

        assertEquals(List.of("dexterity", "constitution"), saves);
    }

    @Test
    void testEmptyComponents() throws Exception {
        String emptyJson = "{}";
        JsonNode components = mapper.readTree(emptyJson);

        boolean hasVerbal = components.get("v") != null && components.get("v").asBoolean();
        assertFalse(hasVerbal);

        boolean hasSomatic = components.get("s") != null && components.get("s").asBoolean();
        assertFalse(hasSomatic);

        JsonNode materialNode = components.get("m");
        assertNull(materialNode);
    }
}