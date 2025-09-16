package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Tools5eLinkifierTest {
    protected static Tui tui;

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);
    }

    @Test
    public void testCustomPathsInLinkifier() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);
        Tools5eLinkifier linkifier = Tools5eLinkifier.instance();
        linkifier.reset(); // Ensure fresh state

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("per-type-paths.json"), java.util.List.of(), (f, node) -> {
            test.readConfigIfPresent(node);

            // Test that custom paths are used for types with custom configuration
            assertThat(linkifier.getRelativePath(Tools5eIndexType.monster)).isEqualTo("custom-monsters");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.spell)).isEqualTo("custom-spells");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.item)).isEqualTo("equipment");

            // Test that types without custom paths fall back to defaults
            assertThat(linkifier.getRelativePath(Tools5eIndexType.background)).isEqualTo("backgrounds");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.feat)).isEqualTo("feats");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.race)).isEqualTo("races");
        });
    }

    @Test
    public void testSubraceSubclassCustomPaths() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);
        Tools5eLinkifier linkifier = Tools5eLinkifier.instance();
        linkifier.reset(); // Ensure fresh state

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("subrace-subclass-paths.json"), java.util.List.of(), (f, node) -> {
            test.readConfigIfPresent(node);

            // Test that separate paths are used for subraces and subclasses
            assertThat(linkifier.getRelativePath(Tools5eIndexType.race)).isEqualTo("character/races");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.subrace)).isEqualTo("character/subraces");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.classtype)).isEqualTo("character/classes");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.subclass)).isEqualTo("character/subclasses");
        });
    }

    @Test
    public void testSubraceSubclassFallbackPaths() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);
        Tools5eLinkifier linkifier = Tools5eLinkifier.instance();
        linkifier.reset(); // Ensure fresh state

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("fallback-paths.json"), java.util.List.of(), (f, node) -> {
            test.readConfigIfPresent(node);

            // Test that subraces and subclasses fall back to parent type paths when not specifically configured
            assertThat(linkifier.getRelativePath(Tools5eIndexType.race)).isEqualTo("character/races");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.subrace)).isEqualTo("character/races");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.classtype)).isEqualTo("character/classes");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.subclass)).isEqualTo("character/classes");
        });
    }

    @Test
    public void testDefaultPathsWhenNoCustomConfig() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);
        Tools5eLinkifier linkifier = Tools5eLinkifier.instance();
        linkifier.reset(); // Ensure fresh state

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("paths.json"), java.util.List.of(), (f, node) -> {
            test.readConfigIfPresent(node);

            // Test that all paths use defaults when no custom per-type paths are configured
            assertThat(linkifier.getRelativePath(Tools5eIndexType.monster)).isEqualTo("bestiary");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.spell)).isEqualTo("spells");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.item)).isEqualTo("items");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.background)).isEqualTo("backgrounds");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.feat)).isEqualTo("feats");
            assertThat(linkifier.getRelativePath(Tools5eIndexType.race)).isEqualTo("races");
        });
    }
}