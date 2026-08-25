import { createSlice } from '@reduxjs/toolkit';
import { parseResponse } from '../api';

export const playerSlice = createSlice({
  name: 'player',

  initialState: {
    info: undefined,
    loading: false,
    error: false,
    errorMessage: undefined,

    regions: {
      loading: false,
      error: false,
      errorMessage: undefined,
      results: [],
      page: 1,
      totalResults: 1,
      totalPages: 1,
    },

    chestShops: {
      loading: false,
      error: false,
      errorMessage: undefined,
      results: [],
      page: 1,
      totalResults: 1,
      totalPages: 1,
    },
  },

  reducers: {
    loading: (state) => {
      state.error = false;
      state.loading = true;
      state.info = undefined;
    },

    loaded: (state, action) => {
      state.error = false;
      state.loading = false;
      state.info = action.payload;
    },

    errored: (state, action) => {
      state.loading = false;
      state.error = true;
      state.errorMessage = action.payload;
    },

    regionsLoading: (state) => {
      state.regions.loading = true;
      state.regions.error = false;
      state.regions.results = [];
      state.regions.totalResults = 0;
      state.regions.totalPages = 0;
    },

    regionsLoaded: (state, action) => {
      state.regions.loading = false;
      state.regions.error = false;
      state.regions.results = action.payload.results;
      state.regions.totalResults = action.payload.totalResults;
      state.regions.totalPages = action.payload.totalPages;
    },

    regionsErrored: (state, action) => {
      state.regions.loading = false;
      state.regions.error = true;
      state.regions.errorMessage = action.payload;
    },

    chestShopsLoading: (state) => {
      state.chestShops.loading = true;
      state.chestShops.error = false;
      state.chestShops.results = [];
      state.chestShops.totalResults = 0;
      state.chestShops.totalPages = 0;
    },

    chestShopsLoaded: (state, action) => {
      state.chestShops.loading = false;
      state.chestShops.error = false;
      state.chestShops.results = action.payload.results;
      state.chestShops.totalResults = action.payload.totalResults;
      state.chestShops.totalPages = action.payload.totalPages;
    },

    chestShopsErrored: (state, action) => {
      state.chestShops.loading = false;
      state.chestShops.error = true;
      state.chestShops.errorMessage = action.payload;
    },

    setRegionsPage: (state, action) => {
      state.regions.page = action.payload;
    },

    resetRegionsPage: (state) => {
      state.regions.page = 1;
    },

    setChestShopsPage: (state, action) => {
      state.chestShops.page = action.payload;
    },

    resetChestShopsPage: (state) => {
      state.chestShops.page = 1;
    },
  },
});

export const {
  loading,
  loaded,
  errored,
  regionsLoading,
  regionsLoaded,
  regionsErrored,
  chestShopsLoading,
  chestShopsLoaded,
  chestShopsErrored,
  setRegionsPage,
  resetRegionsPage,
  setChestShopsPage,
  resetChestShopsPage,
} = playerSlice.actions;

export const getPlayer = (state) => state.player.info;
export const getLoading = (state) => state.player.loading;
export const getError = (state) => state.player.error;
export const getErrorMessage = (state) => state.player.errorMessage;
export const getPlayerRegions = (state) => state.player.regions;
export const getPlayerChestShops = (state) => state.player.chestShops;

export const fetchPlayer = (name) => (dispatch) => {
  dispatch(loading());

  fetch(`${process.env.REACT_APP_BACKEND}/players/${name}`)
    .then(parseResponse)
    .then((response) => {
      dispatch(loaded(response));
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

export const fetchPlayerRegions = (name) => (dispatch, getState) => {
  const page = getState().player.regions.page;

  dispatch(regionsLoading());

  fetch(
    `${process.env.REACT_APP_BACKEND}/players/${name}/regions?page=${page}`
  )
    .then(parseResponse)
    .then((response) => {
      dispatch(
        regionsLoaded({
          results: response.results,
          totalResults: response.totalElements,
          totalPages: response.totalPages,
        })
      );
    })
    .catch((err) => {
      dispatch(
        regionsErrored(
          err === null || err === undefined
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export const fetchPlayerChestShops = (name, tradeType) => (
  dispatch,
  getState
) => {
  const page = getState().player.chestShops.page;

  dispatch(chestShopsLoading());

  fetch(
    `${process.env.REACT_APP_BACKEND}/players/${name}/chest-shops?page=${page}&tradeType=${tradeType}`
  )
    .then(parseResponse)
    .then((response) => {
      dispatch(
        chestShopsLoaded({
          results: response.results,
          totalResults: response.totalElements,
          totalPages: response.totalPages,
        })
      );
    })
    .catch((err) => {
      dispatch(
        chestShopsErrored(
          err === null || err === undefined
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export default playerSlice.reducer;
