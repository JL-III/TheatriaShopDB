import reducer, {
  fetchRegionNames,
  fetchRegions,
  setName,
  setPage,
  setType,
} from './regionsSlice';
import { MARKET_STALL, PLAYER_SHOP } from '../shopLocationTypes';

const stateWithType = (type) => ({
  regions: {
    options: {
      type,
      server: 'all',
      name: undefined,
      page: 1,
      sortBy: { value: 'name', label: 'Name' },
    },
  },
});

afterEach(() => {
  delete global.fetch;
});

test.each([
  ['region list', fetchRegions, '/regions'],
  ['region names', fetchRegionNames, '/regions/region-names'],
])('%s requests include the selected shop location type', (label, thunk, path) => {
  global.fetch = jest.fn(() => new Promise(() => {}));

  thunk()(jest.fn(), () => stateWithType(PLAYER_SHOP));

  const url = global.fetch.mock.calls[0][0];
  expect(url.pathname.endsWith(path)).toBe(true);
  expect(url.searchParams.get('type')).toBe(PLAYER_SHOP);
});

test('changing location type resets paging and the old name filter', () => {
  let state = reducer(undefined, { type: 'test/init' });
  state = reducer(state, setPage(4));
  state = reducer(state, setName({ value: 'shop4', label: 'shop4' }));
  state = reducer(state, setType(PLAYER_SHOP));

  expect(state.options.type).toBe(PLAYER_SHOP);
  expect(state.options.page).toBe(1);
  expect(state.options.name).toBeUndefined();

  state = reducer(state, setType('not-a-real-type'));
  expect(state.options.type).toBe(MARKET_STALL);
});
