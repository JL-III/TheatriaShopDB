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
import { ChestShop } from '../shared/chest-shop';
import { AlertError } from '../shared/alert-error';

const RegionBreadcrumbs = ({ name, server }) => {
  return (
    <Breadcrumbs>
      <Breadcrumb>
        <Link to="/search/regions" className="link-no-color">
          Regions
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

const LastUpdated = ({ lastUpdated }) => {
  return (
    <div className="last-updated-block background-dark">
      <span className="block txt-sm weight-bold pl-4 pt-4 pb-3 border-bottom">
        Region Last Updated
      </span>
      <span className="block txt-sm pl-4 pt-2 pb-3">
        {getTimeFromNow(lastUpdated)}
      </span>
    </div>
  );
};

const RegionInfo = ({ name, server, active, numMayors, numChestShops }) => {
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
        <p className={`pb-2 ${active ? 'color-green' : 'color-error'}`}>
          {active ? 'Listed - chest shops in this region are shown.' : 'Unlisted - chest shops in this region are hidden.'}
        </p>
        <p className="pb-1">
          Has{' '}
          <span className="weight-bold">
            {numMayors === 0 ? 'no' : numMayors}{' '}
            {numMayors === 1 ? 'owner' : 'owners'}{' '}
          </span>{' '}
          and{' '}
          <span className="weight-bold">
            {numChestShops === 0 ? 'no' : numChestShops}{' '}
            {numChestShops === 1 ? 'chest shop.' : 'chest shops.'}
          </span>
        </p>
      </div>
    </div>
  );
};

const RegionChestShops = ({ name, server, tradeType }) => {
  const dispatch = useDispatch();
  const chestShops = useSelector(getRegionChestShops);
  const page = chestShops.page;

  useEffect(() => {
    dispatch(fetchRegionChestShops(name, server, tradeType));
  }, [dispatch, name, server, tradeType, page]);

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
              dispatch(fetchRegionChestShops(name, server, tradeType))
            }
          />
        )}

        {chestShops.loading && <Loading className="w-100 mt-2" />}
        {chestShops.results &&
          chestShops.results.map((chestShop) => (
            <ChestShop
              chestShop={chestShop}
              tradeType={tradeType}
              key={chestShop.id}
            />
          ))}
      </div>
    </div>
  );
};

const RegionPlayers = ({ name, server }) => {
  const dispatch = useDispatch();
  const players = useSelector(getRegionPlayers);
  const page = players.page;

  useEffect(() => {
    dispatch(fetchRegionPlayers(name, server));
  }, [dispatch, name, server, page]);

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
            retry={() => dispatch(fetchRegionPlayers(name, server))}
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
  const { name, server } = useParams();
  const loading = useSelector(getLoading);
  const region = useSelector(getRegion);
  const error = useSelector(getError);
  const errorMessage = useSelector(getErrorMessage);

  const [page, setPage] = useState('mayors');

  useEffect(() => {
    dispatch(fetchRegion(name, server));
  }, [dispatch, name, server]);

  return (
    <>
      <RegionBreadcrumbs name={name} server={server} />
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
              active={region.active}
              name={region.name}
              numChestShops={region.numChestShops}
              numMayors={region.mayors.length}
              server={region.server}
            />
          )}

          {region && (
            <div>
              <Coordinates iBounds={region.iBounds} oBounds={region.oBounds} />
              {region.lastUpdated && (
                <LastUpdated lastUpdated={region.lastUpdated} />
              )}
            </div>
          )}

          {error && (
            <AlertError
              errorMessage={errorMessage}
              className="mt-3"
              retry={() => dispatch(fetchRegion(name, server))}
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
                <Tab label="Items sold" value="chest-shops-sold" />
                <Tab label="Items purchased" value="chest-shops-bought" />
              </TabList>

              <TabPanel value="mayors">
                <RegionPlayers name={name} server={server} />
              </TabPanel>

              <TabPanel value="chest-shops-sold">
                <RegionChestShops name={name} server={server} tradeType="buy" />
              </TabPanel>

              <TabPanel value="chest-shops-bought">
                <RegionChestShops
                  name={name}
                  server={server}
                  tradeType="sell"
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
