package com.playtheatria.shopdb.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopInfoTextBuilder {
    private static final int APPROXIMATE_CHARACTERS_PER_WRAPPED_LINE = 32;
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static Component build(ItemStack item, boolean adminShop, Integer stockCount,
                                  Integer quantity, boolean hasBuyPrice, boolean showShopFull) {
        List<Component> lines = new ArrayList<>();
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        Component realName = Component.translatable(item.getType());
        Component displayName = meta != null && meta.hasDisplayName() ? meta.displayName() : null;
        lines.addAll(identityLines(displayName, realName));

        if (meta != null) {
            addEnchantLines(lines, meta);
            addLoreLines(lines, meta);
        }
        lines.addAll(stockLines(adminShop, stockCount, quantity, hasBuyPrice, showShopFull));

        return Component.join(Component.newline(), lines);
    }

    static List<Component> identityLines(Component displayName, Component realName) {
        if (displayName == null) {
            return List.of(realName.color(NamedTextColor.WHITE));
        }

        Component itemType = Component.text("(item:", NamedTextColor.GRAY)
                .append(realName.colorIfAbsent(NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.GRAY));
        return List.of(displayName, itemType);
    }

    static int estimatedRenderedLineCount(Component component) {
        String[] explicitLines = PLAIN_TEXT.serialize(component).split("\\n", -1);
        int renderedLines = 0;
        for (String line : explicitLines) {
            int characterCount = line.codePointCount(0, line.length());
            renderedLines += Math.max(1,
                    (characterCount + APPROXIMATE_CHARACTERS_PER_WRAPPED_LINE - 1)
                            / APPROXIMATE_CHARACTERS_PER_WRAPPED_LINE);
        }
        return renderedLines;
    }

    static List<Component> stockLines(boolean adminShop, Integer stockCount, Integer quantity,
                                      boolean hasBuyPrice, boolean showShopFull) {
        List<Component> lines = new ArrayList<>();

        if (adminShop) {
            lines.add(Component.text("Always in stock", NamedTextColor.GREEN));
            return lines;
        }

        if (stockCount == null) {
            return lines;
        }

        if (hasBuyPrice && quantity != null) {
            if (stockCount >= quantity) {
                lines.add(Component.text("In stock", NamedTextColor.GREEN)
                        .append(Component.text(" (" + stockCount + ")", NamedTextColor.GRAY)));
            } else {
                lines.add(Component.text("Out of stock", NamedTextColor.RED));
            }
        }

        if (showShopFull) {
            lines.add(Component.text("Shop full", NamedTextColor.RED));
        }

        return lines;
    }

    private static void addEnchantLines(List<Component> lines, ItemMeta meta) {
        if (!meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            addEnchantLines(lines, meta.getEnchants());
        }

        if (meta instanceof EnchantmentStorageMeta storageMeta
                && !meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS)
                && !meta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
            addEnchantLines(lines, storageMeta.getStoredEnchants());
        }
    }

    private static void addEnchantLines(List<Component> lines, Map<Enchantment, Integer> enchants) {
        for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
            lines.add(enchant.getKey().displayName(enchant.getValue())
                    .colorIfAbsent(NamedTextColor.GRAY));
        }
    }

    private static void addLoreLines(List<Component> lines, ItemMeta meta) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            return;
        }

        lines.addAll(styledLoreLines(lore));
    }

    static List<Component> styledLoreLines(List<Component> lore) {
        List<Component> styled = new ArrayList<>(lore.size());
        for (Component line : lore) {
            styled.add(line
                    .colorIfAbsent(NamedTextColor.LIGHT_PURPLE)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.TRUE));
        }
        return styled;
    }

    private ShopInfoTextBuilder() {
    }
}
