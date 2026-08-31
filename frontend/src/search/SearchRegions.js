import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useHistory, useLocation } from 'react-router-dom';
import Paper from '@material-ui/core/Paper';
import Tab from '@material-ui/core/Tab';
import Tabs from '@material-ui/core/Tabs';

import {
  getOptions,
  setSortBy,
  setType,
} from '../state/regionsSlice';

import { Filter } from '../shared/filters';
import Regions from './Regions';
import {Select} from "../shared/select";
import {
  isShopLocationType,
  MARKET_STALL,
  PLAYER_SHOP,
} from '../shopLocationTypes';

const SearchRegions = () => {
  const dispatch = useDispatch();
  const options = useSelector(getOptions);
  const history = useHistory();
  const location = useLocation();

  const requestedTypeParam = new URLSearchParams(location.search).get('type');
  const requestedType = isShopLocationType(requestedTypeParam)
    ? requestedTypeParam
    : MARKET_STALL;

  useEffect(() => {
    if (requestedType !== options.type) {
      dispatch(setType(requestedType));
    }
  }, [dispatch, options.type, requestedType]);

  const changeType = (event, type) => {
    dispatch(setType(type));
    history.replace(`/search/regions?type=${type}`);
  };

  const sortByOptions = [
    { value: 'name', label: 'Name' },
    { value: 'num-chest-shops', label: 'Chest Shop Count'},
    { value: 'num-players', label: 'Owner Count'}
  ]

  return (
    <section id='regions' className='background vh-100 pt-50'>
      <div className='container shop-locations-intro pb-4'>
        <h1 className='color-white weight-bold txt-lg'>Shop Locations</h1>
        <p className='color-white txt-sm pt-1'>
          Browse shops at the server market or on player-owned lands.
        </p>
      </div>

      <Paper square className='shop-location-tabs'>
        <Tabs
          centered
          value={options.type}
          onChange={changeType}
          aria-label='Shop location type'
        >
          <Tab label='Market Stalls' value={MARKET_STALL} />
          <Tab label='Player Shops' value={PLAYER_SHOP} />
        </Tabs>
      </Paper>

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

        <Regions />
      </div>
    </section>
  );
};

export default SearchRegions;
