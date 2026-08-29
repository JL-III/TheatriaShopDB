import reducer, {
  ALL_ITEMS,
  SHOW_ALL_ENCHANTMENT_LEVELS,
  contextualEnchantmentLevels,
  createEnchantmentOption,
  deduplicateSearchOptions,
  erroredMaterials,
  fetchChestShops,
  fetchEnchantmentLevels,
  fetchMaterials,
  findExactEnchantmentOption,
  findExactSearchOption,
  isValidNewSearchOption,
  loaded,
  loadedEnchantmentLevels,
  loadedMaterials,
  loadingEnchantmentLevels,
  loadingMaterials,
  matchesSearchOption,
  setItemType,
  setEnchantmentLevel,
  setMaterial,
  setPage,
} from './chestShopsSlice';

const optionsWith = (overrides = {}) => ({
  tradeType: 'buy',
  server: 'all',
  hideOutOfStock: false,
  hideFull: false,
  hideDistinct: false,
  sortBy: { value: 'best-price', label: 'Best Price' },
  material: undefined,
  enchantmentLevel: undefined,
  itemType: ALL_ITEMS,
  page: 1,
  ...overrides,
});

const requestedUrlFor = (overrides = {}) => {
  global.fetch = jest.fn(() => new Promise(() => {}));
  fetchChestShops()(
    jest.fn(),
    () => ({ chestShops: { options: optionsWith(overrides) } })
  );
  return global.fetch.mock.calls[0][0];
};

const deferred = () => {
  let resolve;
  const promise = new Promise((done) => {
    resolve = done;
  });
  return { promise, resolve };
};

const responseWith = (body) => ({
  json: () => Promise.resolve(body),
});

afterEach(() => {
  delete global.fetch;
});

test('free-text item choices use the broad query parameter', () => {
  const url = requestedUrlFor({
    material: {
      value: 'efficiency wand',
      label: 'efficiency wand',
      kind: 'query',
    },
  });

  expect(url.searchParams.get('query')).toBe('efficiency wand');
  expect(url.searchParams.has('material')).toBe(false);
  expect(url.searchParams.has('name')).toBe(false);
  expect(url.searchParams.has('enchantment')).toBe(false);
});

test.each([
  ['material', 'blue wool'],
  ['name', 'Wool Wand'],
])('listed %s choices keep their exact filter', (kind, value) => {
  const url = requestedUrlFor({
    material: { value, label: value, kind },
  });

  expect(url.searchParams.get(kind)).toBe(value);
  expect(url.searchParams.has('query')).toBe(false);
});

test('an enchantment suggestion serializes its exact name and level', () => {
  const enchantment = createEnchantmentOption({
    name: 'efficiency',
    level: 7,
  });
  const url = requestedUrlFor({
    material: enchantment,
    enchantmentLevel: enchantment,
  });

  expect(url.searchParams.get('enchantment')).toBe('efficiency');
  expect(url.searchParams.get('enchantmentLevel')).toBe('7');
  expect(url.searchParams.has('query')).toBe(false);
  expect(url.searchParams.has('minEnchantmentLevel')).toBe(false);
});

test('show all keeps an enchantment name but omits the exact level', () => {
  const enchantment = createEnchantmentOption({
    name: 'efficiency',
    level: 10,
  });
  const url = requestedUrlFor({
    material: enchantment,
    enchantmentLevel: undefined,
  });

  expect(url.searchParams.get('enchantment')).toBe('efficiency');
  expect(url.searchParams.has('enchantmentLevel')).toBe(false);
});

test('a level facet augments a free query without replacing it', () => {
  const level = createEnchantmentOption({ name: 'soul_speed', level: 3 });
  const url = requestedUrlFor({
    material: { value: 'speed', label: 'speed', kind: 'query' },
    enchantmentLevel: level,
  });

  expect(url.searchParams.get('query')).toBe('speed');
  expect(url.searchParams.get('enchantment')).toBe('soul_speed');
  expect(url.searchParams.get('enchantmentLevel')).toBe('3');
});

test('only the books item type is serialized', () => {
  const books = requestedUrlFor({
    itemType: { value: 'books', label: 'All books' },
  });
  expect(books.searchParams.get('itemType')).toBe('books');

  const all = requestedUrlFor({ itemType: ALL_ITEMS });
  expect(all.searchParams.has('itemType')).toBe(false);
});

test('changing the item type resets the page', () => {
  let state = reducer(undefined, { type: 'test/init' });
  state = reducer(state, setPage(4));
  state = reducer(
    state,
    setItemType({ value: 'books', label: 'All books' })
  );

  expect(state.options.itemType.value).toBe('books');
  expect(state.options.page).toBe(1);
});

test('selecting and clearing a contextual enchantment level resets the page', () => {
  const level = createEnchantmentOption({ name: 'efficiency', level: 10 });
  let state = reducer(undefined, { type: 'test/init' });
  state = reducer(state, setPage(4));
  state = reducer(state, setEnchantmentLevel(level));

  expect(state.options.enchantmentLevel).toEqual(level);
  expect(state.options.page).toBe(1);

  state = reducer(state, setEnchantmentLevel(undefined));
  expect(state.options.enchantmentLevel).toBeUndefined();
});

test.each([
  ['efficiency 7', 7],
  ['Efficiency VII', 7],
  ['efficiency_7', 7],
  ['efficiency 13', 13],
  ['EFFICIENCY XIII', 13],
])('matches the typed enchantment alias %s', (input, level) => {
  const option = createEnchantmentOption({ name: 'efficiency', level });

  expect(matchesSearchOption(option, input)).toBe(true);
  expect(findExactEnchantmentOption([option], input)).toBe(option);
});

test('formats levels above ten as Roman numerals', () => {
  const option = createEnchantmentOption({ name: 'efficiency', level: 13 });

  expect(option.label).toBe('Efficiency XIII');
  expect(option.aliases).toEqual(
    expect.arrayContaining(['efficiency 13', 'efficiency xiii'])
  );
});

test('does not convert a partial enchantment search into an exact filter', () => {
  const option = createEnchantmentOption({ name: 'efficiency', level: 7 });

  expect(matchesSearchOption(option, 'eff 7')).toBe(false);
  expect(findExactEnchantmentOption([option], 'efficiency')).toBeUndefined();
});

test('does not offer a duplicate free-text row for an exact item option', () => {
  const item = {
    value: 'emerald#3m',
    label: 'emerald#3m',
    kind: 'material',
  };

  expect(findExactSearchOption([item], '  EMERALD#3M  ')).toBe(item);
  expect(isValidNewSearchOption([item], 'emerald#3m')).toBe(false);
  expect(isValidNewSearchOption([item], 'emerald#3')).toBe(true);
});

test.each(['Efficiency 7', 'Efficiency VII'])(
  'suppresses a duplicate free-text row for exact enchantment alias %s',
  (input) => {
    const option = createEnchantmentOption({ name: 'efficiency', level: 7 });

    expect(findExactSearchOption([option], input)).toBe(option);
    expect(isValidNewSearchOption([option], input)).toBe(false);
  }
);

test('deduplicates repeated material/name and enchantment suggestions', () => {
  const customName = {
    value: 'Diamond Axe',
    label: 'Diamond Axe',
    kind: 'name',
    searchTerms: 'Diamond Axe',
  };
  const material = {
    value: 'diamond_axe',
    label: 'diamond_axe',
    kind: 'material',
    searchTerms: 'diamond_axe',
  };
  const enchantment = createEnchantmentOption({
    name: 'efficiency',
    level: 10,
  });

  expect(
    deduplicateSearchOptions([
      customName,
      material,
      enchantment,
      { ...enchantment },
    ])
  ).toEqual([
    {
      ...material,
      label: 'Diamond Axe',
      searchTerms: 'Diamond Axe diamond_axe',
    },
    enchantment,
  ]);
});

test('builds a contextual Show all facet from matching enchantment names', () => {
  const search = { value: 'speed', label: 'speed', kind: 'query' };
  const levels = contextualEnchantmentLevels(search, [
    { name: 'soul_speed', level: 3 },
    { name: 'efficiency', level: 10 },
    { name: 'soul_speed', level: 1 },
    { name: 'soul_speed', level: 3 },
  ]);

  expect([SHOW_ALL_ENCHANTMENT_LEVELS, ...levels].map(({ label }) => label))
    .toEqual(['Show all', 'Soul Speed I', 'Soul Speed III']);
});

test.each(['soul speed 3', 'Soul Speed III'])(
  'removes a typed level when building facets for %s',
  (value) => {
    const levels = contextualEnchantmentLevels(
      { value, label: value, kind: 'query' },
      [
        { name: 'soul_speed', level: 1 },
        { name: 'soul_speed', level: 3 },
      ]
    );

    expect(levels.map(({ level }) => level)).toEqual([1, 3]);
  }
);

test('an exact enchantment search only facets that enchantment name', () => {
  const search = createEnchantmentOption({ name: 'efficiency', level: 10 });
  const levels = contextualEnchantmentLevels(search, [
    { name: 'efficiency', level: 5 },
    { name: 'efficiency', level: 10 },
    { name: 'unbreaking', level: 3 },
  ]);

  expect(levels.map(({ label }) => label)).toEqual([
    'Efficiency V',
    'Efficiency X',
  ]);
});

test.each(['Efficiency 7', 'Efficiency VII'])(
  'promotes %s to an exact enchantment when suggestions finish loading',
  (input) => {
    const option = createEnchantmentOption({ name: 'efficiency', level: 7 });
    let state = reducer(undefined, { type: 'test/init' });
    state = reducer(
      state,
      setMaterial({ value: input, label: input, kind: 'query' })
    );

    state = reducer(state, loadedMaterials([option]));

    expect(state.options.material).toEqual(option);
    expect(state.options.enchantmentLevel).toEqual(option);
    expect(state.materials.results).toEqual([option]);
  }
);

test('promotes an exact item entered before suggestions finish loading', () => {
  const item = {
    value: 'emerald#3m',
    label: 'emerald#3m',
    kind: 'material',
  };
  let state = reducer(undefined, { type: 'test/init' });
  state = reducer(
    state,
    setMaterial({ value: 'emerald#3m', label: 'emerald#3m', kind: 'query' })
  );

  state = reducer(state, loadedMaterials([item]));

  expect(state.options.material).toEqual(item);
  expect(state.options.enchantmentLevel).toBeUndefined();
});

test('fetches contextual levels without the selected exact level', async () => {
  global.fetch = jest.fn().mockResolvedValue(
    responseWith([
      { name: 'soul_speed', level: 3 },
      { name: 'efficiency', level: 10 },
      { name: 'soul_speed', level: 1 },
    ])
  );
  const dispatch = jest.fn();

  await fetchEnchantmentLevels()(
    dispatch,
    () => ({
      chestShops: {
        options: optionsWith({
          tradeType: 'sell',
          server: 'main',
          hideFull: true,
          itemType: { value: 'books', label: 'All books' },
          material: { value: 'speed', label: 'speed', kind: 'query' },
          enchantmentLevel: createEnchantmentOption({
            name: 'soul_speed',
            level: 3,
          }),
        }),
      },
    })
  );

  const url = global.fetch.mock.calls[0][0];
  expect(url.pathname).toEqual(
    expect.stringMatching(/\/chest-shops\/enchantment-options$/)
  );
  expect(url.searchParams.get('tradeType')).toBe('sell');
  expect(url.searchParams.get('server')).toBe('main');
  expect(url.searchParams.get('itemType')).toBe('books');
  expect(url.searchParams.get('hideUnavailable')).toBe('true');
  expect(url.searchParams.get('query')).toBe('speed');
  expect(url.searchParams.has('enchantmentLevel')).toBe(false);
  expect(dispatch).toHaveBeenNthCalledWith(1, loadingEnchantmentLevels());
  expect(dispatch).toHaveBeenNthCalledWith(
    2,
    loadedEnchantmentLevels([
      createEnchantmentOption({ name: 'soul_speed', level: 1 }),
      createEnchantmentOption({ name: 'soul_speed', level: 3 }),
    ])
  );
});

test('fetches combined custom-item, material, and enchantment suggestions', async () => {
  global.fetch = jest
    .fn()
    .mockResolvedValueOnce(responseWith(['bread']))
    .mockResolvedValueOnce(responseWith(['Titan Pickaxe']))
    .mockResolvedValueOnce(
      responseWith([
        { name: 'efficiency', level: 7 },
        { name: 'efficiency', level: 13 },
      ])
    );
  const dispatch = jest.fn();

  await fetchMaterials()(
    dispatch,
    () => ({
      chestShops: {
        options: optionsWith({ server: 'main', tradeType: 'sell' }),
      },
    })
  );

  const urls = global.fetch.mock.calls.map(([url]) => url);
  expect(urls.map((url) => url.pathname)).toEqual([
    expect.stringMatching(/\/chest-shops\/material-names$/),
    expect.stringMatching(/\/chest-shops\/display-names$/),
    expect.stringMatching(/\/chest-shops\/enchantment-options$/),
  ]);
  urls.forEach((url) => {
    expect(url.searchParams.get('server')).toBe('main');
    expect(url.searchParams.get('tradeType')).toBe('sell');
  });
  expect(dispatch).toHaveBeenNthCalledWith(1, loadingMaterials());
  expect(dispatch).toHaveBeenNthCalledWith(
    2,
    loadedMaterials([
      {
        value: 'Titan Pickaxe',
        label: 'Titan Pickaxe',
        kind: 'name',
        searchTerms: 'Titan Pickaxe',
      },
      {
        value: 'bread',
        label: 'bread',
        kind: 'material',
        searchTerms: 'bread',
      },
      createEnchantmentOption({ name: 'efficiency', level: 7 }),
      createEnchantmentOption({ name: 'efficiency', level: 13 }),
    ])
  );
});

test('records a combined-suggestion fetch error', async () => {
  global.fetch = jest.fn().mockRejectedValue(new Error('offline'));
  const dispatch = jest.fn();

  await fetchMaterials()(
    dispatch,
    () => ({ chestShops: { options: optionsWith() } })
  );

  expect(dispatch).toHaveBeenLastCalledWith(
    erroredMaterials('Error: offline')
  );
});

test('ignores an older chest-shop response after filters change', async () => {
  const older = deferred();
  const newer = deferred();
  global.fetch = jest
    .fn()
    .mockImplementationOnce(() => older.promise)
    .mockImplementationOnce(() => newer.promise);
  const dispatch = jest.fn();

  const olderRequest = fetchChestShops()(dispatch, () => ({
    chestShops: { options: optionsWith() },
  }));
  const newerRequest = fetchChestShops()(dispatch, () => ({
    chestShops: {
      options: optionsWith({
        material: createEnchantmentOption({ name: 'mending', level: 1 }),
      }),
    },
  }));

  newer.resolve(
    responseWith({ results: ['new'], totalElements: 1, totalPages: 1 })
  );
  await newerRequest;
  older.resolve(
    responseWith({ results: ['old'], totalElements: 1, totalPages: 1 })
  );
  await olderRequest;

  const loadedActions = dispatch.mock.calls
    .map(([action]) => action)
    .filter((action) => action.type === loaded.type);
  expect(loadedActions).toEqual([
    loaded({ results: ['new'], totalResults: 1, totalPages: 1 }),
  ]);
});

test('ignores stale combined suggestions', async () => {
  const requests = Array.from({ length: 6 }, deferred);
  global.fetch = jest.fn(
    () => requests[global.fetch.mock.calls.length - 1].promise
  );
  const dispatch = jest.fn();
  const state = () => ({ chestShops: { options: optionsWith() } });

  const olderRequest = fetchMaterials()(dispatch, state);
  const newerRequest = fetchMaterials()(dispatch, state);

  requests[3].resolve(responseWith(['diamond_pickaxe']));
  requests[4].resolve(responseWith(['Titan Pickaxe']));
  requests[5].resolve(responseWith([{ name: 'efficiency', level: 13 }]));
  await newerRequest;
  requests[0].resolve(responseWith(['bread']));
  requests[1].resolve(responseWith([]));
  requests[2].resolve(responseWith([]));
  await olderRequest;

  const loadedActions = dispatch.mock.calls
    .map(([action]) => action)
    .filter((action) => action.type === loadedMaterials.type);
  expect(loadedActions).toEqual([
    loadedMaterials([
      {
        value: 'Titan Pickaxe',
        label: 'Titan Pickaxe',
        kind: 'name',
        searchTerms: 'Titan Pickaxe',
      },
      {
        value: 'diamond_pickaxe',
        label: 'diamond_pickaxe',
        kind: 'material',
        searchTerms: 'diamond_pickaxe',
      },
      createEnchantmentOption({ name: 'efficiency', level: 13 }),
    ]),
  ]);
});
