package dev.ebullient.convert.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;

public class PerTypePathsTest {

    private Tui tui;

    @BeforeEach
    void setUp() {
        tui = new Tui();
    }

    @Test
    void testBackwardCompatibility() throws IOException {
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

        // Verify backward compatibility
        assertEquals("/compendium/", config.compendiumVaultRoot());
        assertEquals("/rules/", config.rulesVaultRoot());
        assertEquals(Path.of("compendium/"), config.compendiumFilePath());
        assertEquals(Path.of("rules/"), config.rulesFilePath());

        // Verify per-type paths fall back to compendium
        assertEquals("/compendium/", config.getTypeVaultRoot("monsters"));
        assertEquals("/compendium/", config.getTypeVaultRoot("spells"));
        assertEquals(Path.of("compendium/"), config.getTypeFilePath("monsters"));
        assertEquals(Path.of("compendium/"), config.getTypeFilePath("spells"));
    }

    @Test
    void testPerTypePaths() throws IOException {
        String jsonConfig = """
                {
                    "sources": {
                        "book": ["PHB"]
                    },
                    "paths": {
                        "compendium": "/compendium/",
                        "rules": "/rules/",
                        "types": {
                            "monsters": "/bestiary/",
                            "spells": "/magic/spells/"
                        }
                    }
                }
                """;

        JsonNode node = Tui.MAPPER.readTree(jsonConfig);
        TtrpgConfig.init(tui, Datasource.tools5e);
        CompendiumConfig config = TtrpgConfig.getConfig();
        CompendiumConfig.Configurator configurator = new CompendiumConfig.Configurator(tui);

        configurator.readConfigIfPresent(node);

        // Verify base paths work
        assertEquals("/compendium/", config.compendiumVaultRoot());
        assertEquals("/rules/", config.rulesVaultRoot());

        // Verify per-type paths are used when specified
        assertEquals("/bestiary/", config.getTypeVaultRoot("monsters"));
        assertEquals("/magic/spells/", config.getTypeVaultRoot("spells"));
        assertEquals(Path.of("bestiary/"), config.getTypeFilePath("monsters"));
        assertEquals(Path.of("magic/spells/"), config.getTypeFilePath("spells"));

        // Verify fallback for unspecified types
        assertEquals("/compendium/", config.getTypeVaultRoot("items"));
        assertEquals(Path.of("compendium/"), config.getTypeFilePath("items"));

        // Test subclasses and subraces specifically
        assertEquals("/compendium/", config.getTypeVaultRoot("subclasses"));
        assertEquals("/compendium/", config.getTypeVaultRoot("subraces"));
        assertEquals(Path.of("compendium/"), config.getTypeFilePath("subclasses"));
        assertEquals(Path.of("compendium/"), config.getTypeFilePath("subraces"));
    }

    @Test
    void testMixedConfiguration() throws IOException {
        String jsonConfig = """
                {
                    "sources": {
                        "book": ["PHB"]
                    },
                    "paths": {
                        "compendium": "/compendium/",
                        "types": {
                            "monsters": "/creatures/"
                        }
                    }
                }
                """;

        JsonNode node = Tui.MAPPER.readTree(jsonConfig);
        TtrpgConfig.init(tui, Datasource.tools5e);
        CompendiumConfig config = TtrpgConfig.getConfig();
        CompendiumConfig.Configurator configurator = new CompendiumConfig.Configurator(tui);

        configurator.readConfigIfPresent(node);

        // Verify compendium fallback when rules not specified
        assertEquals("rules/", config.rulesVaultRoot()); // Default value
        assertEquals("/compendium/", config.compendiumVaultRoot());

        // Verify per-type path works
        assertEquals("/creatures/", config.getTypeVaultRoot("monsters"));
        assertEquals(Path.of("creatures/"), config.getTypeFilePath("monsters"));

        // Verify fallback for other types
        assertEquals("/compendium/", config.getTypeVaultRoot("spells"));
        assertEquals(Path.of("compendium/"), config.getTypeFilePath("spells"));
    }
}