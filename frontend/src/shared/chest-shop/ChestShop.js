import React from 'react';
import { Link } from 'react-router-dom';

import { CopyButton } from '../copy-button';
import { McText, prettyMaterial, prettyEnchant } from '../mc-text';
import './chest-shop.css';

const moneyFormatter = new Intl.NumberFormat('en-US', {
  maximumFractionDigits: 20,
});

const unitPriceFormatter = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

// Clean display name for a shop's item: the true material when the updater
// captured it, otherwise the raw (possibly truncated) sign text.
const itemName = (chestShop) =>
  prettyMaterial(chestShop.baseMaterial) || chestShop.material;

// Custom name, enchantments, and lore of the traded item, when it has any.
export const ItemDetails = ({ details }) => {
  if (!details) {
    return null;
  }
  return (
    <div className="item-details txt-xs pt-1">
      {(details.enchants || []).map((enchant, i) => (
        <span key={`e${i}`} className="block item-enchant">
          {prettyEnchant(enchant)}
        </span>
      ))}
      {(details.lore || []).map((line, i) => (
        <span key={`l${i}`} className="block item-lore">
          <McText text={line} />
        </span>
      ))}
    </div>
  );
};

export const Stock = ({ tradeType, count, isFull }) => {
  if (tradeType === 'buy' && count === 0) {
    return (
      <span className="block txt-xs weight-bold color-error">
        Out of stock (0 left)
      </span>
    );
  }

  if (tradeType === 'buy' && count !== 0) {
    return (
      <span className="block txt-xs weight-bold color-green">
        In stock ({count} left)
      </span>
    );
  }

  if (tradeType === 'sell' && isFull) {
    return (
      <span className="block txt-xs weight-bold color-error">
        Full (count: {count})
      </span>
    );
  }

  if (tradeType === 'sell' && !isFull) {
    return (
      <span className="block txt-xs weight-bold color-green">
        Available (count: {count})
      </span>
    );
  }
};

export const ShopInfo = ({
  tradeType,
  quantity,
  item,
  displayName,
  details,
  count,
  price,
  player,
  region,
  server,
  isFull,
}) => {
  return (
    <div>
      <span className="block txt-sm weight-bold pb-1">
        {tradeType === 'buy' ? 'Selling' : 'Buying'} {quantity}{' '}
        {displayName ? (
          <>
            <McText text={displayName} />{' '}
            <span className="item-base-name weight-lite">({item})</span>
          </>
        ) : (
          item
        )}{' '}
        for ${unitPriceFormatter.format(price / quantity)} each
      </span>
      <span className="block txt-sm weight-lite pb-1">
        By{' '}
        <Link to={`/search/players/${player}`} className="link weight-bold">
          {player}
        </Link>{' '}
        • in{' '}
        <Link
          to={`/search/regions/${server}/${region}`}
          className="link weight-bold"
        >
          {region}
        </Link>{' '}
        <i>({server})</i>
      </span>
      <Stock tradeType={tradeType} count={count} isFull={isFull} />
      <ItemDetails details={details} />
    </div>
  );
};

export const ShopDescription = ({
  player,
  tradeType,
  quantity,
  item,
  region,
  price,
}) => {
  return (
    <span className="ml-80 block pt-4 pb-4 txt-sm">
      {player} is {tradeType === 'buy' ? 'selling' : 'buying'} {quantity} {item}{' '}
      in {region} for ${moneyFormatter.format(price)}
    </span>
  );
};

export const ChestShop = ({ chestShop, tradeType }) => {
  return (
    <div className="chest-shop background-dark p-5 mt-3 mb-5">
      <div className="flex">
        <img
          className="mc-avatar mr-3"
          src={`https://mc-heads.net/avatar/${chestShop.owner.name}/60`}
          alt="Avatar"
        />
        <ShopInfo
          tradeType={tradeType}
          quantity={chestShop.quantity}
          item={itemName(chestShop)}
          displayName={chestShop.itemDetails && chestShop.itemDetails.displayName}
          details={chestShop.itemDetails}
          count={chestShop.quantityAvailable}
          price={tradeType === 'buy' ? chestShop.buyPrice : chestShop.sellPrice}
          player={chestShop.owner.name}
          region={chestShop.town.name}
          server={chestShop.server}
          isFull={chestShop.full}
        />
      </div>
      <ShopDescription
        player={chestShop.owner.name}
        tradeType={tradeType}
        quantity={chestShop.quantity}
        item={itemName(chestShop)}
        region={chestShop.town.name}
        price={tradeType === 'buy' ? chestShop.buyPrice : chestShop.sellPrice}
      />
      <CopyButton
        text="Copy Warp"
        copyText={`/warp ${chestShop.town.name}`}
        className="ml-80 mt-2 txt-xs button-primary"
      />
      <CopyButton
        text="Copy Coordinates"
        copyText={`${chestShop.location.x} ${chestShop.location.y} ${chestShop.location.z}`}
        className="ml-2 mt-2 txt-xs button-primary"
      />
      <span className="ml-80 block mt-4 shop-coordinates txt-xs">
        Coordinates: {chestShop.location.x} {chestShop.location.y}{' '}
        {chestShop.location.z}
      </span>
    </div>
  );
};
