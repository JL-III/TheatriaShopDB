export const MARKET_STALL = 'MARKET_STALL';
export const PLAYER_SHOP = 'PLAYER_SHOP';

export const isShopLocationType = (type) =>
  type === MARKET_STALL || type === PLAYER_SHOP;

export const normalizeShopLocationType = (type) =>
  type === PLAYER_SHOP ? PLAYER_SHOP : MARKET_STALL;

export const shopLocationTypeOf = (location) =>
  normalizeShopLocationType(location && location.type);

export const shopLocationLabel = (type, plural = true) => {
  if (normalizeShopLocationType(type) === PLAYER_SHOP) {
    return plural ? 'Player Shops' : 'Player Shop';
  }
  return plural ? 'Market Stalls' : 'Market Stall';
};

export const shopLocationListPath = (type) =>
  `/search/regions?type=${normalizeShopLocationType(type)}`;

export const shopLocationPath = (type, server, name) =>
  `/search/regions/${normalizeShopLocationType(type)}/${encodeURIComponent(
    server
  )}/${encodeURIComponent(name)}`;

export const travelCommandFor = (type, name, travelCommand) => {
  if (travelCommand) return travelCommand;
  return normalizeShopLocationType(type) === PLAYER_SHOP
    ? `/lands spawn ${name.replace(/\s+/g, '_')}`
    : `/warp ${name}`;
};

export const travelButtonTextFor = (type) =>
  normalizeShopLocationType(type) === PLAYER_SHOP
    ? 'Copy Land Spawn'
    : 'Copy Warp';
