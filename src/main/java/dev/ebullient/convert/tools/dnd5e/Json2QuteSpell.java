package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.uppercaseFirst;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.SpellEntry.SpellReference;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.QuteAreaOfEffect;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.QuteDamage;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteSpell extends Json2QuteCommon {
    final String decoratedName;

    Json2QuteSpell(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        decoratedName = linkifier().decoratedName(type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        SpellEntry spellEntry = index().getSpellIndex().getSpellEntry(getSources().getKey());

        Tags tags = new Tags(getSources());

        tags.add("spell", "school", spellEntry.school.name());
        tags.add("spell", "level", JsonSource.spellLevelToText(spellEntry.level));
        if (spellEntry.ritual) {
            tags.add("spell", "ritual");
        }

        // 🔧 Spell: spell|fireball|phb,
        //    references: {subclass|destruction domain|cleric|phb|vss=subclass|destruction domain|cleric|phb|vss;c:5;s:null;null, ...}
        //    expanded: {subclass|the fiend|warlock|phb|phb=subclass|the fiend|warlock|phb|phb;c:null;s:3;null, ...}
        Set<String> referenceLinks = new HashSet<>();
        Set<SpellReference> allRefs = new TreeSet<>(Comparator.comparing(x -> x.refererKey));
        allRefs.addAll(spellEntry.references.values());
        allRefs.addAll(spellEntry.expandedList.values());

        for (var r : allRefs) {
            tags.addRaw(r.tagifyReference());
            referenceLinks.add(r.linkifyReference());
        }

        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, "##");
        if (SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            maybeAddBlankLine(text);
            appendToText(text, SpellFields.entriesHigherLevel.getFrom(rootNode),
                    textContains(text, "## ") ? "##" : null);
        }

        return new QuteSpell(sources,
                decoratedName,
                getSourceText(sources),
                JsonSource.spellLevelToText(spellEntry.level),
                spellEntry.school.name(),
                spellEntry.ritual,
                spellCastingTime(),
                spellRange(),
                spellComponents(),
                hasVerbalComponent(),
                hasSomaticComponent(),
                getMaterialComponentText(),
                getDamageTypes(),
                getSavingThrows(),
                spellDuration(),
                referenceLinks,
                getDamageInfo(),
                getAreaOfEffect(),
                getSavingThrowSucceeds(),
                getFluffImages(Tools5eIndexType.spellFluff),
                String.join("\n", text),
                tags);
    }

    SpellSchool getSchool() {
        String code = SpellFields.school.getTextOrEmpty(rootNode);
        return index().findSpellSchool(code, getSources());
    }

    boolean spellIsRitual() {
        boolean ritual = false;
        JsonNode meta = SpellFields.meta.getFrom(rootNode);
        if (meta != null) {
            ritual = SpellFields.ritual.booleanOrDefault(meta, false);
        }
        return ritual;
    }

    String spellComponents() {
        JsonNode components = SpellFields.components.getFrom(rootNode);
        if (components == null) {
            return "";
        }

        List<String> list = new ArrayList<>();
        for (Entry<String, JsonNode> f : iterableFields(components)) {
            switch (f.getKey().toLowerCase()) {
                case "v" -> list.add("V");
                case "s" -> list.add("S");
                case "m" -> {
                    list.add(materialComponents(f.getValue()));
                }
                case "r" -> list.add("R"); // Royalty. Acquisitions Incorporated
            }
        }
        return String.join(", ", list);
    }

    String materialComponents(JsonNode source) {
        return "M (%s)".formatted(
                source.isObject()
                        ? SpellFields.text.replaceTextFrom(source, this)
                        : replaceText(source.asText()));
    }

    boolean hasVerbalComponent() {
        JsonNode components = SpellFields.components.getFrom(rootNode);
        return components != null && booleanOrDefault(components, "v", false);
    }

    boolean hasSomaticComponent() {
        JsonNode components = SpellFields.components.getFrom(rootNode);
        return components != null && booleanOrDefault(components, "s", false);
    }

    String getMaterialComponentText() {
        JsonNode components = SpellFields.components.getFrom(rootNode);
        if (components == null) {
            return "";
        }
        JsonNode materialNode = components.get("m");
        if (materialNode == null) {
            return "";
        }
        return materialNode.isObject()
                ? SpellFields.text.replaceTextFrom(materialNode, this)
                : replaceText(materialNode.asText());
    }

    List<String> getDamageTypes() {
        JsonNode damageInflict = SpellFields.damageInflict.getFrom(rootNode);
        if (damageInflict == null || !damageInflict.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode node : damageInflict) {
            String damageType = node.asText();
            result.add(damageTypeToFull(damageType));
        }
        return result;
    }

    List<String> getSavingThrows() {
        JsonNode savingThrow = SpellFields.savingThrow.getFrom(rootNode);
        if (savingThrow == null || !savingThrow.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode node : savingThrow) {
            String saveType = node.asText();
            result.add(uppercaseFirst(saveType.toLowerCase()));
        }
        return result;
    }

    QuteDamage getDamageInfo() {
        String baseDamage = extractBaseDamage();
        String scaling = extractScalingDamage();
        Integer scalingLevel = extractScalingLevel();
        String scalingDamage = extractScalingDamagePerLevel();

        // Return null if no damage information is found
        if (baseDamage == null && scaling == null && scalingLevel == null && scalingDamage == null) {
            return null;
        }

        return new QuteDamage(baseDamage, scaling, scalingLevel, scalingDamage);
    }

    private String extractBaseDamage() {
        // Look for damage dice patterns in the main spell text
        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, null);
        String spellText = String.join(" ", text);

        // Simple regex to find damage dice notation (e.g., "8d6", "2d10+5")
        String damagePattern = "\\b(\\d+d\\d+(?:[+\\-]\\d+)?)\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(damagePattern);
        java.util.regex.Matcher matcher = pattern.matcher(spellText);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractScalingDamage() {
        // Look for scaling information in entriesHigherLevel
        if (SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            List<String> higherLevelText = new ArrayList<>();
            appendToText(higherLevelText, SpellFields.entriesHigherLevel.getFrom(rootNode), null);
            String scaling = String.join(" ", higherLevelText);

            // Clean up the text and extract just the scaling description
            if (!scaling.isEmpty()) {
                // Remove "At Higher Levels" header if present, including markdown formatting
                scaling = scaling.replaceFirst(
                        "(?i)^\\s*\\*\\*at\\s+higher\\s+levels\\.?\\*\\*\\s*|^\\s*at\\s+higher\\s+levels[.:]*\\s*", "");
                return scaling.trim();
            }
        }

        return null;
    }

    private Integer extractScalingLevel() {
        // Look for scaling level information in entriesHigherLevel
        if (SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            List<String> higherLevelText = new ArrayList<>();
            appendToText(higherLevelText, SpellFields.entriesHigherLevel.getFrom(rootNode), null);
            String scaling = String.join(" ", higherLevelText);

            // Look for patterns like "4th level or higher", "3rd level or higher", etc.
            String levelPattern = "\\b(\\d+)(?:st|nd|rd|th)\\s+level\\s+or\\s+higher";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(levelPattern,
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(scaling);

            if (matcher.find()) {
                try {
                    return Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

        return null;
    }

    private String extractScalingDamagePerLevel() {
        // Look for damage increase per level in entriesHigherLevel
        if (SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            List<String> higherLevelText = new ArrayList<>();
            appendToText(higherLevelText, SpellFields.entriesHigherLevel.getFrom(rootNode), null);
            String scaling = String.join(" ", higherLevelText);

            String scalingDamagePerLevelPattern = "dice:(\\d+d\\d+)";

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(scalingDamagePerLevelPattern,
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(scaling);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    QuteAreaOfEffect getAreaOfEffect() {
        // Check if this spell has an area effect in the range field
        JsonNode range = SpellFields.range.getFrom(rootNode);
        if (range != null) {
            String type = SpellFields.type.getTextOrEmpty(range);

            // For area spells, the type might be "cone", "sphere", etc.
            if (isAreaOfEffectType(type)) {
                JsonNode distance = SpellFields.distance.getFrom(range);
                String amountStr = SpellFields.amount.getTextOrEmpty(distance);

                try {
                    Integer size = Integer.valueOf(amountStr);
                    return new QuteAreaOfEffect(type, size);
                } catch (NumberFormatException e) {
                    // If amount is not a number, still return the shape
                    return new QuteAreaOfEffect(type, null);
                }
            }
        }

        // Also check if there's area information in the spell text
        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, null);
        String spellText = String.join(" ", text);

        // Look for area descriptions in text (e.g., "20-foot radius", "30-foot cone")
        String areaPattern = "(\\d+)[-\\s]*foot[-\\s]*(radius|cone|line|cube|sphere|cylinder)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(areaPattern,
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(spellText);

        if (matcher.find()) {
            try {
                Integer size = Integer.valueOf(matcher.group(1));
                String shape = matcher.group(2).toLowerCase();
                // Normalize "radius" to "sphere"
                if ("radius".equals(shape)) {
                    shape = "sphere";
                }
                return new QuteAreaOfEffect(shape, size);
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        return null;
    }

    private boolean isAreaOfEffectType(String type) {
        return type != null && ("cone".equals(type) ||
                "sphere".equals(type) ||
                "line".equals(type) ||
                "cube".equals(type) ||
                "cylinder".equals(type) ||
                "emanation".equals(type) ||
                "hemisphere".equals(type) ||
                "radius".equals(type));
    }

    String getSavingThrowSucceeds() {
        // Look for saving throw outcomes in the spell text
        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, null);
        String spellText = String.join(" ", text).toLowerCase();

        // Common patterns for saving throw outcomes
        if (spellText.contains("half as much damage on a successful") ||
                spellText.contains("half damage on a success")) {
            return "half damage";
        }

        if (spellText.contains("no damage on a successful") ||
                spellText.contains("takes no damage on a success")) {
            return "no damage";
        }

        if (spellText.contains("reduced effect on a successful") ||
                spellText.contains("lesser effect on a success")) {
            return "reduced effect";
        }

        if (spellText.contains("avoids the effect on a successful") ||
                spellText.contains("is unaffected on a success")) {
            return "no effect";
        }

        // If we have saving throws but can't determine the outcome, return generic text
        if (!getSavingThrows().isEmpty()) {
            return "see spell description";
        }

        return null;
    }

    String spellDuration() {
        StringBuilder result = new StringBuilder();
        JsonNode durations = SpellFields.duration.ensureArrayIn(rootNode);
        if (durations.size() > 0) {
            addDuration(durations.get(0), result);
        }
        if (durations.size() > 1) {
            JsonNode ends = durations.get(1);
            result.append(", ");
            String type = SpellFields.type.getTextOrEmpty(ends);
            if ("timed".equals(type)) {
                result.append("up to ");
            }
            addDuration(ends, result);
        }
        return result.toString();
    }

    void addDuration(JsonNode element, StringBuilder result) {
        String type = SpellFields.type.getTextOrEmpty(element);
        switch (type) {
            case "instant" -> result.append("Instantaneous");
            case "permanent" -> {
                result.append("Until dispelled");
                if (element.withArray("ends").size() > 1) {
                    result.append(" or triggered");
                }
            }
            case "special" -> result.append("Special");
            case "timed" -> {
                if (booleanOrDefault(element, "concentration", false)) {
                    result.append("Concentration, up to ");
                }
                JsonNode duration = element.get("duration");
                String amount = SpellFields.amount.getTextOrEmpty(duration);
                result.append(amount)
                        .append(" ")
                        .append(pluralize(
                                SpellFields.type.getTextOrEmpty(duration),
                                Integer.valueOf(amount)));
            }
            default -> tui().errorf("What is this? %s", element.toPrettyString());
        }
    }

    String spellRange() {
        StringBuilder result = new StringBuilder();
        JsonNode range = SpellFields.range.getFrom(rootNode);
        if (range != null) {
            String type = SpellFields.type.getTextOrEmpty(range);
            JsonNode distance = SpellFields.distance.getFrom(range);
            String distanceType = SpellFields.type.getTextOrEmpty(distance);
            String amount = SpellFields.amount.getTextOrEmpty(distance);

            switch (type) {
                case "cube", "cone", "emanation", "hemisphere", "line", "radius", "sphere" -> {// Self (xx-foot yy)
                    result.append("Self (")
                            .append(amount)
                            .append("-")
                            .append(pluralize(distanceType, 1))
                            .append(" ")
                            .append(uppercaseFirst(type))
                            .append(")");
                }
                case "point" -> {
                    switch (distanceType) {
                        case "self", "sight", "touch", "unlimited" ->
                            result.append(uppercaseFirst(distanceType));
                        default -> result.append(amount)
                                .append(" ")
                                .append(distanceType);
                    }
                }
                case "special" -> result.append("Special");
            }
        }
        return result.toString();
    }

    String spellCastingTime() {
        StringBuilder result = new StringBuilder();
        JsonNode time = rootNode.withArray("time").get(0);
        String number = SpellFields.number.getTextOrEmpty(time);
        String unit = SpellFields.unit.getTextOrEmpty(time);
        result.append(number).append(" ");
        switch (unit) {
            case "action", "reaction" ->
                result.append(uppercaseFirst(unit));
            case "bonus" ->
                result.append(uppercaseFirst(unit))
                        .append(" Action");
            default ->
                result.append(unit);
        }
        return pluralize(result.toString(), Integer.valueOf(number));
    }

    enum SpellFields implements JsonNodeReader {
        amount,
        className,
        classSource,
        classes,
        components,
        damageInflict,
        distance,
        duration,
        entriesHigherLevel,
        level,
        meta,
        number,
        range,
        ritual,
        savingThrow,
        school,
        self,
        sight,
        special,
        text,
        touch,
        type,
        unit,
        unlimited,
        definedInSource,
        spellAttack,
    }
}
