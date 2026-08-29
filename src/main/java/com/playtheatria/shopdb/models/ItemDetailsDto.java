package com.playtheatria.shopdb.models;

import java.util.List;
import java.util.Objects;

/**
 * Cosmetic metadata of the item a shop trades, captured from the resolved
 * ItemStack: custom display name and lore as legacy §-formatted strings
 * (colors preserved for the website to render), display-visible enchantments,
 * and the enchantments that may be used for search. The complete object is
 * used on the updater wire and in the chest_shop_sign item_details JSON
 * column; search-only enchantments are stripped from player-facing responses.
 */
public class ItemDetailsDto {
    private String displayName;
    private List<String> lore;
    private List<EnchantmentDto> enchants;
    private List<EnchantmentDto> searchEnchants;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public List<EnchantmentDto> getEnchants() {
        return enchants;
    }

    public void setEnchants(List<EnchantmentDto> enchants) {
        this.enchants = enchants;
    }

    public List<EnchantmentDto> getSearchEnchants() {
        return searchEnchants;
    }

    public void setSearchEnchants(List<EnchantmentDto> searchEnchants) {
        this.searchEnchants = searchEnchants;
    }

    public boolean isEmpty() {
        return displayName == null
                && (lore == null || lore.isEmpty())
                && (enchants == null || enchants.isEmpty())
                && (searchEnchants == null || searchEnchants.isEmpty());
    }

    public static class EnchantmentDto {
        private String name; // namespaced key's value, e.g. "unbreaking"
        private int level;

        public EnchantmentDto() {
        }

        public EnchantmentDto(String name, int level) {
            this.name = name;
            this.level = level;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EnchantmentDto)) return false;
            EnchantmentDto other = (EnchantmentDto) o;
            return level == other.level && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, level);
        }
    }
}
