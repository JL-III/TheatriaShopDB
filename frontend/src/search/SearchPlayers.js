import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { getOptions, setSortBy } from '../state/playersSlice';

import { Filter } from '../shared/filters';
import Players from './Players';
import {Select} from "../shared/select";

const SearchPlayers = () => {
  const dispatch = useDispatch();
  const options = useSelector(getOptions);

  const sortByOptions = [
    { value: 'name', label: 'Name' },
    { value: 'num-chest-shops', label: 'Chest Shop Count'},
    { value: 'num-regions', label: 'Town Count'}
  ]

  return (
    <section id='players' className='background vh-100 pt-50'>
      <div className='container flex'>
        <div id='filters'>
          <h3 className='color-white weight-bold txt-sm'>Filters</h3>
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

        <Players />
      </div>
    </section>
  );
};

export default SearchPlayers;
