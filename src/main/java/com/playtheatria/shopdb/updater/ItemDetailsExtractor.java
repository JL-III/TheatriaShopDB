package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.ItemDetailsDto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts cosmetic metadata from the ItemStack a shop trades (resolved via
 * ChestShop's ItemParseEvent). Names and lore keep their formatting as legacy
 * §-strings (hex colors in the §x§r§r§g§g§b§b form) so the website can render
 * them faithfully. The display enchantment list omits hidden enchants. A
 * separate search list includes visible enchants and hidden Titan enchants
 * whose plain lore independently confirms their names and levels.
 */
public final class ItemDetailsExtractor {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern ARABIC_LEVEL = Pattern.compile("[1-9]\\d*");
    private static final Pattern ROMAN_LEVEL = Pattern.compile("[ivxlcdm]+");
    private static final String TITAN_SIGNATURE = "ancient power ω";
    private static final Set<String> TITAN_NON_ENCHANTMENT_LINES = Set.of("unbreakable");

    /*
     * TitanTools writes functional enchantments into lore instead of Bukkit's
     * enchantment map. Keep this list limited to real vanilla enchantment names
     * so ordinary lore near a copied Titan signature cannot invent searchable
     * enchantments. Levels are intentionally not capped at vanilla maxima:
     * Titan tools use levels such as Efficiency X.
     */
    private static final List<LoreEnchantment> LORE_ENCHANTMENTS = List.of(
            loreEnchant("bane_of_arthropods", "bane of arthropods"),
            loreEnchant("projectile_protection", "projectile protection"),
            loreEnchant("binding_curse", "curse of binding", "binding curse"),
            loreEnchant("vanishing_curse", "curse of vanishing", "vanishing curse"),
            loreEnchant("fire_protection", "fire protection"),
            loreEnchant("blast_protection", "blast protection"),
            loreEnchant("aqua_affinity", "aqua affinity"),
            loreEnchant("depth_strider", "depth strider"),
            loreEnchant("feather_falling", "feather falling"),
            loreEnchant("frost_walker", "frost walker"),
            loreEnchant("luck_of_the_sea", "luck of the sea"),
            loreEnchant("quick_charge", "quick charge"),
            loreEnchant("sweeping_edge", "sweeping edge", "sweeping"),
            loreEnchant("swift_sneak", "swift sneak"),
            loreEnchant("soul_speed", "soul speed"),
            loreEnchant("fire_aspect", "fire aspect"),
            loreEnchant("silk_touch", "silk touch"),
            loreEnchant("wind_burst", "wind burst"),
            loreEnchant("channeling", "channeling"),
            loreEnchant("efficiency", "efficiency"),
            loreEnchant("impaling", "impaling"),
            loreEnchant("infinity", "infinity"),
            loreEnchant("knockback", "knockback"),
            loreEnchant("looting", "looting"),
            loreEnchant("loyalty", "loyalty"),
            loreEnchant("multishot", "multishot"),
            loreEnchant("piercing", "piercing"),
            loreEnchant("protection", "protection"),
            loreEnchant("respiration", "respiration"),
            loreEnchant("riptide", "riptide"),
            loreEnchant("sharpness", "sharpness"),
            loreEnchant("unbreaking", "unbreaking"),
            loreEnchant("breach", "breach"),
            loreEnchant("density", "density"),
            loreEnchant("fortune", "fortune"),
            loreEnchant("mending", "mending"),
            loreEnchant("power", "power"),
            loreEnchant("punch", "punch"),
            loreEnchant("smite", "smite"),
            loreEnchant("thorns", "thorns"),
            loreEnchant("flame", "flame"),
            loreEnchant("lure", "lure")
    );

    /** Lower-cased Bukkit material name, or null when the item couldn't be resolved. */
    public static String baseMaterial(ItemStack item) {
        if (item == null) return null;
        Material type = item.getType();
        return type == null ? null : type.name().toLowerCase(Locale.ROOT);
    }

    /** Cosmetic details, or null when the item has none worth showing. */
    public static ItemDetailsDto details(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        ItemDetailsDto details = new ItemDetailsDto();

        if (meta.hasDisplayName()) {
            details.setDisplayName(serialize(meta.displayName()));
        }

        List<String> plainLore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            List<String> lore = new ArrayList<>();
            for (Component line : meta.lore()) {
                lore.add(serialize(line));
                plainLore.add(plain(line));
            }
            if (!lore.isEmpty()) details.setLore(lore);
        }

        List<ItemDetailsDto.EnchantmentDto> enchants = new ArrayList<>();
        List<ItemDetailsDto.EnchantmentDto> searchEnchants = new ArrayList<>();
        if (meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            addApprovedHiddenEnchants(searchEnchants, meta.getEnchants(), plainLore);
        } else {
            addEnchants(enchants, meta.getEnchants());
            addEnchants(searchEnchants, meta.getEnchants());
        }
        // Stored enchants (enchanted books). 1.20.5+ clients hide them via
        // HIDE_ADDITIONAL_TOOLTIP; older items may carry HIDE_STORED_ENCHANTS.
        if (meta instanceof EnchantmentStorageMeta
                && !meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS)
                && !meta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
            addEnchants(enchants, ((EnchantmentStorageMeta) meta).getStoredEnchants());
            addEnchants(searchEnchants, ((EnchantmentStorageMeta) meta).getStoredEnchants());
        }
        if (!enchants.isEmpty()) details.setEnchants(enchants);
        if (!searchEnchants.isEmpty()) details.setSearchEnchants(searchEnchants);

        return details.isEmpty() ? null : details;
    }

    private static void addApprovedHiddenEnchants(List<ItemDetailsDto.EnchantmentDto> out,
                                                   Map<Enchantment, Integer> enchants,
                                                   List<String> plainLore) {
        if (!hasTitanSignature(plainLore)) return;

        for (ItemDetailsDto.EnchantmentDto enchant : titanLoreEnchantments(plainLore)) {
            addUnique(out, enchant);
        }

        // Preserve support for registered/custom enchants when the lore names
        // the exact hidden enchant and level. A hidden cosmetic glint enchant
        // is still excluded because it has no matching enchantment lore line.
        if (enchants != null) {
            for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
                String name = enchant.getKey().getKey().getKey().toLowerCase(Locale.ROOT);
                int level = enchant.getValue();
                if (hasMatchingEnchantLore(plainLore, name, level)) {
                    addUnique(out, new ItemDetailsDto.EnchantmentDto(name, level));
                }
            }
        }
    }

    static List<ItemDetailsDto.EnchantmentDto> titanLoreEnchantments(List<String> plainLore) {
        int signatureIndex = titanSignatureIndex(plainLore);
        if (signatureIndex < 0) return List.of();

        List<ItemDetailsDto.EnchantmentDto> reversed = new ArrayList<>();
        for (int i = signatureIndex - 1; i >= 0; i--) {
            String line = normalizeWords(plainLore.get(i));
            if (line.isEmpty()) continue;
            if (TITAN_NON_ENCHANTMENT_LINES.contains(line)) continue;

            ItemDetailsDto.EnchantmentDto enchant = parseLoreEnchantment(line);
            if (enchant == null) break;
            addUnique(reversed, enchant);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    static boolean isApprovedHiddenEnchantment(List<String> plainLore, String enchantmentName, int level) {
        return hasTitanSignature(plainLore) && hasMatchingEnchantLore(plainLore, enchantmentName, level);
    }

    static boolean hasTitanSignature(List<String> plainLore) {
        return titanSignatureIndex(plainLore) >= 0;
    }

    static boolean hasMatchingEnchantLore(List<String> plainLore, String enchantmentName, int level) {
        if (plainLore == null || enchantmentName == null || level < 1) return false;

        int namespaceSeparator = enchantmentName.lastIndexOf(':');
        String unnamespacedName = namespaceSeparator >= 0
                ? enchantmentName.substring(namespaceSeparator + 1)
                : enchantmentName;
        String normalizedName = normalizeWords(unnamespacedName);
        if (normalizedName.isEmpty()) return false;

        String arabic = normalizedName + " " + level;
        String roman = normalizedName + " " + toRoman(level).toLowerCase(Locale.ROOT);
        for (String line : plainLore) {
            String normalizedLine = normalizeWords(line);
            if ((level == 1 && normalizedLine.equals(normalizedName))
                    || normalizedLine.equals(arabic)
                    || normalizedLine.equals(roman)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeWords(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").trim();
        return WHITESPACE.matcher(normalized).replaceAll(" ");
    }

    private static int titanSignatureIndex(List<String> plainLore) {
        if (plainLore == null) return -1;
        for (int i = 0; i < plainLore.size(); i++) {
            if (TITAN_SIGNATURE.equals(normalizeWords(plainLore.get(i)))) return i;
        }
        return -1;
    }

    private static ItemDetailsDto.EnchantmentDto parseLoreEnchantment(String normalizedLine) {
        for (LoreEnchantment enchantment : LORE_ENCHANTMENTS) {
            for (String displayName : enchantment.displayNames()) {
                if (normalizedLine.equals(displayName)) {
                    return new ItemDetailsDto.EnchantmentDto(enchantment.key(), 1);
                }
                if (!normalizedLine.startsWith(displayName + " ")) continue;

                int level = parseLevel(normalizedLine.substring(displayName.length() + 1));
                if (level > 0) {
                    return new ItemDetailsDto.EnchantmentDto(enchantment.key(), level);
                }
            }
        }
        return null;
    }

    private static int parseLevel(String value) {
        if (ARABIC_LEVEL.matcher(value).matches()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        if (!ROMAN_LEVEL.matcher(value).matches()) return -1;

        int total = 0;
        int previous = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            int current = romanValue(value.charAt(i));
            total += current < previous ? -current : current;
            previous = current;
        }
        return total > 0 && toRoman(total).equalsIgnoreCase(value) ? total : -1;
    }

    private static int romanValue(char numeral) {
        return switch (numeral) {
            case 'i' -> 1;
            case 'v' -> 5;
            case 'x' -> 10;
            case 'l' -> 50;
            case 'c' -> 100;
            case 'd' -> 500;
            case 'm' -> 1000;
            default -> 0;
        };
    }

    private static LoreEnchantment loreEnchant(String key, String... displayNames) {
        return new LoreEnchantment(key, List.of(displayNames));
    }

    private static void addUnique(List<ItemDetailsDto.EnchantmentDto> out,
                                  ItemDetailsDto.EnchantmentDto enchantment) {
        if (!out.contains(enchantment)) out.add(enchantment);
    }

    private static String toRoman(int value) {
        if (value < 1 || value > 3999) return "";
        int remaining = value;
        StringBuilder result = new StringBuilder();
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                result.append(numerals[i]);
                remaining -= values[i];
            }
        }
        return result.toString();
    }

    private static void addEnchants(List<ItemDetailsDto.EnchantmentDto> out, Map<Enchantment, Integer> enchants) {
        if (enchants == null) return;
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            out.add(new ItemDetailsDto.EnchantmentDto(
                    e.getKey().getKey().getKey().toLowerCase(Locale.ROOT), e.getValue()));
        }
    }

    private static String serialize(Component component) {
        return component == null ? null : LEGACY.serialize(component);
    }

    private static String plain(Component component) {
        return component == null ? null : PLAIN.serialize(component);
    }

    private record LoreEnchantment(String key, List<String> displayNames) {
    }

    private ItemDetailsExtractor() {
    }
}
