package dev.ebullient.convert.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfiguratorTest {
    protected static Tui tui;

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);
    }

    @Test
    public void testPath() throws Exception {
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("paths.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.compendiumVaultRoot()).isEqualTo("");
            assertThat(config.compendiumFilePath()).isEqualTo(CompendiumConfig.CWD);
            assertThat(config.rulesVaultRoot()).isEqualTo("rules/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("rules/"));
            assertThat(config.images.copyInternal()).isFalse();
        });
    }

    @Test
    public void testSources() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("5e/sources.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            config.resolveAdventures();
            config.resolveBooks();
            config.resolveHomebrew();

            assertThat(config).isNotNull();
            assertThat(config.allSources()).isFalse();
            assertThat(config.sourceIncluded("phb")).isTrue();
            assertThat(config.sourceIncluded("scag")).isFalse();
            assertThat(config.sourceIncluded("dmg")).isTrue();
            assertThat(config.sourceIncluded("xge")).isTrue();
            assertThat(config.sourceIncluded("tce")).isTrue();
            assertThat(config.sourceIncluded("wbtw")).isTrue();
        });
    }

    @Test
    public void testFromAll() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("sources-from-all.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.allSources()).isTrue();
            assertThat(config.sourceIncluded("scag")).isTrue();
        });
    }

    @Test
    public void testBooksAdventures() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("5e/sources-2014-book-adventure.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            Collection<String> books = config.resolveBooks();
            Collection<String> adventures = config.resolveAdventures();

            assertThat(config).isNotNull();
            assertThat(books).contains("book/book-phb.json");
            assertThat(adventures).contains("adventure/adventure-wbtw.json");

            assertThat(config.compendiumVaultRoot()).isEqualTo("/compend%20ium/");
            assertThat(config.compendiumFilePath()).isEqualTo(Path.of("compend ium/"));
            assertThat(config.rulesVaultRoot()).isEqualTo("/ru%20les/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("ru les/"));
        });
    }

    @Test
    public void testSourcesBadTemplates() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("sources-bad-template.json"), List.of(), (f, node) -> {
            assertThrows(IllegalArgumentException.class,
                    () -> test.readConfigIfPresent(node));

            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.getCustomTemplate("background")).isNull();
        });
    }

    @Test
    public void testSourcesNoImages() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.imageOptions()).isNotNull();
            assertThat(config.imageOptions().copyInternal()).isFalse();
        });
    }

    @Test
    public void testPerTypePaths() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("per-type-paths.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();

            // Test basic paths still work
            assertThat(config.compendiumVaultRoot()).isEqualTo("default-compendium/");
            assertThat(config.compendiumFilePath()).isEqualTo(Path.of("default-compendium/"));
            assertThat(config.rulesVaultRoot()).isEqualTo("rules/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("rules/"));

            // Test per-type paths
            assertThat(config.getTypeSpecificVaultPath("monsters")).isEqualTo("custom-monsters/");
            assertThat(config.getTypeSpecificFilePath("monsters")).isEqualTo(Path.of("custom-monsters/"));
            assertThat(config.getTypeSpecificVaultPath("spells")).isEqualTo("custom-spells/");
            assertThat(config.getTypeSpecificFilePath("spells")).isEqualTo(Path.of("custom-spells/"));
            assertThat(config.getTypeSpecificVaultPath("items")).isEqualTo("equipment/");
            assertThat(config.getTypeSpecificFilePath("items")).isEqualTo(Path.of("equipment/"));

            // Test non-configured types return null
            assertThat(config.getTypeSpecificVaultPath("backgrounds")).isNull();
            assertThat(config.getTypeSpecificFilePath("backgrounds")).isNull();
        });
    }

    @Test
    public void testPartialPerTypePaths() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        tui.readFile(TestUtils.TEST_RESOURCES.resolve("partial-per-type-paths.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();

            // Test fallback to defaults when no base paths are configured
            assertThat(config.compendiumVaultRoot()).isEqualTo("compendium/");
            assertThat(config.compendiumFilePath()).isEqualTo(Path.of("compendium/"));
            assertThat(config.rulesVaultRoot()).isEqualTo("rules/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("rules/"));

            // Test only specified per-type paths are set
            assertThat(config.getTypeSpecificVaultPath("monsters")).isEqualTo("bestiary-only/");
            assertThat(config.getTypeSpecificFilePath("monsters")).isEqualTo(Path.of("bestiary-only/"));
            assertThat(config.getTypeSpecificVaultPath("spells")).isEqualTo("magic/");
            assertThat(config.getTypeSpecificFilePath("spells")).isEqualTo(Path.of("magic/"));

            // Test non-configured types return null (will fall back to default compendium behavior)
            assertThat(config.getTypeSpecificVaultPath("items")).isNull();
            assertThat(config.getTypeSpecificFilePath("items")).isNull();
        });
    }

    @Test
    public void testBackwardCompatibility() throws Exception {
        TtrpgConfig.init(tui, Datasource.tools5e);
        Configurator test = new Configurator(tui);

        // Test existing paths.json still works as before
        tui.readFile(TestUtils.TEST_RESOURCES.resolve("paths.json"), List.of(), (f, node) -> {
            test.readConfigIfPresent(node);
            CompendiumConfig config = TtrpgConfig.getConfig();
            assertThat(config).isNotNull();

            // Verify existing behavior unchanged
            assertThat(config.compendiumVaultRoot()).isEqualTo("");
            assertThat(config.compendiumFilePath()).isEqualTo(CompendiumConfig.CWD);
            assertThat(config.rulesVaultRoot()).isEqualTo("rules/");
            assertThat(config.rulesFilePath()).isEqualTo(Path.of("rules/"));

            // Verify no per-type paths are set
            assertThat(config.getTypeSpecificVaultPath("monsters")).isNull();
            assertThat(config.getTypeSpecificVaultPath("spells")).isNull();
            assertThat(config.getTypeSpecificVaultPath("items")).isNull();
        });
    }
}
