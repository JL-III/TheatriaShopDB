import React from 'react';
import { Link } from 'react-router-dom';
import StoreIcon from '@material-ui/icons/Store';

import { CopyButton } from '../copy-button';
import './region.css';

export const getBackgroundColor = (regionName) => {
  if (regionName.length < 3) {
    return 'store-icon-1';
  }

  const regionNameChar = regionName[2].toUpperCase();
  if ('ABCD'.includes(regionNameChar)) {
    return 'store-icon-1';
  } else if ('EFG'.includes(regionNameChar)) {
    return 'store-icon-2';
  } else if ('HIJK'.includes(regionNameChar)) {
    return 'store-icon-3';
  } else if ('LMNO'.includes(regionNameChar)) {
    return 'store-icon-4';
  } else if ('PQRS'.includes(regionNameChar)) {
    return 'store-icon-5';
  } else if ('TUV'.includes(regionNameChar)) {
    return 'store-icon-6';
  } else {
    return 'store-icon-7';
  }
};

export const Mayors = ({ names }) => {
  if (names === undefined || names.length === 0) {
    return 'None';
  }

  return names.map((name, idx) => {
    if (idx > 3) return undefined;
    return (
      <span className="mayor" key={idx}>
        <Link to={`/search/players/${name}`} className="link weight-bold">
          {name}
        </Link>
        {names.length >= 4 && idx === 3 && `... (${names.length - 3} more)`}
        {idx !== 3 && names.length - 1 !== idx && ', '}
      </span>
    );
  });
};

export const RegionDescription = ({
  name,
  numMayors,
  numChestShops,
  active,
  server,
}) => {
  return (
    <span className="ml-80 block pb-4 txt-sm">
      {name} is a town on {server.toLowerCase()} with{' '}
      {numMayors === 0 ? 'no' : numMayors}{' '}
      {numMayors === 1 ? 'owner' : 'owners'} and{' '}
      {numChestShops === 0 ? 'no' : numChestShops}{' '}
      {numChestShops === 1 ? 'chest shop.' : 'chest shops.'}
    </span>
  );
};

export const RegionInfo = ({ name, server, mayors, active }) => {
  return (
    <div>
      <Link to={`/search/regions/${server}/${name}`} className="link-no-color">
        <span className="block txt-sm weight-bold">{name}</span>
      </Link>

      <span className="block txt-sm weight-lite">
        Owners: <Mayors names={mayors} />
      </span>

      <span className={`block txt-sm weight-lite pb-3 weight-bold ${active ? 'color-green' : 'color-error'}`}>
        {active ? 'Listed' : 'Unlisted'}
      </span>
    </div>
  );
};

export const Region = ({ region }) => {
  return (
    <div className="region background-dark p-5 mt-3 mb-3">
      <div className="flex">
        <Link to={`/search/regions/${region.name}`}>
          <StoreIcon
            fontSize="inherit"
            className={getBackgroundColor(region.name)}
          />
        </Link>
        <RegionInfo
          name={region.name}
          server={region.server}
          mayors={region.mayors.map(m => m.name)}
          active={region.active}
        />
      </div>
      <RegionDescription
        name={region.name}
        numMayors={region.mayors.length}
        numChestShops={region.numChestShops}
        active={region.active}
        server={region.server}
      />
      <CopyButton
        text="Copy Warp"
        copyText={`/warp ${region.name}`}
        className="ml-80 mt-2 txt-xs button-primary"
      />
    </div>
  );
};
