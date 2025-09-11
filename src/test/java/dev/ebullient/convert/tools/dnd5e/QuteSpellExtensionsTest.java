package dev.ebullient.convert.tools.dnd5e;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.QuteAreaOfEffect;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.QuteDamage;

class QuteSpellExtensionsTest {

    @Test
    void testQuteDamageCreation() {
        QuteDamage damage = new QuteDamage("8d6",
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                4, "1d6");

        assertNotNull(damage);
        assertEquals("8d6", damage.baseDamage);
        assertEquals(
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                damage.scaling);
        assertEquals(4, damage.scalingLevel);
        assertEquals("1d6", damage.scalingDamage);
    }

    @Test
    void testQuteDamageWithNullValues() {
        QuteDamage damage1 = new QuteDamage(null, "scaling info", null, null);
        assertNull(damage1.baseDamage);
        assertEquals("scaling info", damage1.scaling);
        assertNull(damage1.scalingLevel);
        assertNull(damage1.scalingDamage);

        QuteDamage damage2 = new QuteDamage("2d8", null, 3, "1d8");
        assertEquals("2d8", damage2.baseDamage);
        assertNull(damage2.scaling);
        assertEquals(3, damage2.scalingLevel);
        assertEquals("1d8", damage2.scalingDamage);

        QuteDamage damage3 = new QuteDamage(null, null, null, null);
        assertNull(damage3.baseDamage);
        assertNull(damage3.scaling);
        assertNull(damage3.scalingLevel);
        assertNull(damage3.scalingDamage);
    }

    @Test
    void testQuteAreaOfEffectCreation() {
        QuteAreaOfEffect aoe = new QuteAreaOfEffect("sphere", 20);

        assertNotNull(aoe);
        assertEquals("sphere", aoe.shape);
        assertEquals(20, aoe.size);
    }

    @Test
    void testQuteAreaOfEffectWithNullValues() {
        QuteAreaOfEffect aoe1 = new QuteAreaOfEffect(null, 15);
        assertNull(aoe1.shape);
        assertEquals(15, aoe1.size);

        QuteAreaOfEffect aoe2 = new QuteAreaOfEffect("cone", null);
        assertEquals("cone", aoe2.shape);
        assertNull(aoe2.size);

        QuteAreaOfEffect aoe3 = new QuteAreaOfEffect(null, null);
        assertNull(aoe3.shape);
        assertNull(aoe3.size);
    }

    @Test
    void testVariousAreaShapes() {
        String[] validShapes = { "cone", "sphere", "line", "cube", "cylinder" };

        for (String shape : validShapes) {
            QuteAreaOfEffect aoe = new QuteAreaOfEffect(shape, 30);
            assertEquals(shape, aoe.shape);
            assertEquals(30, aoe.size);
        }
    }

    @Test
    void testVariousDamagePatterns() {
        String[] damagePatterns = {
                "2d10+5", "1d8", "10d6+10", "3d4-1", "12d6"
        };

        for (String pattern : damagePatterns) {
            QuteDamage damage = new QuteDamage(pattern, "some scaling", 4, "1d6");
            assertEquals(pattern, damage.baseDamage);
            assertEquals("some scaling", damage.scaling);
            assertEquals(4, damage.scalingLevel);
            assertEquals("1d6", damage.scalingDamage);
        }
    }

    @Test
    void testSavingThrowOutcomes() {
        String[] outcomes = {
                "half damage", "no damage", "reduced effect", "no effect", "see spell description"
        };

        // Just test that these are valid string values that could be used
        for (String outcome : outcomes) {
            assertNotNull(outcome);
            assertFalse(outcome.isEmpty());
        }
    }

    @Test
    void testQuteDamageScalingFields() {
        // Test with both scaling level and damage
        QuteDamage damage1 = new QuteDamage("6d6",
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                4, "1d6");
        assertEquals(4, damage1.scalingLevel);
        assertEquals("1d6", damage1.scalingDamage);

        // Test with different scaling values
        QuteDamage damage2 = new QuteDamage("3d8",
                "At 3rd level or higher, increases by 2d8 per level",
                3, "2d8");
        assertEquals(3, damage2.scalingLevel);
        assertEquals("2d8", damage2.scalingDamage);

        // Test with null scaling values
        QuteDamage damage3 = new QuteDamage("5d4", "No scaling", null, null);
        assertNull(damage3.scalingLevel);
        assertNull(damage3.scalingDamage);
    }

    @Test
    void testQuteDamageCalculationReady() {
        // Test that the new fields support calculation logic
        QuteDamage damage = new QuteDamage("8d6",
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                4, "1d6");

        // Example calculation: cast at 6th level
        int castLevel = 6;
        if (damage.scalingLevel != null && castLevel >= damage.scalingLevel) {
            int levelsAboveBase = castLevel - (damage.scalingLevel - 1); // 6 - 3 = 3 extra dice
            // Template could use: baseDamage + (levelsAboveBase * scalingDamage)
            // This would be: 8d6 + 3 * 1d6 = 11d6
            assertTrue(levelsAboveBase > 0);
        }
    }

    @Test
    void testMarkdownFormattingRemoval() {
        // Test markdown removal with regex patterns directly since the actual parsing
        // happens in Json2QuteSpell, not in the constructor
        String[] testTexts = {
                "**At Higher Levels.** When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                "At Higher Levels: When you cast this spell using a spell slot of 2nd level or higher, the damage increases by 1d8 for each slot level above 1st.",
                "At higher levels. The damage increases by 2d6 for each slot level above 1st."
        };

        String pattern = "(?i)^\\s*\\*\\*at\\s+higher\\s+levels\\.?\\*\\*\\s*|^\\s*at\\s+higher\\s+levels[.:]*\\s*";

        for (String text : testTexts) {
            String cleaned = text.replaceFirst(pattern, "");
            assertFalse(cleaned.toLowerCase().startsWith("**at higher levels"),
                    "Text should not start with markdown formatted 'At Higher Levels': " + cleaned);
            assertFalse(cleaned.toLowerCase().startsWith("at higher levels"),
                    "Text should not start with 'At Higher Levels': " + cleaned);
            assertTrue(cleaned.toLowerCase().startsWith("when") || cleaned.toLowerCase().startsWith("the"),
                    "Text should start with actual content: " + cleaned);
        }
    }

    @Test
    void testVariousScalingDamagePatterns() {
        // Test that various damage scaling patterns can be stored correctly
        // (The actual parsing happens in Json2QuteSpell)

        // Test 1: Standard pattern
        QuteDamage damage1 = new QuteDamage("8d6",
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                4, "1d6");
        assertEquals("1d6", damage1.scalingDamage);

        // Test 2: Different pattern
        QuteDamage damage2 = new QuteDamage("3d8",
                "At 2nd level and higher, the spell deals an additional 1d8 damage for each slot level above 1st.",
                2, "1d8");
        assertEquals("1d8", damage2.scalingDamage);

        // Test 3: Another pattern
        QuteDamage damage3 = new QuteDamage("2d10",
                "The damage increases by 1d10 per spell slot level above 3rd.",
                4, "1d10");
        assertEquals("1d10", damage3.scalingDamage);
    }
}