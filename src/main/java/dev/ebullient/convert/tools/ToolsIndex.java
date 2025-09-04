package dev.ebullient.convert.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex;
import dev.ebullient.convert.tools.pf2e.Pf2eIndex;

public interface ToolsIndex {
    // Special one-offs for accounting/tracking
    enum TtrpgValue implements JsonNodeReader {
        indexBaseItem,
        indexFluffKey,
        indexInputType,
        indexKey,
        indexParentKey,
        indexVersionKeys,
        isHomebrew,
        homebrewSource,
        homebrewBaseSource,
    }

    static ToolsIndex createIndex() {
        CompendiumConfig config = TtrpgConfig.getConfig();
        return createIndex(config.datasource(), config);
    }

    static ToolsIndex createIndex(Datasource game, CompendiumConfig config) {
        if (Objects.requireNonNull(game) == Datasource.toolsPf2e) {
            return new Pf2eIndex(config);
        }
        return new Tools5eIndex(config);
    }

    CompendiumConfig cfg();

    default String rulesVaultRoot() {
        return cfg().rulesVaultRoot();
    }

    default String compendiumVaultRoot() {
        return cfg().compendiumVaultRoot();
    }

    default Path rulesFilePath() {
        return cfg().rulesFilePath();
    }

    default Path compendiumFilePath() {
        return cfg().compendiumFilePath();
    }

    /**
     * Get vault root for a specific content type, falling back to compendium root if not specified.
     *
     * @param typeName the content type name (e.g., "monsters", "spells", "items")
     * @return the vault root path for the content type
     */
    default String getTypeVaultRoot(String typeName) {
        return cfg().getTypeVaultRoot(typeName);
    }

    /**
     * Get file path for a specific content type, falling back to compendium path if not specified.
     *
     * @param typeName the content type name (e.g., "monsters", "spells", "items")
     * @return the file path for the content type
     */
    default Path getTypeFilePath(String typeName) {
        return cfg().getTypeFilePath(typeName);
    }

    /**
     * Check if a specific type has a custom path configured.
     *
     * @param typeName the content type name (e.g., "monsters", "spells", "items")
     * @return true if the type has a custom path, false if it uses the default compendium path
     */
    default boolean hasTypeSpecificPath(String typeName) {
        return cfg().hasTypeSpecificPath(typeName);
    }

    default boolean resolveSources(Path toolsPath) {
        // Check for a 'data' subdirectory
        Path data = toolsPath.resolve("data");
        if (data.toFile().isDirectory()) {
            toolsPath = data;
        }

        TtrpgConfig.setToolsPath(toolsPath);
        var allOk = true;
        for (String adventure : cfg().resolveAdventures()) {
            allOk &= cfg().readSource(toolsPath.resolve(adventure), TtrpgConfig.getFixes(adventure), this::importTree);
        }
        for (String book : cfg().resolveBooks()) {
            allOk &= cfg().readSource(toolsPath.resolve(book), TtrpgConfig.getFixes(book), this::importTree);
        }
        // Include additional standalone files from config (relative to current directory)
        for (String brew : cfg().resolveHomebrew()) {
            allOk &= cfg().readSource(Path.of(brew), TtrpgConfig.getFixes(brew), this::importTree);
        }
        return allOk;
    }

    void prepare();

    boolean notPrepared();

    ToolsIndex importTree(String filename, JsonNode node);

    MarkdownConverter markdownConverter(MarkdownWriter writer);

    void writeFullIndex(Path resolve) throws IOException;

    void writeFilteredIndex(Path resolve) throws IOException;

    JsonNode getBook(String b);

    JsonNode getAdventure(String a);
}
