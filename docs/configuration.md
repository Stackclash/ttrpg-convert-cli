# CLI Configuration guide

> [!IMPORTANT]
> 🚀 Respect copyrights and support content creators; use only the sources you own.

This guide introduces you to configuring data transformations using the Command Line Interface (CLI). Whether you're new to command line tools or an experienced user, you'll find helpful information on utilizing configuration files to tailor your experience.

<!-- markdownlint-disable-next-line no-emphasis-as-heading -->
**Table of Contents**

- [Overview](#overview)
    - [Basic configuration example](#basic-configuration-example)
    - [Advanced configuration example](#advanced-configuration-example)
- [Source identifiers](#source-identifiers)
- [Specify content with `sources`](#specify-content-with-sources)
    - [Homebrew](#homebrew)
        - [Additional notes about homebrew](#additional-notes-about-homebrew)
    - [Reporting content errors to 5eTools](#reporting-content-errors-to-5etools)
- [Specify target vault paths (`paths`)](#specify-target-vault-paths-paths)
- [Refine content choices](#refine-content-choices)
    - [Excluding content matching an `excludePattern`](#excluding-content-matching-an-excludepattern)
    - [Excluding specific content with `exclude`](#excluding-specific-content-with-exclude)
    - [Including specific content with `include`](#including-specific-content-with-include)
- [Reprint behavior](#reprint-behavior)
    - [Troubleshooting reprint behavior](#troubleshooting-reprint-behavior)
- [Use the dice roller plugin](#use-the-dice-roller-plugin)
- [Render with Fantasy Statblocks](#render-with-fantasy-statblocks)
- [Tag prefix](#tag-prefix)
- [Templates](#templates)
    - [Customizing templates](#customizing-templates)
- [Images](#images)
    - [Copying internal images](#copying-internal-images)
    - [Copying external images](#copying-external-images)
    - [Fallback paths](#fallback-paths)
- [Customizing the default source](#customizing-the-default-source)
- [Migrating `from`, `full-source`, and `convert`](#migrating-from-full-source-and-convert)

## Overview

The CLI can be set up using JSON or YAML files. These files allow you to specify your preferences and can be used alongside or in place of command-line options. For examples of configuration file structures in both formats, see [examples/config](../examples/config).

> [!NOTE]
> 📝 JSON and YAML are both file formats for storing data in useful and human-readable ways.
>
> - JSON: If you want to know why the `{}` and `[]` are used in the ways that they are you can read about json *objects* and *arrays* [here](https://www.toolsqa.com/rest-assured/what-is-json/)).
> - YAML: A format where indentation (spaces at the beginning of lines) is important. Learn about YAML's [specification](https://yaml.org/spec/1.2/spec.html).

The following examples are presented in JSON.

### Basic configuration example

Below is a straightforward `config.json` file. In this format, settings are noted in a `"key": "value"` structure.

``` json
{
    "sources": {
        "book": [
            "DMG",
            "PHB",
            "MM"
        ]
    },
    "paths": {
        "compendium": "z_compendium/",
        "rules": "z_compendium/rules"
    }
}
```

This example performs two basic functions:

1. **Select Input Sources:** The `sources` key lists the sources to be included, identified by their [source identifiers](#source-identifiers).
2. **Define Vault Paths:** The [`paths`](#specify-target-vault-paths-paths) key sets the destination paths for the `compendium` and `rules` content. These paths are relative to the output directory set in the CLI command with `-o`.

> [!WARNING]
> **Windows Users**: Replace any `\` with `/` in paths in JSON or YAML files

### Advanced configuration example

Here's a more comprehensive `config.json` file.

```json
{
    "sources": {
        "adventure": [
            "LMoP",
            "LoX"
        ],
        "book": [
            "PHB"
        ],
        "reference": [
            "AI",
            "DMG",
            "TCE",
            "ESK",
            "DIP",
            "XGE",
            "FTD",
            "MM",
            "MTF",
            "VGM"
        ],
        "homebrew": [
            "homebrew/creature/MCDM Productions; Flee, Mortals!.json"
        ]
    },
    "paths": {
        "rules": "/compendium/rules/",
        "types": {
            "monsters": "/bestiary/",
            "spells": "/magic/spells/"
        }
    },
    "excludePattern": [
        "race\\|.*\\|dmg"
    ],
    "exclude": [
        "monster|expert|dc",
        "monster|expert|sdw",
        "monster|expert|slw"
    ],
    "include": [
        "race|changeling|mpmm"
    ],
    "reprintBehavior": "newest",
    "useDiceRoller": true,
    "tagPrefix": "ttrpg-cli",
    "template": {
        "background": "examples/templates/tools5e/images-background2md.txt",
        "monster": "examples/templates/tools5e/monster2md-scores.txt"
    }
}
```

Additional capabilities:

1. **Select input sources:** The [`sources`](#specify-content-with-sources) key is used to select included sources (full text from two adventures and a book, reference content from a slew of other official sources, and one [homebrew source](#homebrew)).
2. **Define Vault Paths:** The [`paths`](#specify-target-vault-paths-paths) sets the vault path destination for `rules` content and per-type paths for `monsters` and `spells`.
3. **Targeted exclusion:** [`excludePattern`](#excluding-content-matching-an-excludepattern) and [`exclude`](#excluding-specific-content-with-exclude) leaves out specific content.
4. **Targeted inclusion:** The [`include`](#including-specific-content-with-include) specifies content that is *always included*.
5. **[Reprint behavior](#reprint-behavior):** Only the latest/newest version of a resource should be emitted (this is the default).
6. **Use the dice roller plugin:** The [`useDiceRoller`](#use-the-dice-roller-plugin) key enables the dice roller plugin.
7. **Tag prefix:** The [`tagPrefix`](#tag-prefix) key sets the prefix for tags generated by the CLI.
8. **Templates:** The [`template`](#templates) key specifies the templates to use for different types of content.

> [!WARNING]
> **Windows Users**: Replace any `\` in your paths with '/' in your JSON and YAML files.

## Source identifiers

> 🚀 Remember: Only include content you legally own.

Sources in 5eTools and Pf2eTools are referenced by unique identifiers. Find the identifiers for your sources in the [Source Map](sourceMap.md).

Content is classified as a `book` or `adventure` (shown as the third column in the source map). Use this classification when [specifying your sources](#specify-content-with-sources).

Some sources are split into multiple files, in which case, you will need to specify each identifier separately. For example, *Tales from the Yawning Portal* is split into seven files. Content appears using any one of the seven (`TftYP-*`), in addition to `TftYP` for common content. If you want to include all of them, you will need to specify each identifier separately.

If you're expecting to see content from a book or adventure and it's not showing up, run the CLI with the `--index` option, and check the `all-index.json` file to see which source identifier you should be using.

## Specify content with `sources`

> 🔥 Version 3.x or SNAPSHOT ONLY. If you're using a 2.x version of the CLI, use [the legacy version](#migrating-from-full-source-and-convert)

The CLI can emit content from a source in two ways:

- "full text": notes for all content and reference data from your sources.
    When including the full text, use the `book`, `adventure`, or `homebrew` key as appropriate for the source material.
- "reference only": only emit reference notes (spells, classes, etc.).
    Use the `reference` key to include reference content from books or adventures.

With that in mind, specify your sources in this way:

```json
"sources": {
    "adventure": [
        "WBtW"
    ],
    "book": [
        "PHB"
    ],
    "reference": [
        "MPMM"
    ]
}
```

The above example that will include full text for the *Player's Handbook* (a book, PHB) and *The Wild Beyond the Witchlight* (an adventure, WBtW), but will only create reference notes (backgrounds, cults/boons, races, traps/hazards) from *Mordenkainen Presents: Monsters of the Multiverse* (MPMM).

> [!TIP]
> You only need to list your source once.

### Homebrew

> [!TIP]
> 🍺 *You only need the particular file you wish to import*.
>
> Homebrew data is different from the 5etools or Pf2eTools data. Each homebrew file is a complete reference. If you compare it to cooking: the 5etools and Pf2eTools mirror repositories are organized by ingredient (all of the carrots, all of the onions, ... ); homebrew data is organized by prepared meal / complete receipe.
>
> Support your content creators! Only use homebrew that you own.

To include Homebrew in your notes, specify the path to the homebrew json file in a `homebrew` section inside of `sources`.

For example, if you wanted to use Benjamin Huffman's popular homebrewed [Pugilist class](https://www.dmsguild.com/product/184921/The-Pugilist-Class):

1. Download a copy of the [Pugilist json file](https://github.com/TheGiddyLimit/homebrew/blob/master/class/Benjamin%20Huffman%3B%20Pugilist.json).

    Save this file to a well-known location on your computer. It is probably easiest if it sits next your 5eTools or Pf2eTools directory.

2. Add the path to this file to a `homebrew` section under `sources`:

    ```json
    {
      "sources": {
        "homebrew": [
            "path/to/Benjamin Huffman; Pugilist.json"
        ]
      }
    }
    ```

In the above example, `path/to/` is a placeholder. If you use a relative path, it will be resolved relative to the current working directory[^1]. An absolute path[^2] will also work.

There are a few ways to figure out the path to a file:

- You may be able to drag and drop the file into the terminal window.
- You may have the ability to right-click on the file and select "Copy Path".

> [!WARNING]
> **Windows Users**: Replace any `\` with `/` in paths in JSON or YAML files

#### Additional notes about homebrew

Homebrew json files are not rigorously validated. There may be errors when importing.
I've done what I can to make the errors clear, or to highlight the suspect json, but I can't catch everything.

Here are some examples of what you may see, and how to fix them:

- `Unable to find image 'img/bestiary/MM/Green Hag.jpg'` (or similar)

    This kind of path refers to an "internal" (meaning part of the base 5e corpus of stuff) image. These paths are computed relative to a known base.

    Recent releases of 5eTools use a different repository structure, see [Copying "internal" images](#copying-internal-images)), and images have, by and large, been converted from `.jpg` or `.png` to `.webp`.
    Fixing this kind of error is usually a case of fixing the path.

    - If you can fix the link yourself (change to `.webp` and guess the new location by removing `img/`), please [report it in 5eTools #brew-conversion](#reporting-content-errors-to-5etools).
    - If you can't find the image that should be used instead, please [ask for CLI help](../README.md#where-to-find-help)and we'll help you find the right one.

- `Unknown spell school Curse in sources[spell|ventus|wandsnwizards]`; similar for item types, item properties, conditions, skills, abilities, etc.

    This kind of error could be caused by a missing companion file (check dependencies listed in the `meta` information at the top of the homebrew file) or a missing definition.

    - If you find the missing definition (or it is already present in the homebrew file), and the error persists, please [ask for CLI help first](../README.md#where-to-find-help) so I can make sure there isn't a bug in the CLI preventing it from being read.
    - If the data really is missing from the homebrew json, please [report it in 5eTools #brew-conversion](#reporting-content-errors-to-5etools).

- You may see messages about missing fields or badly formed tables.

    - If you can fix the error by fixing the json content, please [report it in 5eTools #brew-conversion](#reporting-content-errors-to-5etools).
    - If you can't fix the error yourself, please [ask for CLI help first](../README.md#where-to-find-help) so I can make sure there isn't a bug in the CLI.

### Reporting content errors to 5eTools

> [!NOTE]
> The lovely folks at 5eTools don't understand the CLI, and they don't need to. If you report an issue, keep the details focused on the JSON content (typo, missed definition, etc.). If you aren't sure, [ask for CLI help first](../README.md#where-to-find-help).

If you can fix the error by fixing the json content, please report the error in the 5eTools discord channel.

1. Identify the homebrew source that contains the error in the [homebrew manager](https://wiki.tercept.net/en/5eTools/HelpPages/managebrew).
2. Click the "View Converters" button on the right to find the converter(s).
3. Tag them in the [#⁠brew-conversion](https://discord.com/channels/363680385336606740/493154206115430400) channel, describing what you fixed. If you can't find the right user to tag, or if no one is listed as a converter, post the report anyway but make sure you mention this!

Use the following form for your report.

```text
**Brew:**   Write the name of the homebrew source here  
**Converter:**   @converter's-discord-ID   
**Issue:**   Describe the issue here in clear, concise terms (e.g. "the Red-Spotted Gurgler is listed as having AC 15, when it should be 16")  
**Steps for reproduction:**   If you have to do something specific to make the error appear, describe them here. 
> I go to X and click on Y 
> I expected Z, but instead ...
```

For that last part, you may need to do some digging. Do not report the error using CLI exception messages. Stick to the observed missing links or errors in the data.

## Specify target vault paths (`paths`)

The `paths` key specifies vault path for generated content.

- New directories are made if they aren't already present.
- Paths are relative to the CLI's designated output location (`-o`), which correlates to the root of your Obsidian vault.

### Basic path configuration

**Example:**

```json
  "paths": {
    "compendium": "/compendium/",
    "rules": "/rules/"
  }
```

> [!TIP]
> The leading slash is optional. It marks a path starting from the root of your Obsidian vault.

5eTools and Pf2eTools content is organized differently, but in general, information is organized as follows:

- `compendium`: backgrounds, classes, items, spells, monsters, etc.
- `rules`: conditions, weapon properties, variant rules, etc.

### Per-type path configuration

You can specify individual output paths for different content types using the `types` key within `paths`. This provides finer control over file organization.

**Example:**

```json
  "paths": {
    "compendium": "/compendium/",
    "rules": "/rules/",
    "types": {
      "monsters": "/bestiary/",
      "spells": "/magic/spells/",
      "items": "/equipment/",
      "backgrounds": "/character/backgrounds/"
    }
  }
```

**Supported content types:**

**D&D 5th Edition:**
- `monsters`, `spells`, `items`, `backgrounds`, `classes`, `races`, `feats`, `optional-features`
- `deities`, `facilities`, `hazards`, `traps`, `objects`, `vehicles`, `psionics`, `rewards`
- `decks`, `cards`, `cults`, `boons`, `diseases`, `conditions`, `actions`, `variant-rules`
- `tables`, `skills`, `senses`, `statuses`, `item-types`, `item-properties`, `languages`

**Pathfinder 2nd Edition:**
- `creatures`, `spells`, `items`, `ancestries`, `backgrounds`, `classes`, `archetypes`, `feats`
- `hazards`, `deities`, `afflictions`, `curses`, `diseases`, `rituals`, `companions`
- `familiar-abilities`, `actions`, `traits`, `domains`, `abilities`, `adventures`
- `languages`, `organizations`, `places`, `planes`, `events`, `relic-gifts`, `vehicles`

**Behavior:**
- If a content type has a specific path configured, files of that type are written to the specified directory
- If no specific path is configured for a content type, files are written to the default `compendium` directory
- Rules content (conditions, weapon properties, etc.) always uses the `rules` path regardless of per-type configuration
- All existing configurations continue to work without modification

**Mixed configuration example:**

```json
  "paths": {
    "compendium": "/compendium/",
    "types": {
      "monsters": "/bestiary/",
      "spells": "/magic/"
    }
  }
```

In this example:
- Monsters are written to `/bestiary/`
- Spells are written to `/magic/`
- All other compendium content (items, backgrounds, etc.) goes to `/compendium/`
- Rules content uses the default `/rules/` path

> [!WARNING]
> Do not reorganize or edit the generated content. Tuck generated content away in your vault and use it as read-only reference material. It should be cheap and easy to re-run the tool (add more content, errata, etc.). See [Recommendations](../README.md#recommendations-for-using-the-cli) for more information.

## Refine content choices

You can use the following configuration to exclude or include specific data.

Just as source material has an identifier, so does each piece of data. The *Monster Manual* has the identifier `MM`. Each monster in the *Monster Manual* has its own key, such as `monster|black dragon wyrmling|mm` or `item|drow +1 armor|mm`.

The CLI `--index` option compiles two lists of data keys:

- `all-index.json`: Lists all discovered data keys.
- `src-index.json`: Lists the data keys after source filters (`adventure`, `book`, `reference`, and the config options below) have been applied.

### Excluding content matching an `excludePattern`

This option allows you to exclude data entries based on regular expression matching patterns.

Note: A pipe (`|`) is a special character in regular expressions, and must be escaped.

- JSON

    ```json
    "excludePattern": [
        "race\\|.*\\|dmg"
    ]
    ```

- YAML

    ```yaml
    excludePattern:
      - race\|.*\|dmg
    ```

### Excluding specific content with `exclude`

Specify the data keys you want to omit.

```json
"exclude": [
    "monster|expert|dc",
    ...
]
```

### Including specific content with `include`

Specify the data keys you want to include.

```json
"include": [
    "race|changeling|mpmm"
]
```

This approach is ideal for content acquired in parts, like individual items from D&D Beyond.

## Reprint behavior

> 🔥 Version 3.x or SNAPSHOT ONLY.

Content is often reprinted or updated in later sources or editions. This setting lets you control how reprinted or revised content is handled when generating notes.

``` json
  "reprintBehavior": "newest"
```

This setting has 3 possible values:

- **`newest`** (default): Only includes notes for the most recent version of reprinted content.
- **`edition`**: Focuses on preserving content across incompatible editions (especially for 5e rules).

    Example: The edition check will preserve 2014 edition-specific class and subclass definitions. Other resources (that are not different across editions) will follow the reprints to include new content.

- **`all`**: Includes notes for all reprinted versions from enabled sources

In most cases, you will get the most recent version of the resource that is included, as most resources do not have substantial changes across editions.

For example, `trap|pits|dmg` is reprinted as `trap|hidden pit|xdmg`. If both versions are included by your configuration, you will only get a note for the `XDMG` version unless `reprintBehavior` is set to `all` or you have an explicit include rule that preserves the `DMG` version (in which case, you'll get both).

### Troubleshooting reprint behavior

If the behavior isn’t what you expect, run with the --log option and check the log file.
The log will show whether a specific key was kept or dropped and explain why.

To ensure a specific resource is included, add its key to the [`include` filter](#including-specific-content-with-include) instead of relying on reprint behavior.

## Use the dice roller plugin

The CLI can generate notes that include inline dice rolls. To enable this feature, set the `useDiceRoller` attribute to `true`.

## Render with Fantasy Statblocks

If you are using the Fantasy Statblocks plugin to render your statblocks, set `yamlStatblocks` to `true`. This will remove backticks and other formatting from statblock text.

## Tag prefix

The `tagPrefix` key sets the prefix for tags generated by the CLI. This is useful if you want to distinguish between tags generated by the CLI and tags you've created yourself.

For example, the CLI generates tags like `compendium/src/phb` and `spell/level/1`. If you set `tagPrefix` to `5e-cli`, the tags will be `5e-cli/compendium/src/phb` and `5e-cli/spell/level/1`.

## Templates

The CLI uses the [Qute Templating Engine](https://quarkus.io/guides/qute) to render markdown output. Use the `template` attribute in your configuration file to specify the templates you want to use for different types of content.

``` json
  "template": {
    "background": "examples/templates/tools5e/images-background2md.txt",
    "monster": "examples/templates/tools5e/monster2md-scores.txt"
  }
```

- **Default templates** are included in the `-examples.zip` file from the release, or can be viewed in the [src/main/resources/templates](../src/main/resources/templates) directory.
- **Additional templates** are available in the [examples/templates](../examples/templates) directory.

> [!TIP] The key used to specify a template corresponds to the type of template being used. You can find the list of valid template keys in the [source code](../src/main/resources/convertData.json) (look for `templateKeys`).

- Valid templates for 5etools: `background`, `class`, `deck`, `deity`, `feat`, `hazard`, `index.txt`, `item`, `monster`, `note`, `object`, `psionic`, `race`, `reward`, `spell`, `subclass`, `vehicle`.
- Valid templates for Pf2eTools: `ability`, `action`, `affliction`, `archetype`, `background`, `book`, `deity`, `feat`, `hazard`, `inline-ability`, `inline-affliction`, `inline-attack`, `item`, `note`, `ritual`, `spell`, `trait`.

### Customizing templates

Documentation is generated for [**template attributes**](./templates/).

Not everything is customizable. Some indenting, organizing, formatting, and linking is easier to do consistently while rendering big blobs of text.

See the [examples templates](../examples/templates) for reference.

## Images

The CLI can copy images referenced in the content to your vault. This is useful if you want to use the content offline or if you want to ensure that images are available in your vault.

- Internal images are part of the 5eTools or Pf2e tools corpus of content. They are referenced by computed path (like tokens) or by media references marked as "internal".
- "External" images are usually marked in the Json source as "external" and are referenced by a URL.

### Copying internal images

5eTools mirror-2 moved internal images into a separate repository. Downloads can take some time, and the images repository is quite large.

**By default, no files are downloaded**. Links will reference the remote location, and you will need to be online to view the images. This is a safe, fast, and relatively well-behaving option given that downloads can be quite slow, and you may change your mind about where you want content to be generated.

The following configuration options allow you to change how the CLI treats these internal image references.

- Set `images.copyInternal` to `true` (as shown below) in your configuration file to instruct the CLI to copy these images into your vault. This will make your vault larger, but you will not need to be online to view the images.

    ```json
    "images": {
        "copyInternal": true,
    }
    ```

    With just this setting, the CLI will download each "internal" image it hasn't seen before into your compendium. It is a lot of individual requests. If you have a slow connection, the next option may be better.

- Create a shallow clone of the images repository, and set `images.internalRoot` in your configuration file to tell the CLI where it can locally find "internal" images.

    ```json
    "images": {
        "copyInternal": true,
        "internalRoot": "5etools-img"
    }
    ```

    With this setting, the CLI will look for "internal" images in the local directory, which will speed things up (at the cost of a few extra steps).

    If you use a relative path, it will be resolved relative to the current working directory[^1]. An absolute path[^2] will also work. You will get an error message if that directory doesn't exist (and it will tell you the directory it tried to use, which should help you figure out where the problem is).

### Copying external images

External images are referenced by media references marked as "external", and usually begin with "http://" or "https://". Some homebrew content may use a "file://" URL[^3] to reference a local file.

**By default, external images are not downloaded.** Links will reference the remote location, and you will need to be online to view the images. This is a safe, fast, and relatively well-behaving option given that downloads can be quite slow, and you may change your mind about where you want content to be generated.

To download remote images, set `images.copyExternal` to `true` (as shown below) in your configuration file to instruct the CLI to copy "external" images into your vault. This will make your vault larger, but you will not need to be online to view the images.

```json
"images": {
    "copyExternal": true,
}
```

With this setting, the CLI will copy all "external" images it hasn't seen before into your compendium.

### Fallback paths

🧪 This config has not been fully tested, so if it goes wrong, raise an issue so we can sort it out properly.

In the event you have a bad image reference and the copy fails, you can set a fallback path for an image that should be used instead.

```json
"images": {
    "fallbackPaths": {
        "img/bestiary/MM/Green Hag.jpg": "img/bestiary/MM/Green Hag.webp"
    },
}
```

The hard part will be knowing what the original lookup path was. For "external" and homebrew images, you can usually find the broken image reference in the json source material. Missing internal images may be a bit harder to track down.

Note:

- The key (original path) must match what the Json source is specifying.
- The value (replacement path) should be either: a valid path to a local file[^2] or a valid URL to a remote file[^3].

## Customizing the default source

> [!WARNING]
> 🔥 You can truly make a mess with this setting.
> Change these values with care, and inspect the result carefully in a test vault.
>
> - This will change generated file names. It will break links.
> - If you have content generated with a different defaultSource configuration,
>   completely remove it before copying freshly generated content into your vault ^[You could also use something like rsync that will remove extraneous files].

If you're only running 5e 2024 content (as an example), you may want to change the "default" source for items, etc. to be XPHB or XDMG or XMM, such that those files do not have the additional notations (e.g. the file name suffix, or the additional source designation in the monster name).

Change the default source for an index type in the `sources` block:

```json
"sources": {
    "defaultSource": {
        "monster": "XMM"
    }
}
```

Create a map of a content type to the default source. In general, this is the same key you would use to assign a template. If you open up the [generated index](#source-identifiers), the first segment of a key is its type, for example, `trap|collapsing roof|dmg` is a `trap`. Some types are grouped because of tight inter-relationships, like cards and decks.

| Emitted type          | Default Source | Includes (note) |
|-----------------------|----------------|----------|
| background            | PHB | |
| classtype             | PHB | subclass, class feature, subclass feature |
| deck                  | DMG | card |
| deity                 | PHB | |
| disease               | DMG | |
| facility              | XDMG | (bastion) |
| feat                  | PHB | |
| item                  | DMG | item group, magic variant |
| monster               | MM | legendary group (bestiary) |
| object                | DMG | |
| optfeature            | PHB | |
| psionic               | UATheMysticClass | |
| race                  | PHB | subrace (species) |
| reward                | DMG | |
| spell                 | PHB | |
| table                 | DMG | table group |
| trap                  | DMG | hazard |
| variantrule           | DMG | |
| vehicle               | GoS | |

## Migrating `from`, `full-source`, and `convert`

Older configurations looked a little different. Updating to the new format should be straightforward.

For comparison, the following examples of older configurations use the same values as the [example above](#specify-content-with-sources).

```json
"from": [
    "MPMM"
],
"full-source": {
    "adventure": [
        "WBtW"
    ],
    "book": [
        "PHB"
    ],
    "homebrew": [
        ...
    ]
}
```

OR

```json
"from": [
    "MPMM"
],
"convert": {
    "adventure": [
        "WBtW"
    ],
    "book": [
        "PHB"
    ],
    "homebrew": [
        ...
    ]
}
```

[^1]: The working directory is the directory you were in (in the terminal) when you launched the CLI. See <https://en.wikipedia.org/wiki/Working_directory> for more information
[^2]: Example/explanation of absolute vs. relative path: <https://stackoverflow.com/a/10288252>. If you're using relative paths with the CLI, they should be relative to the working directory (see [^1]).
[^3]: A URL is a uniform resource locator, more information at <https://en.wikipedia.org/wiki/URL>.
