package dev.ebullient.convert.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;

/**
 * Test to verify that the hasTypeSpecificPath functionality works correctly
 * for preventing double-nested folder structures.
 */
public class PerTypePathsBehaviorTest {

    private Tui tui;

    @BeforeEach
    void setUp() {
        tui = new Tui();
    }

    @Test
    void testHasTypeSpecificPathWithPerTypePaths() throws IOException {
        String jsonConfig = """
                {
                    "sources": {
                        "book": ["PHB"]
                    },
                    "paths": {
                        "compendium": "/compendium",
                        "rules": "/compendium/rules",
                        "types": {
                            "monsters": "/bestiary",
                            "spells": "/magic/spells",
                            "items": "/equipment",
                            "races": "/character/ancestries",
                            "classes": "/character/classes"
                        }
                    }
                }
                """;

        JsonNode node = Tui.MAPPER.readTree(jsonConfig);
        TtrpgConfig.init(tui, Datasource.tools5e);
        CompendiumConfig config = TtrpgConfig.getConfig();
        CompendiumConfig.Configurator configurator = new CompendiumConfig.Configurator(tui);

        configurator.readConfigIfPresent(node);

        // Test that types with specific paths report hasTypeSpecificPath as true
        assertEquals(true, config.hasTypeSpecificPath("monsters"));
        assertEquals(true, config.hasTypeSpecificPath("spells"));
        assertEquals(true, config.hasTypeSpecificPath("items"));
        assertEquals(true, config.hasTypeSpecificPath("races"));
        assertEquals(true, config.hasTypeSpecificPath("classes"));

        // Test that types without specific paths report hasTypeSpecificPath as false
        assertEquals(false, config.hasTypeSpecificPath("backgrounds"));
        assertEquals(false, config.hasTypeSpecificPath("feats"));
        assertEquals(false, config.hasTypeSpecificPath("nonexistent"));
    }

    @Test
    void testHasTypeSpecificPathWithoutPerTypePaths() throws IOException {
        String jsonConfig = """
                {
                    "sources": {
                        "book": ["PHB"]
                    },
                    "paths": {
                        "compendium": "/compendium/",
                        "rules": "/rules/"
                    }
                }
                """;

        JsonNode node = Tui.MAPPER.readTree(jsonConfig);
        TtrpgConfig.init(tui, Datasource.tools5e);
        CompendiumConfig config = TtrpgConfig.getConfig();
        CompendiumConfig.Configurator configurator = new CompendiumConfig.Configurator(tui);

        configurator.readConfigIfPresent(node);

        // Verify that no types report as having specific paths
        assertEquals(false, config.hasTypeSpecificPath("monsters"));
        assertEquals(false, config.hasTypeSpecificPath("spells"));
        assertEquals(false, config.hasTypeSpecificPath("items"));
        assertEquals(false, config.hasTypeSpecificPath("backgrounds"));
        assertEquals(false, config.hasTypeSpecificPath("feats"));
    }
}
