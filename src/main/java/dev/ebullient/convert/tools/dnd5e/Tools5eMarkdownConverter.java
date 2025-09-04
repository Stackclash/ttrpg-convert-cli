package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Tools5eMarkdownConverter implements MarkdownConverter {
    final Tools5eIndex index;
    final MarkdownWriter writer;

    public Tools5eMarkdownConverter(Tools5eIndex index, MarkdownWriter writer) {
        this.index = index;
        this.writer = writer;
    }

    public Tools5eMarkdownConverter writeAll() {
        return writeFiles(List.of(Tools5eIndexType.values()));
    }

    public Tools5eMarkdownConverter writeImages() {
        index.tui().verbosef(Msg.WRITING, "Writing images and fonts");
        index.tui().copyImages(Tools5eSources.getImages());
        index.tui().copyFonts(Tools5eSources.getFonts());
        return this;
    }

    public Tools5eMarkdownConverter writeFiles(IndexType type) {
        return writeFiles(List.of(type));
    }

    static class WritingQueue {
        List<QuteBase> baseCompendium = new ArrayList<>();
        List<QuteBase> baseRules = new ArrayList<>();
        List<QuteNote> noteCompendium = new ArrayList<>();
        List<QuteNote> noteRules = new ArrayList<>();

        // Some state for combining notes
        Map<Tools5eIndexType, Json2QuteCommon> combinedDocs = new HashMap<>();
    }

    public Tools5eMarkdownConverter writeFiles(List<? extends IndexType> types) {
        if (index.notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        if (types == null || types.isEmpty()) {
            return this;
        }
        index.tui().verbosef("Converting data: %s", types);

        WritingQueue queue = new WritingQueue();
        for (var entry : index.includedEntries()) {
            final String key = entry.getKey();
            final JsonNode jsonSource = entry.getValue();

            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(key);
            if (types.contains(Tools5eIndexType.race) && nodeType == Tools5eIndexType.subrace) {
                // include subrace with race
            } else if (!types.contains(nodeType)) {
                continue;
            }

            if (nodeType.writeFile()) {
                writeQuteBaseFiles(nodeType, key, jsonSource, queue);
            } else if (nodeType.isOutputType() && nodeType.useQuteNote()) {
                writeQuteNoteFiles(nodeType, key, jsonSource, queue);
            }
        }

        // Group files by their target base paths and write them
        writeFilesByTargetPath(queue.baseCompendium, queue.baseRules);

        for (Json2QuteCommon value : queue.combinedDocs.values()) {
            append(value.type, value.buildNote(), queue.noteCompendium, queue.noteRules);
        }

        if (types.contains(Tools5eIndexType.spell) || types.contains(Tools5eIndexType.spellIndex)) {
            // We're doing this one a different way:
            // Too many different variations of spell list
            var spellIndexParent = new Json2QuteSpellIndex(index);
            queue.noteCompendium.addAll(spellIndexParent.buildNotes());
        }

        if (!Json2QuteBackground.traits.isEmpty()) {
            queue.noteCompendium.addAll(new BackgroundTraits2Note(index).buildNotes());
        }

        // Group notes by their target base paths and write them
        writeNotesByTargetPath(queue.noteCompendium, queue.noteRules);

        return this;
    }

    /**
     * Write files grouped by their target base paths to support per-type path configuration.
     */
    private void writeFilesByTargetPath(List<QuteBase> compendiumFiles, List<QuteBase> rulesFiles) {
        // Group all files by their target base path
        Map<Path, List<QuteBase>> filesByPath = new HashMap<>();

        // Add compendium files, determining their actual target path
        for (QuteBase file : compendiumFiles) {
            Path targetBasePath = getTargetBasePath(file);
            filesByPath.computeIfAbsent(targetBasePath, k -> new ArrayList<>()).add(file);
        }

        // Add rules files (they always go to rules path)
        if (!rulesFiles.isEmpty()) {
            filesByPath.computeIfAbsent(index.rulesFilePath(), k -> new ArrayList<>()).addAll(rulesFiles);
        }

        // Write each group of files to their target path
        for (Map.Entry<Path, List<QuteBase>> entry : filesByPath.entrySet()) {
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
     * Determine the target base path for a QuteBase item, considering per-type path configuration.
     */
    private Path getTargetBasePath(QuteBase item) {
        if (item.sources() instanceof Tools5eSources) {
            Tools5eSources sources = (Tools5eSources) item.sources();
            Tools5eIndexType type = sources.getType();
            if (type != null && type.useCompendiumBase()) {
                String configTypeName = type.getConfigTypeName();
                if (configTypeName != null && index.hasTypeSpecificPath(configTypeName)) {
                    return index.getTypeFilePath(configTypeName);
                }
            }
        }
        // Default fallback: use compendium path for compendium items
        return index.compendiumFilePath();
    }

    private void writeQuteBaseFiles(Tools5eIndexType type, String key, JsonNode jsonSource, WritingQueue queue) {
        var compendium = queue.baseCompendium;
        var rules = queue.baseRules;
        if (type == Tools5eIndexType.classtype) {
            Json2QuteClass jsonClass = new Json2QuteClass(index, type, jsonSource);
            QuteBase converted = jsonClass.build();
            if (converted != null) {
                compendium.add(converted);
                compendium.addAll(jsonClass.buildSubclasses());
            }
        } else {
            QuteBase converted = switch (type) {
                case background -> new Json2QuteBackground(index, type, jsonSource).build();
                case deck -> new Json2QuteDeck(index, type, jsonSource).build();
                case deity -> new Json2QuteDeity(index, type, jsonSource).build();
                case facility -> new Json2QuteBastion(index, type, jsonSource).build();
                case feat -> new Json2QuteFeat(index, type, jsonSource).build();
                case hazard, trap -> new Json2QuteHazard(index, type, jsonSource).build();
                case item, itemGroup -> new Json2QuteItem(index, type, jsonSource).build();
                case monster -> new Json2QuteMonster(index, type, jsonSource).build();
                case object -> new Json2QuteObject(index, type, jsonSource).build();
                case optfeature -> new Json2QuteOptionalFeature(index, type, jsonSource).build();
                case psionic -> new Json2QutePsionicTalent(index, type, jsonSource).build();
                case race, subrace -> new Json2QuteRace(index, type, jsonSource).build();
                case reward -> new Json2QuteReward(index, type, jsonSource).build();
                case spell -> new Json2QuteSpell(index, type, jsonSource).build();
                case vehicle -> new Json2QuteVehicle(index, type, jsonSource).build();
                default -> throw new IllegalArgumentException("Unsupported type " + type);
            };
            if (converted != null) {
                append(type, converted, compendium, rules);
            }
        }
    }

    private void writeQuteNoteFiles(Tools5eIndexType nodeType, String key, JsonNode node, WritingQueue queue) {
        var compendiumDocs = queue.noteCompendium;
        var ruleDocs = queue.noteRules;
        var combinedDocs = queue.combinedDocs;
        final var vrDir = linkifier().getRelativePath(Tools5eIndexType.variantrule);

        switch (nodeType) {
            case action -> {
                Json2QuteCompose action = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Actions"));
                action.add(node);
            }
            case adventureData, bookData -> {
                String metadataKey = key.replace("data|", "|");
                JsonNode metadata = index.getOrigin(metadataKey);
                if (!node.has("data")) {
                    index.tui().errorf("No data for %s", key);
                } else if (metadata == null) {
                    index.tui().errorf("Unable to find metadata (%s) for %s", metadataKey, key);
                } else if (index.isIncluded(metadataKey)) {
                    compendiumDocs.addAll(new Json2QuteBook(index, nodeType, metadata, node).buildBook());
                } else {
                    index.tui().debugf(Msg.FILTER, "%s is excluded", metadataKey);
                }
            }
            case status, condition -> {
                Json2QuteCompose conditions = (Json2QuteCompose) combinedDocs.computeIfAbsent(
                        Tools5eIndexType.condition,
                        t -> new Json2QuteCompose(nodeType, index, "Conditions"));
                conditions.add(node);
            }
            case disease -> {
                Json2QuteCompose disease = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Diseases"));
                disease.add(node);
            }
            case itemMastery -> {
                Json2QuteCompose itemMastery = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Item Mastery"));
                itemMastery.add(node);
            }
            case itemProperty -> {
                Json2QuteCompose itemProperty = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Item Properties"));
                itemProperty.add(node);
            }
            case itemType -> {
                Json2QuteCompose itemTypes = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Item Types"));
                itemTypes.add(node);
            }
            case legendaryGroup -> {
                QuteNote converted = new Json2QuteLegendaryGroup(index, nodeType, node).buildNote();
                if (converted != null) {
                    compendiumDocs.add(converted);
                }
            }
            case optionalFeatureTypes -> {
                OptionalFeatureType oft = index.getOptionalFeatureType(node);
                if (oft == null) {
                    return;
                }
                QuteNote converted = new Json2QuteOptionalFeatureType(index, node, oft).buildNote();
                if (converted != null) {
                    compendiumDocs.add(converted);
                }
            }
            case sense -> {
                Json2QuteCompose sense = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Senses"));
                sense.add(node);
            }
            case skill -> {
                Json2QuteCompose skill = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Skills"));
                skill.add(node);
            }
            case table, tableGroup -> {
                Tools5eQuteNote tableNote = new Json2QuteTable(index, nodeType, node).buildNote();
                if (tableNote.getName().equals("Damage Types")) {
                    ruleDocs.add(tableNote);
                } else {
                    compendiumDocs.add(tableNote);
                }
            }
            case variantrule -> append(nodeType,
                    new Json2QuteNote(index, nodeType, node)
                            .useSuffix(true)
                            .withImagePath(vrDir)
                            .buildNote()
                            .withTargetPath(vrDir),
                    compendiumDocs, ruleDocs);
            default -> {
                // skip it
            }
        }
    }

    <T extends QuteBase> void append(Tools5eIndexType type, T note, List<T> compendium, List<T> rules) {
        if (note != null) {
            if (type.useCompendiumBase()) {
                compendium.add(note);
            } else {
                rules.add(note);
            }
        }
    }

    private static Tools5eLinkifier linkifier() {
        return Tools5eLinkifier.instance();
    }
}
