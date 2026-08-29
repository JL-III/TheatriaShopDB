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

const ROMAN_NUMERALS = [
  [1000, 'M'],
  [900, 'CM'],
  [500, 'D'],
  [400, 'CD'],
  [100, 'C'],
  [90, 'XC'],
  [50, 'L'],
  [40, 'XL'],
  [10, 'X'],
  [9, 'IX'],
  [5, 'V'],
  [4, 'IV'],
  [1, 'I'],
];

export const romanLevel = (level) => {
  let remaining = Number(level);
  if (!Number.isInteger(remaining) || remaining < 1) return String(level);

  return ROMAN_NUMERALS.reduce((roman, [value, numeral]) => {
    while (remaining >= value) {
      roman += numeral;
      remaining -= value;
    }
    return roman;
  }, '');
};

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
