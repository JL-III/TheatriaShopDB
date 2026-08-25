import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  getOptions,
  fetchPlayers,
  setPage,
  getResults,
  getError,
  getErrorMessage,
  getNames,
  getLoading,
  getTotalResults,
  getTotalPages,
  fetchPlayerNames,
  setName,
} from '../state/playersSlice';

import { Select } from '../shared/select';
import { TopPagination } from '../shared/top-pagination';
import { BottomPagination } from '../shared/bottom-pagination';
import { Loading } from '../shared/loading';
import { AlertError } from '../shared/alert-error';
import { Player } from '../shared/player';

const Players = () => {
  const dispatch = useDispatch();

  const options = useSelector(getOptions);
  const results = useSelector(getResults);
  const totalResults = useSelector(getTotalResults);
  const totalPages = useSelector(getTotalPages);
  const error = useSelector(getError);
  const errorMessage = useSelector(getErrorMessage);
  const loading = useSelector(getLoading);
  const names = useSelector(getNames);

  useEffect(() => {
    dispatch(fetchPlayers());
  }, [options, dispatch]);

  return (
    <div className="w-100">
      <div id="results-top-panel" className="flex flex-between flex-center">
        <TopPagination
          page={options.page}
          setPage={(e, page) => dispatch(setPage(page + 1))}
          count={totalResults}
          labelTextEnd="players."
          loading={loading}
        />

        <Select
          className="name-selector"
          placeHolder="Player Name..."
          onFocus={() => dispatch(fetchPlayerNames())}
          value={options.name}
          setValue={(e, v) => dispatch(setName(e))}
          loading={names.loading}
          options={names.results}
          isClearable
          windowed
        />
      </div>

      {loading && <Loading className="mt-5" />}

      {error && (
        <AlertError
          errorMessage={errorMessage}
          className="mt-3"
          retry={() => dispatch(fetchPlayers())}
        />
      )}

      {results.map((player) => (
        <Player player={player} key={player.id} />
      ))}

      {results.length !== 0 && (
        <BottomPagination
          page={options.page}
          totalPages={totalPages}
          setPage={(e, page) => dispatch(setPage(page))}
        />
      )}
    </div>
  );
};

export default Players;
