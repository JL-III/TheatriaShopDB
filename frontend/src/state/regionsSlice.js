import { createSlice } from '@reduxjs/toolkit';
import { parseResponse } from '../api';

export const regionsSlice = createSlice({
  name: 'regions',
  initialState: {
    options: {
      server: 'all',
      hideNonShopTowns: true,
      name: undefined,
      page: 1,
      sortBy: { value: 'name', label: 'Name'}
    },
    loading: false,
    error: false,
    errorMessage: '',
    results: [],
    totalResults: 1,
    totalPages: 1,
    names: {
      loading: false,
      error: false,
      errorMessage: undefined,
      results: [],
    },
  },

  reducers: {
    setServer: (state, action) => {
      state.options.server = action.payload;
      state.options.page = 1;
    },
    setName: (state, action) => {
      state.options.name = action.payload;
      state.options.page = 1;
    },
    setHideNonShopTowns: (state, action) => {
      state.options.hideNonShopTowns = action.payload;
      state.options.page = 1;
    },
    setSortBy: (state, action) => {
      state.options.sortBy = action.payload;
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

    loadingNames: (state) => {
      state.names.loading = true;
      state.names.error = false;
    },

    loadedNames: (state, action) => {
      state.names.loading = false;
      state.names.error = false;
      state.names.results = action.payload;
    },

    erroredNames: (state, action) => {
      state.names.error = true;
      state.names.errorMessage = action.payload;
      state.names.loading = false;
      state.names.results = [];
    },
  },
});

export const {
  setServer,
  setName,
  setHideNonShopTowns,
  setSortBy,
  setPage,
  loading,
  loaded,
  errored,
  loadingNames,
  loadedNames,
  erroredNames,
} = regionsSlice.actions;

export const getOptions = (state) => state.regions.options;
export const getResults = (state) => state.regions.results;
export const getLoading = (state) => state.regions.loading;
export const getError = (state) => state.regions.error;
export const getErrorMessage = (state) => state.regions.errorMessage;
export const getTotalResults = (state) => state.regions.totalResults;
export const getRegionNames = (state) => state.regions.names;
export const getTotalPages = (state) => state.regions.totalPages;

export default regionsSlice.reducer;

export const fetchRegions = () => (dispatch, getState) => {
  const options = getState().regions.options;

  const url = new URL(`${process.env.REACT_APP_BACKEND}/regions`);

  url.searchParams.append('page', options.page);
  url.searchParams.append('active', options.hideNonShopTowns);

  if (options.server !== 'all') {
    url.searchParams.append('server', options.server);
  }

  if (options.name) {
    url.searchParams.append('name', options.name.value);
  }

  url.searchParams.append('sortBy', options.sortBy.value);

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

export const fetchRegionNames = () => (dispatch, getState) => {
  const options = getState().regions.options;

  dispatch(loadingNames());

  const url = new URL(`${process.env.REACT_APP_BACKEND}/regions/region-names`);

  if (options.server !== 'all') url.searchParams.append('server', options.server);
  url.searchParams.append('active', options.hideNonShopTowns);

  fetch(url)
    .then(parseResponse)
    .then((response) => {
      dispatch(
        loadedNames(response.map((name) => ({ value: name, label: name })))
      );
    })
    .catch((err) => {
      dispatch(
        erroredNames(
          err === null || err === undefined
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};
