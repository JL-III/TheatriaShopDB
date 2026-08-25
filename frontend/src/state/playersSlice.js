import { createSlice } from '@reduxjs/toolkit';
import { parseResponse } from '../api';

export const playersSlice = createSlice({
  name: 'players',

  initialState: {
    options: {
      name: undefined,
      sortBy: { value: 'name', label: 'Name'},
      page: 1
    },
    names: {
      loading: false,
      error: false,
      errorMessage: undefined,
      results: [],
    },
    loading: false,
    error: false,
    errorMessage: '',
    results: [],
    totalResults: 1,
    totalPages: 1,
  },

  reducers: {
    setName: (state, action) => {
      state.options.name = action.payload;
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
      state.names.loading = false;
      state.names.error = true;
      state.names.errorMessage = action.payload;
      state.names.results = [];
    },
  },
});

export const {
  setName,
  setPage,
  setSortBy,
  loading,
  loaded,
  errored,
  loadingNames,
  loadedNames,
  erroredNames,
} = playersSlice.actions;

export const getOptions = (state) => state.players.options;
export const getResults = (state) => state.players.results;
export const getLoading = (state) => state.players.loading;
export const getError = (state) => state.players.error;
export const getErrorMessage = (state) => state.players.errorMessage;
export const getTotalResults = (state) => state.players.totalResults;
export const getNames = (state) => state.players.names;
export const getTotalPages = (state) => state.players.totalPages;

export const fetchPlayers = () => (dispatch, getState) => {
  const options = getState().players.options;

  const url = new URL(`${process.env.REACT_APP_BACKEND}/players`);
  url.searchParams.append('page', options.page);

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

export const fetchPlayerNames = () => (dispatch, getState) => {
  const options = getState().players.options;

  fetch(
    `${process.env.REACT_APP_BACKEND}/players/player-names`
  )
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

export default playersSlice.reducer;
