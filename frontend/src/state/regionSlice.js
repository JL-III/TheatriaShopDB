import { BACKEND } from '../backend';
import { createSlice } from '@reduxjs/toolkit';
import { parseResponse } from '../api';
import { normalizeShopLocationType } from '../shopLocationTypes';

export const regionSlice = createSlice({
  name: 'region',

  initialState: {
    info: undefined,
    loading: false,
    error: false,
    errorMessage: undefined,

    players: {
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

    playersLoading: (state) => {
      state.players.loading = true;
      state.players.error = false;
      state.players.results = [];
      state.players.totalResults = 0;
      state.players.totalPages = 0;
    },

    playersLoaded: (state, action) => {
      state.players.loading = false;
      state.players.error = false;
      state.players.results = action.payload.results;
      state.players.totalResults = action.payload.totalResults;
      state.players.totalPages = action.payload.totalPages;
    },

    playersErrored: (state, action) => {
      state.players.loading = false;
      state.players.error = true;
      state.players.errorMessage = action.payload;
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

    setPlayersPage: (state, action) => {
      state.players.page = action.payload;
    },

    resetPlayersPage: (state) => {
      state.players.page = 1;
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
  playersLoading,
  playersLoaded,
  playersErrored,
  regionsLoading,
  regionsLoaded,
  regionsErrored,
  chestShopsLoading,
  chestShopsLoaded,
  chestShopsErrored,
  setPlayersPage,
  resetPlayersPage,
  setChestShopsPage,
  resetChestShopsPage,
} = regionSlice.actions;

export const getRegion = (state) => state.region.info;
export const getLoading = (state) => state.region.loading;
export const getError = (state) => state.region.error;
export const getErrorMessage = (state) => state.region.errorMessage;
export const getRegionPlayers = (state) => state.region.players;
export const getRegionChestShops = (state) => state.region.chestShops;

const regionUrl = (name, server, type, suffix = '') => {
  const url = new URL(
    `${BACKEND}/regions/${encodeURIComponent(server)}/${encodeURIComponent(name)}${suffix}`
  );
  url.searchParams.append('type', normalizeShopLocationType(type));
  return url;
};

export const fetchRegion = (name, server, type) => (dispatch) => {
  dispatch(loading());

  fetch(regionUrl(name, server, type))
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

export const fetchRegionPlayers = (name, server, type) => (dispatch, getState) => {
  const page = getState().region.players.page;

  dispatch(playersLoading());

  const url = regionUrl(name, server, type, '/players');
  url.searchParams.append('page', page);

  fetch(url)
    .then(parseResponse)
    .then((response) => {
      dispatch(
        playersLoaded({
          results: response.results,
          totalResults: response.totalElements,
          totalPages: response.totalPages,
        })
      );
    })
    .catch((err) => {
      dispatch(
        playersErrored(
          err === null || err === undefined
            ? 'Unknown error occurred'
            : err.toString()
        )
      );
    });
};

export const fetchRegionChestShops = (name, server, type, tradeType) => (
  dispatch,
  getState
) => {
  const page = getState().region.chestShops.page;

  dispatch(chestShopsLoading());

  const url = regionUrl(name, server, type, '/chest-shops');
  url.searchParams.append('page', page);
  url.searchParams.append('tradeType', tradeType);

  fetch(url)
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

export default regionSlice.reducer;
