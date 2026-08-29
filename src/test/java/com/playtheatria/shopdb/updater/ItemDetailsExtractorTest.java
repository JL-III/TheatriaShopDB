package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.ItemDetailsDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDetailsExtractorTest {
    private static final String TITAN_SIGNATURE = "Ancient Power Ω";

    @Test
    void approvesTitanEnchantWhenRomanLoreMatchesActualLevel() {
        assertTrue(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Efficiency VII", TITAN_SIGNATURE), "efficiency", 7));
    }

    @Test
    void extractsLoreOnlyTitanEnchantmentsAtCustomLevels() {
        assertEquals(List.of(
                        new ItemDetailsDto.EnchantmentDto("efficiency", 10),
                        new ItemDetailsDto.EnchantmentDto("silk_touch", 1)),
                ItemDetailsExtractor.titanLoreEnchantments(List.of(
                        "Efficiency X",
                        "Silk Touch",
                        "Unbreakable",
                        TITAN_SIGNATURE,
                        "• Charge 0",
                        "• Status OFF")));
    }

    @Test
    void rejectsNonEnchantPropertiesAndLoreWithoutExactTitanSignature() {
        assertTrue(ItemDetailsExtractor.titanLoreEnchantments(List.of(
                "Efficiency X", "Unbreakable", "Decorative glow")).isEmpty());
        assertTrue(ItemDetailsExtractor.titanLoreEnchantments(List.of(
                "Efficiency X", "Unbreakable", "Replica Ancient Power Ω")).isEmpty());
        assertFalse(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Efficiency X", "Unbreakable", TITAN_SIGNATURE), "unbreaking", 1));
    }

    @Test
    void onlyReadsTheRecognizedEnchantBlockAdjacentToTheSignature() {
        assertEquals(List.of(new ItemDetailsDto.EnchantmentDto("silk_touch", 1)),
                ItemDetailsExtractor.titanLoreEnchantments(List.of(
                        "Efficiency X",
                        "A decorative inscription",
                        "Silk Touch",
                        "Unbreakable",
                        TITAN_SIGNATURE)));
    }

    @Test
    void approvesArabicLevelsAndNormalizesNamespacedUnderscoreNames() {
        assertTrue(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Luck of the Sea 5", TITAN_SIGNATURE), "minecraft:luck_of_the_sea", 5));
    }

    @Test
    void approvesAnOmittedNumeralOnlyForLevelOne() {
        assertTrue(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Silk Touch", TITAN_SIGNATURE), "silk_touch", 1));
        assertFalse(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Efficiency", TITAN_SIGNATURE), "efficiency", 7));
    }

    @Test
    void rejectsMissingSignatureAndMismatchedLoreLevels() {
        assertFalse(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Efficiency VII"), "efficiency", 7));
        assertFalse(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Efficiency VI", TITAN_SIGNATURE), "efficiency", 7));
    }

    @Test
    void requiresTheTitanWordsAndSymbolOnTheSameLoreLine() {
        assertFalse(ItemDetailsExtractor.isApprovedHiddenEnchantment(
                List.of("Efficiency VII", "Ancient Power", "Ω"), "efficiency", 7));
    }

    @Test
    void searchableOnlyEnchantmentsKeepTheDtoNonEmpty() {
        ItemDetailsDto details = new ItemDetailsDto();
        details.setSearchEnchants(List.of(new ItemDetailsDto.EnchantmentDto("efficiency", 7)));

        assertFalse(details.isEmpty());
    }
}
