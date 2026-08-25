import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  getOptions,
  fetchChestShops,
  setPage,
  getResults,
  getError,
  getErrorMessage,
  getLoading,
  getTotalResults,
  getMaterials,
  getTotalPages,
  fetchMaterials,
  setMaterial,
} from '../state/chestShopsSlice';

import { Select } from '../shared/select';
import { TopPagination } from '../shared/top-pagination';
import { BottomPagination } from '../shared/bottom-pagination';
import { ChestShop } from '../shared/chest-shop';
import { Loading } from '../shared/loading';
import { AlertError } from '../shared/alert-error';

const ChestShops = () => {
  const dispatch = useDispatch();

  const options = useSelector(getOptions);
  const results = useSelector(getResults);
  const totalResults = useSelector(getTotalResults);
  const totalPages = useSelector(getTotalPages);
  const error = useSelector(getError);
  const errorMessage = useSelector(getErrorMessage);
  const loading = useSelector(getLoading);
  const materials = useSelector(getMaterials);

  useEffect(() => {
    dispatch(fetchChestShops());
  }, [options, dispatch]);

  return (
    <div className="w-100">
      <div id="results-top-panel" className="flex flex-between flex-center">
        <TopPagination
          page={options.page}
          setPage={(e, page) => dispatch(setPage(page + 1))}
          count={totalResults}
          labelTextEnd="chest shops."
          loading={loading}
        />

        <Select
          className="item-selector"
          placeholder="Item Name..."
          onFocus={() => dispatch(fetchMaterials())}
          value={options.material}
          setValue={(e, v) => dispatch(setMaterial(e))}
          loading={materials.loading}
          options={materials.results}
          isClearable
          windowed
        />
      </div>

      {loading && <Loading className="mt-5" />}

      {error && (
        <AlertError
          errorMessage={errorMessage}
          className="mt-3"
          retry={() => dispatch(fetchChestShops())}
        />
      )}

      {results.map((chestShop) => (
        <ChestShop
          chestShop={chestShop}
          key={chestShop.id}
          tradeType={options.tradeType}
        />
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

export default ChestShops;
