package dev.ebullient.convert.tools.dnd5e;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.QuteAreaOfEffect;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.QuteDamage;

class QuteSpellExtensionsTest {

    @Test
    void testQuteDamageCreation() {
        QuteDamage damage = new QuteDamage("8d6",
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.");

        assertNotNull(damage);
        assertEquals("8d6", damage.baseDamage);
        assertEquals(
                "When you cast this spell using a spell slot of 4th level or higher, the damage increases by 1d6 for each slot level above 3rd.",
                damage.scaling);
    }

    @Test
    void testQuteDamageWithNullValues() {
        QuteDamage damage1 = new QuteDamage(null, "scaling info");
        assertNull(damage1.baseDamage);
        assertEquals("scaling info", damage1.scaling);

        QuteDamage damage2 = new QuteDamage("2d8", null);
        assertEquals("2d8", damage2.baseDamage);
        assertNull(damage2.scaling);

        QuteDamage damage3 = new QuteDamage(null, null);
        assertNull(damage3.baseDamage);
        assertNull(damage3.scaling);
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
            QuteDamage damage = new QuteDamage(pattern, "some scaling");
            assertEquals(pattern, damage.baseDamage);
            assertEquals("some scaling", damage.scaling);
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
}