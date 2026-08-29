import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';

import {
  getOptions,
  setTradeType,
  setHideOutOfStock,
  setSortBy,
  setHideFull,
  setHideDistinct,
  setItemType,
  getEnchantmentLevels,
  fetchEnchantmentLevels,
  setEnchantmentLevel,
  SHOW_ALL_ENCHANTMENT_LEVELS,
} from '../state/chestShopsSlice';
import { Filter, TradeTypeFilter } from '../shared/filters';
import { Select } from '../shared/select';
import ChestShops from './ChestShops';

import './search.css';

const ITEM_TYPE_OPTIONS = [
  { value: 'all', label: 'All items' },
  { value: 'books', label: 'All books' },
];

const SearchChestShops = () => {
  const dispatch = useDispatch();
  const options = useSelector(getOptions);
  const enchantmentLevels = useSelector(getEnchantmentLevels);

  React.useEffect(() => {
    dispatch(fetchEnchantmentLevels());
  }, [
    dispatch,
    options.material,
    options.itemType,
    options.tradeType,
    options.server,
    options.hideOutOfStock,
    options.hideFull,
  ]);

  const sortByOptionsBuy = [
    { value: 'best-price', label: 'Best Price' },
    { value: 'quantity', label: 'Quantity' },
    { value: 'quantity-available', label: 'Quantity Available' },
  ];

  const sortByOptionsSell = [
    { value: 'best-price', label: 'Best Price' },
    { value: 'quantity', label: 'Quantity' },
  ];

  return (
    <section id="chest-shops" className="background vh-100 pt-50">
      <div className="container flex">
        <div id="filters">
          <h3 className="color-white weight-bold txt-sm">Filters</h3>
          <TradeTypeFilter
            value={options.tradeType}
            setValue={(e) => dispatch(setTradeType(e.target.value))}
          />

          <Filter title="Item Type">
            <Select
              className="filter-selector"
              label="Item type"
              value={options.itemType}
              setValue={(value) => dispatch(setItemType(value))}
              options={ITEM_TYPE_OPTIONS}
              isSearchable={false}
            />
          </Filter>

          {enchantmentLevels.results.length > 0 && (
            <Filter title="Enchantment Level">
              <Select
                className="filter-selector"
                label="Enchantment level"
                value={
                  options.enchantmentLevel || SHOW_ALL_ENCHANTMENT_LEVELS
                }
                setValue={(value) =>
                  dispatch(
                    setEnchantmentLevel(
                      value?.value === SHOW_ALL_ENCHANTMENT_LEVELS.value
                        ? undefined
                        : value
                    )
                  )
                }
                options={[
                  SHOW_ALL_ENCHANTMENT_LEVELS,
                  ...enchantmentLevels.results,
                ]}
                loading={enchantmentLevels.loading}
                isSearchable={false}
              />
            </Filter>
          )}

          <Filter title="Options">
            {options.tradeType === 'buy' ? (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={options.hideOutOfStock}
                    onChange={(e) =>
                      dispatch(setHideOutOfStock(e.target.checked))
                    }
                  />
                }
                label="Hide Unavailable Shops"
              />
            ) : (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={options.hideFull}
                    onChange={(e) => dispatch(setHideFull(e.target.checked))}
                  />
                }
                label="Hide Unavailable Shops"
              />
            )}

            <FormControlLabel
              control={
                <Checkbox
                  checked={options.hideDistinct}
                  onChange={(e) =>
                    dispatch(setHideDistinct(e.target.checked))
                  }
                />
              }
              label="Unique Shops Only"
            />
          </Filter>

          <Filter title="Sort By">
            <Select
              className="sort-by-selector"
              label="Sort chest shops by"
              value={options.sortBy}
              setValue={(e) => dispatch(setSortBy(e))}
              options={
                options.tradeType === 'buy'
                  ? sortByOptionsBuy
                  : sortByOptionsSell
              }
              isSearchable={false}
            />
          </Filter>
        </div>

        <ChestShops />
      </div>
    </section>
  );
};

export default SearchChestShops;
