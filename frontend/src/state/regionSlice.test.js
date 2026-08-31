import {
  fetchRegion,
  fetchRegionChestShops,
  fetchRegionPlayers,
} from './regionSlice';
import { PLAYER_SHOP } from '../shopLocationTypes';

afterEach(() => {
  delete global.fetch;
});

test.each([
  ['detail', fetchRegion('Moon Base', 'The_Ark', PLAYER_SHOP)],
  ['owners', fetchRegionPlayers('Moon Base', 'The_Ark', PLAYER_SHOP)],
  [
    'chest shops',
    fetchRegionChestShops('Moon Base', 'The_Ark', PLAYER_SHOP, 'buy'),
  ],
])('%s requests identify the player-shop location type', (label, request) => {
  global.fetch = jest.fn(() => new Promise(() => {}));
  const getState = () => ({
    region: {
      players: { page: 1 },
      chestShops: { page: 1 },
    },
  });

  request(jest.fn(), getState);

  const url = global.fetch.mock.calls[0][0];
  expect(url.searchParams.get('type')).toBe(PLAYER_SHOP);
  expect(url.pathname).toContain('/regions/The_Ark/Moon%20Base');
});
