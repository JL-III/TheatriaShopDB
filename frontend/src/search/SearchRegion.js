import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';

import Paper from '@material-ui/core/Paper';
import Tab from '@material-ui/core/Tab';
import TabPanel from '@material-ui/lab/TabPanel';
import TabContext from '@material-ui/lab/TabContext';
import TabList from '@material-ui/lab/TabList';
import StoreIcon from '@material-ui/icons/Store';

import {
  fetchRegion,
  getRegion,
  getError,
  getErrorMessage,
  getLoading,
  fetchRegionPlayers,
  fetchRegionChestShops,
  getRegionPlayers,
  getRegionChestShops,
  setPlayersPage,
  setChestShopsPage,
  resetPlayersPage,
  resetChestShopsPage,
} from '../state/regionSlice';

import { getTimeFromNow } from '../api';
import { Breadcrumbs, Breadcrumb } from '../shared/breadcrumbs';
import { Loading } from '../shared/loading';
import { TopPagination } from '../shared/top-pagination';
import { Player } from '../shared/player';
import { getBackgroundColor } from '../shared/region';
import {
  ChestShop,
  ITEMS_BUYING_TAB,
  ITEMS_FOR_SALE_TAB,
} from '../shared/chest-shop';
import { AlertError } from '../shared/alert-error';
import { CopyButton } from '../shared/copy-button';
import {
  MARKET_STALL,
  normalizeShopLocationType,
  shopLocationLabel,
  shopLocationListPath,
  travelButtonTextFor,
  travelCommandFor,
} from '../shopLocationTypes';

const RegionBreadcrumbs = ({ name, server, type }) => {
  return (
    <Breadcrumbs>
      <Breadcrumb>
        <Link to="/search/regions" className="link-no-color">
          Shop Locations
        </Link>
      </Breadcrumb>
      <Breadcrumb>
        <Link to={shopLocationListPath(type)} className="link-no-color">
          {shopLocationLabel(type)}
        </Link>
      </Breadcrumb>
      <Breadcrumb>
        <span className="color-primary">
          {name} on {server}
        </span>
      </Breadcrumb>
    </Breadcrumbs>
  );
};

const Coordinates = ({ iBounds, oBounds }) => {
  return (
    <div className="coordinates-block background-dark mb-3">
      <span className="block txt-sm weight-bold pl-4 pt-4 pb-3 border-bottom">
        Shop Boundaries
      </span>
      <span className="block txt-sm pl-4 pt-2 pb-3 border-bottom">
        <span className="weight-bold">Lower: </span> {iBounds.x}, {iBounds.y},{' '}
        {iBounds.z}
      </span>
      <span className="block txt-sm pl-4 pt-2 pb-3 border-bottom">
        <span className="weight-bold">Upper: </span> {oBounds.x}, {oBounds.y},{' '}
        {oBounds.z}
      </span>
    </div>
  );
};

const LastUpdated = ({ lastUpdated, type }) => {
  return (
    <div className="last-updated-block background-dark">
      <span className="block txt-sm weight-bold pl-4 pt-4 pb-3 border-bottom">
        {shopLocationLabel(type, false)} Last Updated
      </span>
      <span className="block txt-sm pl-4 pt-2 pb-3">
        {getTimeFromNow(lastUpdated)}
      </span>
    </div>
  );
};

const RegionInfo = ({ name, server, numChestShops, type, travelCommand }) => {
  return (
    <div className="pt-50 pb-50 background flex">
      <StoreIcon
        fontSize="inherit"
        className={'icon-lg ' + getBackgroundColor(name)}
      />

      <div className="pl-1">
        <h2 className="txt-md weight-lite pb-1">
          {name} <span className="italic">({server})</span>
        </h2>
        <p className="pb-1">
          {name} has{' '}
          <span className="weight-bold">
            {numChestShops === 0 ? 'no' : numChestShops}{' '}
            {numChestShops === 1 ? 'chest shop.' : 'chest shops.'}
          </span>
        </p>
        <CopyButton
          text={travelButtonTextFor(type)}
          copyText={travelCommandFor(type, name, travelCommand)}
          className="mt-2 txt-xs button-primary"
        />
      </div>
    </div>
  );
};

const RegionChestShops = ({ name, server, type, tradeType }) => {
  const dispatch = useDispatch();
  const chestShops = useSelector(getRegionChestShops);
  const page = chestShops.page;

  useEffect(() => {
    dispatch(fetchRegionChestShops(name, server, type, tradeType));
  }, [dispatch, name, server, type, tradeType, page]);

  useEffect(() => {
    return () => dispatch(resetChestShopsPage());
  }, [dispatch]);

  return (
    <div className="background-black pt-5 vh-100">
      <div className="container flex flex-column flex-center vh-100">
        <TopPagination
          page={page}
          setPage={(e, page) => dispatch(setChestShopsPage(page + 1))}
          count={chestShops.totalResults}
          labelTextEnd="chest shops."
          loading={chestShops.loading}
        />

        {chestShops.error && (
          <AlertError
            errorMessage={chestShops.errorMessage}
            className="mt-3"
            retry={() =>
              dispatch(fetchRegionChestShops(name, server, type, tradeType))
            }
          />
        )}

        {chestShops.loading && <Loading className="w-100 mt-2" />}
        {chestShops.results &&
          chestShops.results.map((chestShop) => (
            <ChestShop
              chestShop={chestShop}
              tradeType={tradeType}
              locationType={type}
              key={chestShop.id}
            />
          ))}
      </div>
    </div>
  );
};

const RegionPlayers = ({ name, server, type }) => {
  const dispatch = useDispatch();
  const players = useSelector(getRegionPlayers);
  const page = players.page;

  useEffect(() => {
    dispatch(fetchRegionPlayers(name, server, type));
  }, [dispatch, name, server, type, page]);

  useEffect(() => {
    return () => dispatch(resetPlayersPage());
  }, [dispatch]);

  return (
    <div className="background-black pt-5 vh-100">
      <div className="container flex flex-column flex-center vh-100">
        <TopPagination
          page={page}
          setPage={(e, page) => dispatch(setPlayersPage(page + 1))}
          count={players.totalResults}
          labelTextEnd="owners."
          loading={players.loading}
        />
        {players.error && (
          <AlertError
            errorMessage={players.errorMessage}
            className="mt-3"
            retry={() => dispatch(fetchRegionPlayers(name, server, type))}
          />
        )}
        {players.loading && <Loading className="w-100 mt-2" />}
        {players.results &&
          players.results.map((player) => (
            <Player player={player} key={player.id} />
          ))}
      </div>
    </div>
  );
};

const SearchRegion = () => {
  const dispatch = useDispatch();
  const { name, server, type: routeType } = useParams();
  const type = normalizeShopLocationType(routeType);
  const loading = useSelector(getLoading);
  const region = useSelector(getRegion);
  const error = useSelector(getError);
  const errorMessage = useSelector(getErrorMessage);

  const [page, setPage] = useState('mayors');

  useEffect(() => {
    dispatch(fetchRegion(name, server, type));
  }, [dispatch, name, server, type]);

  return (
    <>
      <RegionBreadcrumbs name={name} server={server} type={type} />
      <div
        id="region"
        className={
          error || loading
            ? 'background vh-100 pt-3 pb-3'
            : 'background pt-3 pb-3'
        }
      >
        {loading && <Loading />}

        <div className="pt-3 pb-3 container flex flex-between flex-center">
          {region && (
            <RegionInfo
              name={region.name}
              numChestShops={region.numChestShops}
              server={region.server}
              type={type}
              travelCommand={region.travelCommand}
            />
          )}

          {region && (
            <div>
              {type === MARKET_STALL && region.iBounds && region.oBounds && (
                <Coordinates iBounds={region.iBounds} oBounds={region.oBounds} />
              )}
              {region.lastUpdated && (
                <LastUpdated lastUpdated={region.lastUpdated} type={type} />
              )}
            </div>
          )}

          {error && (
            <AlertError
              errorMessage={errorMessage}
              className="mt-3"
              retry={() => dispatch(fetchRegion(name, server, type))}
            />
          )}
        </div>

        {region && (
          <TabContext value={page}>
            <Paper square>
              <TabList
                centered
                onChange={(event, newChange) => setPage(newChange)}
              >
                <Tab label="Owners" value="mayors" />
                <Tab
                  label={ITEMS_FOR_SALE_TAB.label}
                  value={ITEMS_FOR_SALE_TAB.value}
                />
                <Tab
                  label={ITEMS_BUYING_TAB.label}
                  value={ITEMS_BUYING_TAB.value}
                />
              </TabList>

              <TabPanel value="mayors">
                <RegionPlayers name={name} server={server} type={type} />
              </TabPanel>

              <TabPanel value={ITEMS_FOR_SALE_TAB.value}>
                <RegionChestShops
                  name={name}
                  server={server}
                  type={type}
                  tradeType={ITEMS_FOR_SALE_TAB.tradeType}
                />
              </TabPanel>

              <TabPanel value={ITEMS_BUYING_TAB.value}>
                <RegionChestShops
                  name={name}
                  server={server}
                  type={type}
                  tradeType={ITEMS_BUYING_TAB.tradeType}
                />
              </TabPanel>
            </Paper>
          </TabContext>
        )}
      </div>
    </>
  );
};

export default SearchRegion;
