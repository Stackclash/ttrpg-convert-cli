package dev.ebullient.convert.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserConfig {

    @JsonAlias({ "convert", "full-source", "fullSource" })
    Sources sources = new Sources();

    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    List<String> from = new ArrayList<>();

    VaultPaths paths = new VaultPaths();

    List<String> include = new ArrayList<>();

    List<String> includePattern = new ArrayList<>();

    List<String> includeGroup = new ArrayList<>();

    List<String> exclude = new ArrayList<>();

    List<String> excludePattern = new ArrayList<>();

    ReprintBehavior reprintBehavior = ReprintBehavior.newest;

    Map<String, String> template = new HashMap<>();

    Boolean useDiceRoller = null;
    Boolean yamlStatblocks = null;

    String tagPrefix = "";

    ImageOptions images = new ImageOptions();

    List<String> references() {
        List<String> reference = new ArrayList<>();
        reference.addAll(sources.reference);
        if (from != null) {
            reference.addAll(from);
        }
        return reference;
    }

    enum ConfigKeys {
        defaultSource,
        exclude,
        excludePattern,
        fallbackPaths(List.of("fallback-paths")),
        from,
        images,
        include,
        includeGroups,
        includePattern,
        paths,
        reprintBehavior,
        sources(List.of("fullSource", "full-source", "convert")),
        tagPrefix,
        template,
        useDiceRoller,
        yamlStatblocks,
        ;

        final List<String> aliases;

        ConfigKeys() {
            aliases = List.of();
        }

        ConfigKeys(List<String> aliases) {
            this.aliases = aliases;
        }

        JsonNode get(JsonNode node) {
            JsonNode child = node.get(this.name());
            if (child == null) {
                Optional<JsonNode> y = aliases.stream()
                        .map(node::get)
                        .filter(Objects::nonNull)
                        .findFirst();
                return y.orElse(null);
            }
            return child;
        }
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class VaultPaths {
        String compendium;
        String rules;

        // Per-compendium-type paths for fine-grained control
        String adventures;
        String backgrounds;
        String books;
        String classes;
        String conditions;
        String decks;
        String deities;
        String facilities;
        String feats;
        String items;
        String monsters;
        String races;
        String spells;
        String tables;
        String variantRules;

        // Pathfinder 2e specific types
        String actions;
        String ancestries;
        String archetypes;
        String afflictions;
        String creatures;
        String hazards;
        String rituals;
        String traits;
        String vehicles;
        String equipment;
        String relics;
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class Sources {
        String toolsRoot;
        List<String> reference = new ArrayList<>();
        List<String> adventure = new ArrayList<>();
        List<String> book = new ArrayList<>();
        List<String> homebrew = new ArrayList<>();
        Map<String, String> defaultSource = new HashMap<>();
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class ImageOptions {
        String internalRoot;
        Boolean copyInternal;
        Boolean copyExternal;
        final Map<String, String> fallbackPaths = new HashMap<>();

        public ImageOptions() {
        }

        public ImageOptions(ImageOptions images, ImageOptions images2) {
            if (images != null) {
                copyExternal = images.copyExternal;
                copyInternal = images.copyInternal;
                internalRoot = images.internalRoot;
                fallbackPaths.putAll(images.fallbackPaths);
            }
            if (images2 != null) {
                copyExternal = images2.copyExternal == null
                        ? copyExternal
                        : images2.copyExternal;
                copyInternal = images2.copyInternal == null
                        ? copyInternal
                        : images2.copyInternal;
                internalRoot = images2.internalRoot == null
                        ? internalRoot
                        : images2.internalRoot;
                fallbackPaths.putAll(images2.fallbackPaths);
            }
        }

        public boolean copyExternal() {
            return copyExternal != null && copyExternal;
        }

        public boolean copyInternal() {
            return copyInternal != null && copyInternal;
        }

        public Map<String, String> fallbackPaths() {
            return Collections.unmodifiableMap(fallbackPaths);
        }
    }
}
