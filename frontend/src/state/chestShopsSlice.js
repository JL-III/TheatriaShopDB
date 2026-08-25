import { createSlice } from '@reduxjs/toolkit';
import { parseResponse } from '../api';

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
  },

  reducers: {
    setTradeType: (state, action) => {
      state.options.tradeType = action.payload;
      state.options.page = 1;
      if (action.payload === 'sell') {
        state.options.sortBy = { value: 'best-price', label: 'Best Price'};
      }
    },
    setServer: (state, action) => {
      state.options.server = action.payload;
      state.options.page = 1;
    },
    setHideOutOfStock: (state, action) => {
      state.options.hideOutOfStock = action.payload;
      state.options.page = 1;
    },
    setHideFull: (state, action) => {
      state.options.hideFull = action.payload;
      state.options.page = 1;
    },
    setSortBy: (state, action) => {
      state.options.sortBy = action.payload;
      state.options.page = 1;
    },
    setMaterial: (state, action) => {
      state.options.material = action.payload;
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
    },

    erroredMaterials: (state, action) => {
      state.materials.loading = false;
      state.materials.error = true;
      state.materials.errorMessage = action.payload;
      state.materials.results = [];
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
  setHideDistinct,
  setPage,
  loading,
  loaded,
  errored,
  loadingMaterials,
  loadedMaterials,
  erroredMaterials,
} = chestShopsSlice.actions;

export const getOptions = (state) => state.chestShops.options;
export const getResults = (state) => state.chestShops.results;
export const getLoading = (state) => state.chestShops.loading;
export const getError = (state) => state.chestShops.error;
export const getErrorMessage = (state) => state.chestShops.errorMessage;
export const getTotalResults = (state) => state.chestShops.totalResults;
export const getMaterials = (state) => state.chestShops.materials;
export const getTotalPages = (state) => state.chestShops.totalPages;

export const fetchChestShops = () => (dispatch, getState) => {
  const options = getState().chestShops.options;

  const url = new URL(`${process.env.REACT_APP_BACKEND}/chest-shops`);
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
    url.searchParams.append('material', options.material.value);
  }

  dispatch(loading());

  fetch(url)
    .then(parseResponse)
    .then((response) => {
      dispatch(
        loaded({
          results: response.results,
          totalResults: response.totalElements,
          totalPages: response.totalPages,
        })
      );
    })
    .catch((err) => {
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

  dispatch(loadingMaterials());

  const url = new URL(`${process.env.REACT_APP_BACKEND}/chest-shops/material-names`)

  if (options.server !== 'all') url.searchParams.append('server', options.server);
  url.searchParams.append('tradeType', options.tradeType)

  fetch(url)
    .then(parseResponse)
    .then((response) => {
      dispatch(
        loadedMaterials(
          response.map((material) => ({ value: material, label: material }))
        )
      );
    })
    .catch((err) => {
      dispatch(
        erroredMaterials(
          err === undefined || err === null
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export default chestShopsSlice.reducer;
