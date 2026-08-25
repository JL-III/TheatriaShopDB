import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';

import Paper from '@material-ui/core/Paper';
import Tab from '@material-ui/core/Tab';
import TabPanel from '@material-ui/lab/TabPanel';
import TabContext from '@material-ui/lab/TabContext';
import TabList from '@material-ui/lab/TabList';

import {
  fetchPlayer,
  getPlayer,
  getError,
  getErrorMessage,
  getLoading,
  fetchPlayerRegions,
  getPlayerRegions,
  setRegionsPage,
  fetchPlayerChestShops,
  getPlayerChestShops,
  setChestShopsPage,
  resetChestShopsPage,
  resetRegionsPage,
} from '../state/playerSlice';

import { getTimeFromNow } from '../api';
import { Breadcrumbs, Breadcrumb } from '../shared/breadcrumbs';
import { Loading } from '../shared/loading';
import { TopPagination } from '../shared/top-pagination';
import { Region } from '../shared/region';
import { ChestShop } from '../shared/chest-shop';
import { AlertError } from '../shared/alert-error';

const PlayerBreadcrumbs = ({ name }) => {
  return (
    <Breadcrumbs>
      <Breadcrumb>
        <Link to="/search/players" className="link-no-color">
          Players
        </Link>
      </Breadcrumb>
      <Breadcrumb>
        <span className="color-primary">{name}</span>
      </Breadcrumb>
    </Breadcrumbs>
  );
};

const LastUpdated = ({ lastUpdated }) => {
  return (
    <div className="last-updated-block background-dark">
      <span className="block txt-sm weight-bold pl-4 pt-4 pb-3 border-bottom">
        Player Last Updated
      </span>
      <span className="block txt-sm pl-4 pt-2 pb-3">
        {getTimeFromNow(lastUpdated)}
      </span>
    </div>
  );
};

const PlayerInfo = ({ name, numTowns, numChestShops }) => {
  return (
    <div className="pt-50 pb-50 background flex">
      <img
        className="icon-lg mc-avatar mr-4"
        src={`https://mc-heads.net/avatar/${name}/100`}
        alt="Avatar"
      />

      <div className="pl-1">
        <h2 className="txt-md weight-lite pb-1">{name}</h2>
        <p className="pb-1">
          Owns{' '}
          <span className="weight-bold">
            {numTowns === 0 ? 'no' : numTowns}{' '}
            {numTowns === 1 ? 'town' : 'towns'}{' '}
          </span>
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

const PlayerChestShops = ({ name, tradeType }) => {
  const dispatch = useDispatch();
  const chestShops = useSelector(getPlayerChestShops);
  const page = chestShops.page;

  useEffect(() => {
    dispatch(fetchPlayerChestShops(name, tradeType));
  }, [dispatch, name, tradeType, page]);

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
            retry={() => dispatch(fetchPlayerChestShops(name, tradeType))}
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

const PlayerTowns = ({ name }) => {
  const dispatch = useDispatch();
  const regions = useSelector(getPlayerRegions);
  const page = regions.page;

  useEffect(() => {
    dispatch(fetchPlayerRegions(name));
  }, [dispatch, name, page]);

  useEffect(() => {
    return () => dispatch(resetRegionsPage());
  }, [dispatch]);

  return (
    <div className="background-black pt-5 vh-100">
      <div className="container flex flex-column flex-center vh-100">
        <TopPagination
          page={page}
          setPage={(e, page) => dispatch(setRegionsPage(page + 1))}
          count={regions.totalResults}
          labelTextEnd="towns."
          loading={regions.loading}
        />
        {regions.error && (
          <AlertError
            errorMessage={regions.errorMessage}
            className="mt-3"
            retry={() => dispatch(fetchPlayerRegions(name))}
          />
        )}
        {regions.loading && <Loading className="w-100 mt-2" />}
        {regions.results &&
          regions.results.map((region) => (
            <Region region={region} key={region.id} />
          ))}
      </div>
    </div>
  );
};

const SearchPlayer = () => {
  const dispatch = useDispatch();
  const { name } = useParams();
  const loading = useSelector(getLoading);
  const player = useSelector(getPlayer);
  const error = useSelector(getError);
  const errorMessage = useSelector(getErrorMessage);

  const [page, setPage] = useState('towns');

  useEffect(() => {
    dispatch(fetchPlayer(name));
  }, [dispatch, name]);

  return (
    <>
      <PlayerBreadcrumbs name={name} />
      <div
        id="player"
        className={error || loading ? 'background vh-100' : 'background'}
      >
        {loading && <Loading />}

        <div className="container flex flex-between flex-center">
          {player && (
            <PlayerInfo
              name={player.name}
              numChestShops={player.numChestShops}
              numTowns={player.towns.length}
            />
          )}

          {player && player.lastUpdated && (
            <LastUpdated lastUpdated={player.lastUpdated} />
          )}

          {error && (
            <AlertError
              errorMessage={errorMessage}
              className="mt-3"
              retry={() => dispatch(fetchPlayer(name))}
            />
          )}
        </div>

        {player && (
          <TabContext value={page}>
            <Paper square>
              <TabList
                centered
                onChange={(event, newChange) => setPage(newChange)}
              >
                <Tab label="Towns" value="towns" />
                <Tab label="Items sold" value="chest-shops-sold" />
                <Tab label="Items purchased" value="chest-shops-bought" />
              </TabList>

              <TabPanel value="towns">
                <PlayerTowns name={name} />
              </TabPanel>

              <TabPanel value="chest-shops-sold">
                <PlayerChestShops name={name} tradeType="buy" />
              </TabPanel>

              <TabPanel value="chest-shops-bought">
                <PlayerChestShops name={name} tradeType="sell" />
              </TabPanel>
            </Paper>
          </TabContext>
        )}
      </div>
    </>
  );
};

export default SearchPlayer;
