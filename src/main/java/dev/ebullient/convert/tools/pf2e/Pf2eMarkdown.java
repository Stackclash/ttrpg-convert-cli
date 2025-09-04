package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public class Pf2eMarkdown implements MarkdownConverter {
    final Pf2eIndex index;
    final MarkdownWriter writer;

    public Pf2eMarkdown(Pf2eIndex index, MarkdownWriter writer) {
        this.index = index;
        this.writer = writer;
    }

    @Override
    public Pf2eMarkdown writeAll() {
        return writeFiles(Stream.of(Pf2eIndexType.values())
                .collect(Collectors.toList()));
    }

    @Override
    public Pf2eMarkdown writeImages() {
        index.tui().progressf("Writing images and fonts");
        index.tui().copyImages(Pf2eSources.getImages());
        return this;
    }

    @Override
    public Pf2eMarkdown writeFiles(IndexType type) {
        return writeFiles(List.of(type));
    }

    static class WritingQueue {
        List<Pf2eQuteBase> baseCompendium = new ArrayList<>();
        List<Pf2eQuteBase> baseRules = new ArrayList<>();
        List<QuteNote> noteCompendium = new ArrayList<>();
        List<QuteNote> noteRules = new ArrayList<>();

        // Some state for combining notes
        Map<Pf2eIndexType, Json2QuteBase> combinedDocs = new HashMap<>();
    }

    @Override
    public Pf2eMarkdown writeFiles(List<? extends IndexType> types) {
        if (types == null || types.isEmpty()) {
            return this;
        }
        index.tui().progressf("Converting data: %s", types);
        WritingQueue queue = new WritingQueue();
        for (var entry : index.filteredEntries()) {
            final String key = entry.getKey();
            final JsonNode jsonSource = entry.getValue();

            final Pf2eIndexType nodeType = Pf2eIndexType.getTypeFromKey(key);
            if (!types.contains(nodeType)) {
                continue;
            }

            if (nodeType.isOutputType() && !nodeType.useQuteNote()) {
                writePf2eQuteBase(nodeType, key, jsonSource, queue);
            } else if (nodeType.isOutputType() && nodeType.useQuteNote()) {
                writeNotesAndTables(nodeType, key, jsonSource, queue);
            }
        }

        // Group files by their target base paths and write them
        writeFilesByTargetPath(queue.baseCompendium, queue.baseRules);

        for (Json2QuteBase value : queue.combinedDocs.values()) {
            append(value.type, value.buildNote(), queue.noteCompendium, queue.noteRules);
        }

        // Custom indices
        append(Pf2eIndexType.trait, Json2QuteTrait.buildIndex(index), queue.noteCompendium, queue.noteRules);

        // Group notes by their target base paths and write them
        writeNotesByTargetPath(queue.noteCompendium, queue.noteRules);

        // TODO: DOES THIS WORK RIGHT? shouldn't these be in the other image map?
        // List<ImageRef> images = rules.stream()
        //         .flatMap(s -> s.images().stream()).collect(Collectors.toList());
        // index.tui().copyImages(images, fallbackPaths);

        return this;
    }

    /**
     * Write files grouped by their target base paths to support per-type path configuration.
     */
    private void writeFilesByTargetPath(List<Pf2eQuteBase> compendiumFiles, List<Pf2eQuteBase> rulesFiles) {
        // Group all files by their target base path
        Map<Path, List<Pf2eQuteBase>> filesByPath = new HashMap<>();

        // Add compendium files, determining their actual target path
        for (Pf2eQuteBase file : compendiumFiles) {
            Path targetBasePath = getTargetBasePath(file);
            filesByPath.computeIfAbsent(targetBasePath, k -> new ArrayList<>()).add(file);
        }

        // Add rules files (they always go to rules path)
        if (!rulesFiles.isEmpty()) {
            filesByPath.computeIfAbsent(index.rulesFilePath(), k -> new ArrayList<>()).addAll(rulesFiles);
        }

        // Write each group of files to their target path
        for (Map.Entry<Path, List<Pf2eQuteBase>> entry : filesByPath.entrySet()) {
            writer.writeFiles(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Write notes grouped by their target base paths to support per-type path configuration.
     */
    private void writeNotesByTargetPath(List<QuteNote> compendiumNotes, List<QuteNote> rulesNotes) {
        // Group all notes by their target base path
        Map<Path, List<QuteNote>> notesByPath = new HashMap<>();

        // Add compendium notes, determining their actual target path
        for (QuteNote note : compendiumNotes) {
            Path targetBasePath = getTargetBasePath(note);
            notesByPath.computeIfAbsent(targetBasePath, k -> new ArrayList<>()).add(note);
        }

        // Add rules notes (they always go to rules path)
        if (!rulesNotes.isEmpty()) {
            notesByPath.computeIfAbsent(index.rulesFilePath(), k -> new ArrayList<>()).addAll(rulesNotes);
        }

        // Write each group of notes to their target path
        for (Map.Entry<Path, List<QuteNote>> entry : notesByPath.entrySet()) {
            writer.writeNotes(entry.getKey(), entry.getValue(),
                    entry.getKey().equals(index.compendiumFilePath()) || !entry.getKey().equals(index.rulesFilePath()));
        }
    }

    /**
     * Determine the target base path for a Pf2eQuteBase item, considering per-type path configuration.
     */
    private Path getTargetBasePath(Pf2eQuteBase item) {
        if (item.sources() instanceof Pf2eSources) {
            Pf2eSources sources = (Pf2eSources) item.sources();
            Pf2eIndexType type = sources.getType();
            if (type != null && type.useCompendiumBase()) {
                String configTypeName = type.getConfigTypeName();
                if (configTypeName != null) {
                    return index.getTypeFilePath(configTypeName);
                }
            }
        }
        // Default fallback: use compendium path for compendium items
        return index.compendiumFilePath();
    }

    /**
     * Determine the target base path for a QuteNote item, considering per-type path configuration.
     */
    private Path getTargetBasePath(QuteNote note) {
        // For notes, we need to determine the type from the note itself
        // This is more complex since notes don't always have direct source references
        // For now, default to compendium path - this could be enhanced if needed
        return index.compendiumFilePath();
    }

    private void writePf2eQuteBase(Pf2eIndexType type, String key, JsonNode node, WritingQueue queue) {
        var compendium = queue.baseCompendium;
        var rules = queue.baseRules;

        // Moved to index type -- also used by embedded rendering
        Pf2eQuteBase converted = type.convertJson2QuteBase(index, node);
        if (converted != null) {
            append(type, converted, compendium, rules);
        }
    }

    private Pf2eMarkdown writeNotesAndTables(Pf2eIndexType type, String key, JsonNode node, WritingQueue queue) {
        var compendium = queue.noteCompendium;
        var rules = queue.noteRules;
        var combinedDocs = queue.combinedDocs;

        switch (type) {
            case ability -> rules.add(new Json2QuteAbility(index, type, node).buildNote());
            case affliction, curse, disease ->
                compendium.add(new Json2QuteAffliction(index, type, node).buildNote());
            case book -> {
                index.tui().progressf("book %s", key);
                JsonNode data = index.getIncludedNode(key.replace("book|", "data|"));
                if (data == null) {
                    index.tui().errorf("No data for %s", key);
                } else {
                    List<Pf2eQuteNote> pages = new Json2QuteBook(index, type, node, data).buildBook();
                    rules.addAll(pages);
                }
            }
            case condition -> {
                Json2QuteCompose conditions = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                        t -> new Json2QuteCompose(type, index, "Conditions"));
                conditions.add(node);
            }
            case domain -> {
                Json2QuteCompose domains = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                        t -> new Json2QuteCompose(type, index, "Domains"));
                domains.add(node);
            }
            case skill -> {
                Json2QuteCompose skills = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                        t -> new Json2QuteCompose(type, index, "Skills"));
                skills.add(node);
            }
            case table -> {
                Pf2eQuteNote table = new Json2QuteTable(index, node).buildNote();
                rules.add(table);
            }
            default -> {
            }
        }

        return this;
    }

    <T> void append(Pf2eIndexType type, T note, List<T> compendium, List<T> rules) {
        if (note != null) {
            if (type.useCompendiumBase()) {
                compendium.add(note);
            } else {
                rules.add(note);
            }
        }
    }
}
