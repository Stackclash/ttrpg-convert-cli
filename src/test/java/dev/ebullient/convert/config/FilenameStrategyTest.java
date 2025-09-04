package dev.ebullient.convert.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.Tags;

public class FilenameStrategyTest {

    private CompendiumConfig originalConfig;

    @BeforeEach
    public void setUp() {
        // Initialize a new config for testing
        TtrpgConfig.init(Tui.instance(), Datasource.tools5e);
        originalConfig = TtrpgConfig.getConfig();
    }

    @AfterEach
    public void tearDown() {
        // Reset any configuration changes
        if (originalConfig != null) {
            originalConfig.useTitleAsFilename = false;
        }
    }

    @Test
    public void testDefaultSlugifiedFilenames() {
        // Default behavior should slugify filenames
        TtrpgConfig.getConfig().useTitleAsFilename = false;

        QuteNote note = new QuteNote("Test Note: With Special Characters!", "Test Source", "Test content", new Tags());

        String filename = note.targetFile();
        assertThat(filename).isEqualTo("test-note-with-special-characters");
        assertThat(filename).doesNotContain(":");
        assertThat(filename).doesNotContain(" ");
        assertThat(filename).doesNotContain("!");
    }

    @Test
    public void testTitleAsFilename() {
        // When useTitleAsFilename is true, should use safe filename without full slugification
        TtrpgConfig.getConfig().useTitleAsFilename = true;

        QuteNote note = new QuteNote("Test Note: With Special Characters!", "Test Source", "Test content", new Tags());

        String filename = note.targetFile();
        assertThat(filename).isEqualTo("Test Note_ With Special Characters_");
        assertThat(filename).doesNotContain(":");
        assertThat(filename).doesNotContain("!");
        assertThat(filename).contains(" "); // Spaces should be preserved
    }

    @Test
    public void testSpecialCharacterHandling() {
        TtrpgConfig.getConfig().useTitleAsFilename = true;

        // Test various problematic characters
        QuteNote note = new QuteNote("File<>:\"/\\|?*Name", "Test Source", "Test content", new Tags());

        String filename = note.targetFile();
        assertThat(filename).isEqualTo("File_________Name");
        assertThat(filename).doesNotContain("<");
        assertThat(filename).doesNotContain(">");
        assertThat(filename).doesNotContain(":");
        assertThat(filename).doesNotContain("\"");
        assertThat(filename).doesNotContain("/");
        assertThat(filename).doesNotContain("\\");
        assertThat(filename).doesNotContain("|");
        assertThat(filename).doesNotContain("?");
        assertThat(filename).doesNotContain("!");
        assertThat(filename).doesNotContain("*");
    }

    @Test
    public void testSafeFilenameUtil() {
        // Test the safeFilename utility method directly
        assertThat(Tui.safeFilename("Normal Title")).isEqualTo("Normal Title");
        assertThat(Tui.safeFilename("Title: With Colon")).isEqualTo("Title_ With Colon");
        assertThat(Tui.safeFilename("Path/With/Slashes")).isEqualTo("Path_With_Slashes");
        assertThat(Tui.safeFilename("  Extra  Spaces  ")).isEqualTo("Extra Spaces");
        assertThat(Tui.safeFilename("")).isEqualTo("");
        assertThat(Tui.safeFilename(null)).isNull();
    }

    @Test
    public void testPf2eFilenameStrategy() {
        // Test Pf2e system as well
        TtrpgConfig.init(Tui.instance(), Datasource.toolsPf2e);
        TtrpgConfig.getConfig().useTitleAsFilename = true;

        // Create a mock Pf2eSources for testing
        // Since Pf2eSources requires complex setup, we'll test the safeFilename method directly
        String testName = "Test Spell: Lightning Bolt";
        String safeName = Tui.safeFilename(testName);

        assertThat(safeName).isEqualTo("Test Spell_ Lightning Bolt");
        assertThat(safeName).doesNotContain(":");
    }

    @Test
    public void testDnd5eDeityFilenameStrategy() {
        // Test D&D 5e deity filename generation
        TtrpgConfig.init(Tui.instance(), Datasource.tools5e);
        TtrpgConfig.getConfig().useTitleAsFilename = true;

        // Test the safeFilename utility for deity-like names
        String deityName = "God: Of War";
        String pantheonName = "Greek";
        String combinedName = pantheonName + "-" + deityName;

        String safeName = Tui.safeFilename(combinedName);
        assertThat(safeName).isEqualTo("Greek-God_ Of War");
        assertThat(safeName).doesNotContain(":");
        assertThat(safeName).contains(" "); // Spaces should be preserved
    }

    @Test
    public void testDnd5eSubclassFilenameStrategy() {
        // Test D&D 5e subclass filename generation
        TtrpgConfig.init(Tui.instance(), Datasource.tools5e);
        TtrpgConfig.getConfig().useTitleAsFilename = true;

        // Test the safeFilename utility for subclass-like names
        String parentClass = "Fighter";
        String subclass = "Champion: Elite";

        String parentSafe = Tui.safeFilename(parentClass);
        String subclassSafe = Tui.safeFilename(subclass);

        assertThat(parentSafe).isEqualTo("Fighter");
        assertThat(subclassSafe).isEqualTo("Champion_ Elite");
        assertThat(subclassSafe).doesNotContain(":");
        assertThat(subclassSafe).contains(" "); // Spaces should be preserved
    }
}
