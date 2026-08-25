import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';

import {
  getOptions,
  setHideNonShopTowns,
  setSortBy
} from '../state/regionsSlice';

import { Filter, ServerFilter } from '../shared/filters';
import Regions from './Regions';
import {Select} from "../shared/select";

const SearchRegions = () => {
  const dispatch = useDispatch();
  const options = useSelector(getOptions);

  const sortByOptions = [
    { value: 'name', label: 'Name' },
    { value: 'num-chest-shops', label: 'Chest Shop Count'},
    { value: 'num-players', label: 'Mayor Count'}
  ]

  return (
    <section id='regions' className='background vh-100 pt-50'>
      <div className='container flex'>
        <div id='filters'>
          <h3 className='color-white weight-bold txt-sm'>Filters</h3>

          <Filter title='Options'>
            <FormControlLabel
              control={
                <Checkbox
                  checked={options.hideNonShopTowns}
                  onChange={(e) =>
                    dispatch(setHideNonShopTowns(e.target.checked))
                  }
                />
              }
              label='Hide Unlisted Towns'
            />
          </Filter>

          <Filter title="Sort By">
            <Select
              className="sort-by-selector"
              value={options.sortBy}
              setValue={(e) => dispatch(setSortBy(e))}
              options={sortByOptions}
              isSearchable={false}
            />
          </Filter>
        </div>

        <Regions />
      </div>
    </section>
  );
};

export default SearchRegions;
