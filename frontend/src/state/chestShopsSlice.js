import { BACKEND } from '../backend';
import { createSlice } from '@reduxjs/toolkit';
import { parseResponse } from '../api';
import { prettyEnchant, prettyMaterial, romanLevel } from '../shared/mc-text/format';

export const ALL_ITEMS = { value: 'all', label: 'All items' };
export const SHOW_ALL_ENCHANTMENT_LEVELS = {
  value: 'all',
  label: 'Show all',
};

let latestChestShopsRequest = 0;
let latestMaterialsRequest = 0;
let latestEnchantmentLevelsRequest = 0;

export const normalizeSearchText = (value) =>
  String(value || '')
    .trim()
    .toLocaleLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ');

export const createEnchantmentOption = ({ name, level }) => {
  const numericLevel = Number(level);
  const label = prettyEnchant({ name, level: numericLevel });
  const baseName = normalizeSearchText(name);
  const prettyName = normalizeSearchText(prettyMaterial(name));
  const numeral = normalizeSearchText(romanLevel(numericLevel));
  const aliases = [
    label,
    `${baseName} ${numericLevel}`,
    `${prettyName} ${numericLevel}`,
    `${baseName} ${numeral}`,
    `${prettyName} ${numeral}`,
  ]
    .map(normalizeSearchText)
    .filter(Boolean);

  return {
    value: `enchantment:${name}:${numericLevel}`,
    label,
    kind: 'enchantment',
    name,
    level: numericLevel,
    aliases: [...new Set(aliases)],
    searchTerms: [...new Set(aliases)].join(' '),
  };
};

export const matchesSearchOption = (candidate, inputValue) => {
  const option = candidate.data || candidate;
  const query = normalizeSearchText(inputValue);
  if (!query) return true;
  return normalizeSearchText(
    option.searchTerms || [option.label, option.value].join(' ')
  ).includes(query);
};

export const findExactEnchantmentOption = (options, inputValue) => {
  const query = normalizeSearchText(inputValue);
  return (options || []).find(
    (option) =>
      option.kind === 'enchantment' &&
      (option.aliases || []).some((alias) => alias === query)
  );
};

export const findExactSearchOption = (options, inputValue) => {
  const query = normalizeSearchText(inputValue);
  if (!query) return undefined;

  // Keep Arabic/Roman enchantment aliases exact even when their displayed
  // label differs from what the player typed ("Efficiency 7" vs
  // "Efficiency VII").
  const enchantment = findExactEnchantmentOption(options, query);
  if (enchantment) return enchantment;

  return (options || []).find((option) =>
    [option.label, option.value]
      .map(normalizeSearchText)
      .some((alias) => alias === query)
  );
};

export const isValidNewSearchOption = (options, inputValue) =>
  normalizeSearchText(inputValue).length > 0 &&
  !findExactSearchOption(options, inputValue);

const searchOptionKey = (option) =>
  option.kind === 'enchantment'
    ? `enchantment:${normalizeSearchText(option.name)}:${Number(option.level)}`
    : `item:${normalizeSearchText(option.label)}`;

export const deduplicateSearchOptions = (options) => {
  const unique = new Map();

  (options || []).forEach((option) => {
    const key = searchOptionKey(option);
    const existing = unique.get(key);

    if (!existing) {
      unique.set(key, option);
      return;
    }

    // A custom display name can be identical to a material label. Prefer the
    // material in that ambiguous case: selecting it returns every shop for
    // that item instead of only the subset carrying the same custom name,
    // while retaining the friendlier display label.
    if (existing.kind === 'name' && option.kind === 'material') {
      unique.set(key, {
        ...option,
        label: existing.label,
        searchTerms: [existing.searchTerms, option.searchTerms]
          .filter(Boolean)
          .join(' '),
      });
    }
  });

  return [...unique.values()];
};

export const enchantmentNameFromSearch = (searchOption) => {
  if (searchOption?.kind === 'enchantment') {
    return normalizeSearchText(searchOption.name);
  }
  if (searchOption?.kind !== 'query') return '';

  return normalizeSearchText(searchOption.value).replace(
    /\s+(?:\d+|[ivxlcdm]+)$/i,
    ''
  );
};

export const contextualEnchantmentLevels = (searchOption, enchantments) => {
  const query = enchantmentNameFromSearch(searchOption);
  if (!query) return [];

  return deduplicateSearchOptions(
    (enchantments || [])
      .filter(
        ({ name, level }) =>
          typeof name === 'string' &&
          name.length > 0 &&
          Number.isFinite(Number(level)) &&
          normalizeSearchText(name).includes(query)
      )
      .map(createEnchantmentOption)
  ).sort(
    (left, right) =>
      normalizeSearchText(left.name).localeCompare(
        normalizeSearchText(right.name)
      ) || left.level - right.level
  );
};

export const chestShopsSlice = createSlice({
  name: 'chestshops',

  initialState: {
    options: {
      tradeType: 'buy',
      server: 'all',
      hideOutOfStock: true,
      hideFull: true,
      hideDistinct: true,
      sortBy: { value: 'best-price', label: 'Best Price' },
      material: undefined,
      enchantmentLevel: undefined,
      itemType: ALL_ITEMS,
      page: 1,
    },
    loading: false,
    error: false,
    errorMessage: '',
    results: [],
    totalResults: 1,
    totalPages: 1,
    materials: {
      loading: false,
      error: false,
      errorMessage: undefined,
      results: [],
    },
    enchantmentLevels: {
      loading: false,
      error: false,
      errorMessage: undefined,
      results: [],
    },
  },

  reducers: {
    setTradeType: (state, action) => {
      state.options.tradeType = action.payload;
      state.enchantmentLevels.results = [];
      state.options.page = 1;
      if (action.payload === 'sell') {
        state.options.sortBy = { value: 'best-price', label: 'Best Price'};
      }
    },
    setServer: (state, action) => {
      state.options.server = action.payload;
      state.enchantmentLevels.results = [];
      state.options.page = 1;
    },
    setHideOutOfStock: (state, action) => {
      state.options.hideOutOfStock = action.payload;
      state.enchantmentLevels.results = [];
      state.options.page = 1;
    },
    setHideFull: (state, action) => {
      state.options.hideFull = action.payload;
      state.enchantmentLevels.results = [];
      state.options.page = 1;
    },
    setSortBy: (state, action) => {
      state.options.sortBy = action.payload;
      state.options.page = 1;
    },
    setMaterial: (state, action) => {
      state.options.material = action.payload;
      state.options.enchantmentLevel =
        action.payload?.kind === 'enchantment' ? action.payload : undefined;
      state.enchantmentLevels.results = [];
      state.options.page = 1;
    },
    setEnchantmentLevel: (state, action) => {
      state.options.enchantmentLevel = action.payload;
      state.options.page = 1;
    },
    setItemType: (state, action) => {
      state.options.itemType = action.payload || ALL_ITEMS;
      state.enchantmentLevels.results = [];
      state.options.page = 1;
    },
    setHideDistinct: (state, action) => {
      state.options.hideDistinct = action.payload;
      state.options.page = 1;
    },

    setPage: (state, action) => {
      state.options.page = action.payload;
    },

    loading: (state) => {
      state.error = false;
      state.loading = true;
      state.results = [];
    },

    loaded: (state, action) => {
      state.error = false;
      state.loading = false;
      state.results = action.payload.results;
      state.totalResults = action.payload.totalResults;
      state.totalPages = action.payload.totalPages;
    },

    errored: (state, action) => {
      state.error = true;
      state.errorMessage = action.payload;
      state.loading = false;
      state.results = [];
      state.options.page = 1;
      state.totalResults = 0;
      state.totalPages = 0;
    },

    loadingMaterials: (state) => {
      state.materials.loading = true;
      state.materials.error = false;
    },

    loadedMaterials: (state, action) => {
      state.materials.loading = false;
      state.materials.error = false;
      state.materials.results = action.payload;

      // Enter remains available while suggestions load. Promote a submitted
      // query when it becomes an exact item or enchantment suggestion so that
      // this timing path has the same semantics as selecting the visible row.
      if (state.options.material?.kind === 'query') {
        const exactOption = findExactSearchOption(
          action.payload,
          state.options.material.value
        );
        if (exactOption) {
          state.options.material = exactOption;
          state.options.enchantmentLevel =
            exactOption.kind === 'enchantment' ? exactOption : undefined;
          state.enchantmentLevels.results = [];
        }
      }
    },

    erroredMaterials: (state, action) => {
      state.materials.loading = false;
      state.materials.error = true;
      state.materials.errorMessage = action.payload;
      state.materials.results = [];
    },

    loadingEnchantmentLevels: (state) => {
      state.enchantmentLevels.loading = true;
      state.enchantmentLevels.error = false;
    },

    loadedEnchantmentLevels: (state, action) => {
      state.enchantmentLevels.loading = false;
      state.enchantmentLevels.error = false;
      state.enchantmentLevels.results = action.payload;
    },

    clearedEnchantmentLevels: (state) => {
      state.enchantmentLevels.loading = false;
      state.enchantmentLevels.error = false;
      state.enchantmentLevels.errorMessage = undefined;
      state.enchantmentLevels.results = [];
    },

    erroredEnchantmentLevels: (state, action) => {
      state.enchantmentLevels.loading = false;
      state.enchantmentLevels.error = true;
      state.enchantmentLevels.errorMessage = action.payload;
      state.enchantmentLevels.results = [];
    },
  },
});

export const {
  setTradeType,
  setServer,
  setHideOutOfStock,
  setHideFull,
  setSortBy,
  setMaterial,
  setEnchantmentLevel,
  setItemType,
  setHideDistinct,
  setPage,
  loading,
  loaded,
  errored,
  loadingMaterials,
  loadedMaterials,
  erroredMaterials,
  loadingEnchantmentLevels,
  loadedEnchantmentLevels,
  clearedEnchantmentLevels,
  erroredEnchantmentLevels,
} = chestShopsSlice.actions;

export const getOptions = (state) => state.chestShops.options;
export const getResults = (state) => state.chestShops.results;
export const getLoading = (state) => state.chestShops.loading;
export const getError = (state) => state.chestShops.error;
export const getErrorMessage = (state) => state.chestShops.errorMessage;
export const getTotalResults = (state) => state.chestShops.totalResults;
export const getMaterials = (state) => state.chestShops.materials;
export const getEnchantmentLevels = (state) =>
  state.chestShops.enchantmentLevels;
export const getTotalPages = (state) => state.chestShops.totalPages;

export const fetchChestShops = () => (dispatch, getState) => {
  const options = getState().chestShops.options;
  const requestId = ++latestChestShopsRequest;

  const url = new URL(`${BACKEND}/chest-shops`);
  url.searchParams.append('tradeType', options.tradeType);
  url.searchParams.append('sortBy', options.sortBy.value);
  url.searchParams.append('page', options.page);

  if (options.server !== 'all') {
    url.searchParams.append('server', options.server);
  }

  if (options.hideOutOfStock && options.tradeType === 'buy') {
    url.searchParams.append('hideUnavailable', 'true');
  }

  if (options.hideFull && options.tradeType === 'sell') {
    url.searchParams.append('hideUnavailable', 'true');
  }

  if (options.hideDistinct) {
    url.searchParams.append('distinct', 'true');
  }

  if (options.material) {
    if (options.material.kind === 'enchantment') {
      url.searchParams.append('enchantment', options.material.name);
    } else {
      // Listed choices retain their exact material/name filter. A free-text
      // choice searches all captured item metadata instead.
      const filter = options.material.kind === 'query'
        ? 'query'
        : options.material.kind === 'name'
          ? 'name'
          : 'material';
      url.searchParams.append(filter, options.material.value);
    }
  }

  if (options.enchantmentLevel) {
    url.searchParams.set('enchantment', options.enchantmentLevel.name);
    url.searchParams.set('enchantmentLevel', options.enchantmentLevel.level);
  }

  if (options.itemType && options.itemType.value !== 'all') {
    url.searchParams.append('itemType', options.itemType.value);
  }

  dispatch(loading());

  return fetch(url)
    .then(parseResponse)
    .then((response) => {
      if (requestId !== latestChestShopsRequest) return;
      dispatch(
        loaded({
          results: response.results,
          totalResults: response.totalElements,
          totalPages: response.totalPages,
        })
      );
    })
    .catch((err) => {
      if (requestId !== latestChestShopsRequest) return;
      dispatch(
        errored(
          err === null || err === undefined
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export const fetchMaterials = () => (dispatch, getState) => {
  const options = getState().chestShops.options;
  const requestId = ++latestMaterialsRequest;

  dispatch(loadingMaterials());

  const params = (path) => {
    const url = new URL(`${BACKEND}/chest-shops/${path}`);
    if (options.server !== 'all') url.searchParams.append('server', options.server);
    url.searchParams.append('tradeType', options.tradeType);
    return url;
  };

  // Materials, custom item names, and exact enchantment levels share the top
  // search field. Display names are first since players recognize them most.
  return Promise.all([
    fetch(params('material-names')).then(parseResponse),
    fetch(params('display-names')).then(parseResponse),
    fetch(params('enchantment-options')).then(parseResponse),
  ])
    .then(([materials, names, enchantments]) => {
      if (requestId !== latestMaterialsRequest) return;
      dispatch(
        loadedMaterials(
          deduplicateSearchOptions([
            ...names.map((name) => ({
              value: name,
              label: name,
              kind: 'name',
              searchTerms: name,
            })),
            ...materials.map((material) => ({
              value: material,
              label: material,
              kind: 'material',
              searchTerms: material,
            })),
            ...enchantments
              .filter(
                ({ name, level }) =>
                  typeof name === 'string' &&
                  name.length > 0 &&
                  Number.isFinite(Number(level))
              )
              .map(createEnchantmentOption),
          ])
        )
      );
    })
    .catch((err) => {
      if (requestId !== latestMaterialsRequest) return;
      dispatch(
        erroredMaterials(
          err === undefined || err === null
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export const fetchEnchantmentLevels = () => (dispatch, getState) => {
  const options = getState().chestShops.options;
  const requestId = ++latestEnchantmentLevelsRequest;
  const searchName = enchantmentNameFromSearch(options.material);

  if (!searchName) {
    dispatch(clearedEnchantmentLevels());
    return Promise.resolve();
  }

  dispatch(loadingEnchantmentLevels());

  const url = new URL(`${BACKEND}/chest-shops/enchantment-options`);
  url.searchParams.append('tradeType', options.tradeType);
  if (options.server !== 'all') url.searchParams.append('server', options.server);
  if (options.itemType && options.itemType.value !== 'all') {
    url.searchParams.append('itemType', options.itemType.value);
  }
  if (
    (options.tradeType === 'buy' && options.hideOutOfStock) ||
    (options.tradeType === 'sell' && options.hideFull)
  ) {
    url.searchParams.append('hideUnavailable', 'true');
  }

  if (options.material.kind === 'enchantment') {
    url.searchParams.append('enchantment', options.material.name);
  } else if (options.material.kind === 'query') {
    url.searchParams.append('query', options.material.value);
  } else if (options.material.kind === 'name') {
    url.searchParams.append('name', options.material.value);
  } else if (options.material.kind === 'material') {
    url.searchParams.append('material', options.material.value);
  }

  return fetch(url)
    .then(parseResponse)
    .then((response) => {
      if (requestId !== latestEnchantmentLevelsRequest) return;
      dispatch(
        loadedEnchantmentLevels(
          contextualEnchantmentLevels(options.material, response)
        )
      );
    })
    .catch((err) => {
      if (requestId !== latestEnchantmentLevelsRequest) return;
      dispatch(
        erroredEnchantmentLevels(
          err === undefined || err === null
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export default chestShopsSlice.reducer;
