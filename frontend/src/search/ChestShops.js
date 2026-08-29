import React, { useEffect, useState } from 'react';
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
  findExactSearchOption,
  isValidNewSearchOption,
  matchesSearchOption,
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
  const [searchText, setSearchText] = useState(
    options.material ? options.material.label : ''
  );

  const applySearchOption = (option) => {
    if (!option) {
      setSearchText('');
      dispatch(setMaterial(undefined));
      return;
    }

    setSearchText(option.label);
    dispatch(setMaterial(option));
  };

  const searchAllDetails = (inputValue) => {
    const query = inputValue.trim();
    if (!query) return;

    // CreatableSelect normally routes exact suggestions through onChange.
    // This fallback keeps Enter deterministic if the menu state changes while
    // suggestions are arriving.
    const exactOption = findExactSearchOption(materials.results, query);
    applySearchOption(
      exactOption || { value: query, label: query, kind: 'query' }
    );
  };

  const updateSearchText = (inputValue, { action }) => {
    // react-select asks to clear its input after a selection. Ignore that
    // internal transition: the field is an editable query, not a hidden
    // selected token.
    if (action === 'input-change') {
      setSearchText(inputValue);
      if (!inputValue && options.material) dispatch(setMaterial(undefined));
    }
    return inputValue;
  };

  const formatSearchOption = (option) => {
    const type =
      option.kind === 'enchantment'
        ? 'Enchantment'
        : option.kind === 'name'
          ? 'Custom item'
          : option.kind === 'material'
            ? 'Item'
            : 'Search';

    return (
      <div className="search-option">
        <span className="search-option-label">{option.label}</span>
        <span className="search-option-type">{type}</span>
      </div>
    );
  };

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

        <div className="item-selector-wrapper">
          <Select
            className="item-selector"
            label="Search items, enchantments, and lore"
            placeholder="Item, enchantment, or lore..."
            onFocus={() => dispatch(fetchMaterials())}
            value={null}
            setValue={applySearchOption}
            inputValue={searchText}
            onInputChange={updateSearchText}
            loading={materials.loading}
            options={materials.results}
            windowed
            creatable
            allowCreateWhileLoading
            createOptionPosition="first"
            formatCreateLabel={(inputValue) =>
              `Search for "${inputValue.trim()}"`
            }
            isValidNewOption={(inputValue) =>
              isValidNewSearchOption(materials.results, inputValue)
            }
            onCreateOption={searchAllDetails}
            filterOption={matchesSearchOption}
            formatOptionLabel={formatSearchOption}
            controlShouldRenderValue={false}
            selectInputTextOnFocus={false}
            keepInputVisible
          />

          {searchText && (
            <button
              type="button"
              className="item-search-clear"
              aria-label="Clear item search"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => applySearchOption(undefined)}
            >
              &times;
            </button>
          )}
        </div>
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
