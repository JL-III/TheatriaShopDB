package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.ItemDetailsDto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extracts cosmetic metadata from the ItemStack a shop trades (resolved via
 * ChestShop's ItemParseEvent). Names and lore keep their formatting as legacy
 * §-strings (hex colors in the §x§r§r§g§g§b§b form) so the website can render
 * them faithfully. Enchantments are omitted when the item hides them.
 */
public final class ItemDetailsExtractor {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

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

        if (meta.hasLore() && meta.lore() != null) {
            List<String> lore = new ArrayList<>();
            for (Component line : meta.lore()) {
                lore.add(serialize(line));
            }
            if (!lore.isEmpty()) details.setLore(lore);
        }

        List<ItemDetailsDto.EnchantmentDto> enchants = new ArrayList<>();
        if (!meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            addEnchants(enchants, meta.getEnchants());
        }
        // Stored enchants (enchanted books). 1.20.5+ clients hide them via
        // HIDE_ADDITIONAL_TOOLTIP; older items may carry HIDE_STORED_ENCHANTS.
        if (meta instanceof EnchantmentStorageMeta
                && !meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS)
                && !meta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
            addEnchants(enchants, ((EnchantmentStorageMeta) meta).getStoredEnchants());
        }
        if (!enchants.isEmpty()) details.setEnchants(enchants);

        return details.isEmpty() ? null : details;
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

    private ItemDetailsExtractor() {
    }
}
