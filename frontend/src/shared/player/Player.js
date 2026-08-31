import React from 'react';
import { Link } from 'react-router-dom';

import { shopLocationPath, shopLocationTypeOf } from '../../shopLocationTypes';
import './player.css';

export const Towns = ({ towns }) => {
  if (towns === undefined || towns.length === 0) {
    return 'None';
  }

  return towns.map((town, idx) => {
    if (idx > 3) return undefined;

    return (
      <span className='towns' key={idx}>
        <Link
          to={shopLocationPath(shopLocationTypeOf(town), town.server, town.name)}
          className='link weight-bold'
        >
          {town.name}
        </Link>
        {towns.length >= 4 && idx === 3 && `... (${towns.length - 3} more)`}
        {idx !== 3 && towns.length - 1 !== idx && ', '}
      </span>
    );
  });
};

export const PlayerDescription = ({ name, numChestShops, numTowns }) => {
  return (
    <span className='ml-80 block txt-sm'>
      {name}{' '}
      owns {numTowns === undefined || numTowns === 0 ? 'no' : numTowns}{' '}
      {numTowns === 1 ? 'shop location' : 'shop locations'} and{' '}
      {numChestShops === 0 ? 'no' : numChestShops}{' '}
      {numChestShops === 1 ? 'chest shop.' : 'chest shops.'}
    </span>
  );
};

export const PlayerInfo = ({ name, towns }) => {
  return (
    <div>
      <Link to={`/search/players/${name}`} className='link-no-color'>
        <span className='block txt-sm weight-bold pb-1'>{name}</span>
      </Link>

      <span className='block txt-sm weight-lite'>
        Shop Locations: <Towns towns={towns} />
      </span>
    </div>
  );
};

export const Player = ({ player }) => {
  return (
    <div className='player background-dark p-5 mt-3 mb-3'>
      <div className='flex flex-between'>
        <div className='flex'>
          <Link to={`/search/players/${player.name}`}>
            <img
              className='mc-avatar mr-3'
              src={`https://mc-heads.net/avatar/${player.name}/60`}
              alt='Avatar'
            />
          </Link>
          <PlayerInfo
            name={player.name}
            towns={player.towns}
            active={player.active}
          />
        </div>
      </div>

      <PlayerDescription
        name={player.name}
        numChestShops={player.numChestShops}
        numTowns={player.towns.length}
      />
    </div>
  );
};
