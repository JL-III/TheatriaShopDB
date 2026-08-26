// Text helpers for material and enchantment names.

// "cobbled_deepslate" -> "Cobbled Deepslate"
export const prettyMaterial = (name) =>
  name
    ? name
        .split('_')
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ')
    : '';

const ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];

export const romanLevel = (level) =>
  level >= 1 && level <= 10 ? ROMAN[level - 1] : String(level);

// Enchantments whose max level is 1 show no numeral, like the vanilla tooltip.
const SINGLE_LEVEL = new Set([
  'mending',
  'infinity',
  'silk_touch',
  'flame',
  'multishot',
  'channeling',
  'aqua_affinity',
  'binding_curse',
  'vanishing_curse',
]);

export const prettyEnchant = ({ name, level }) =>
  SINGLE_LEVEL.has(name) && level === 1
    ? prettyMaterial(name)
    : `${prettyMaterial(name)} ${romanLevel(level)}`;
