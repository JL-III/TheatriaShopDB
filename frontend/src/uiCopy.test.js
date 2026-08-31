import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ITEMS_BUYING_TAB, ITEMS_FOR_SALE_TAB } from './shared/chest-shop';
import { TradeTypeFilter } from './shared/filters';
import { Navbar } from './shared/navbar';
import { Region, RegionDescription } from './shared/region';
import { Docs } from './docs';
import {
  MARKET_STALL,
  PLAYER_SHOP,
  shopLocationPath,
  travelCommandFor,
} from './shopLocationTypes';

test('trade labels describe the shop side of each API trade type', () => {
  const { getByLabelText } = render(
    <TradeTypeFilter value="buy" setValue={() => {}} />
  );

  expect(getByLabelText('Shops Selling').value).toBe('buy');
  expect(getByLabelText('Shops Buying').value).toBe('sell');
});

test('market stall cards show only the stall name and chest shop count', () => {
  const { container } = render(
    <RegionDescription name="bshop8" numChestShops={20} />
  );
  const copy = container.textContent.replace(/\s+/g, ' ').trim();

  expect(copy).toBe('bshop8 has 20 chest shops.');
  expect(copy).not.toMatch(/town|owner|the_ark/i);
});

test('shop locations navigation retains the existing regions route', () => {
  const { getAllByText, queryByText } = render(
    <MemoryRouter>
      <Navbar selectedPage="regions" />
    </MemoryRouter>
  );

  const shopLocationLinks = getAllByText('Shop Locations').map((label) =>
    label.closest('a')
  );

  expect(shopLocationLinks.length).toBeGreaterThan(0);
  shopLocationLinks.forEach((link) => {
    expect(link.getAttribute('href')).toBe('/search/regions');
  });
  expect(queryByText('Regions')).toBeNull();
});

test('shop location links and travel commands follow their location type', () => {
  expect(shopLocationPath(MARKET_STALL, 'The_Ark', 'shop4')).toBe(
    '/search/regions/MARKET_STALL/The_Ark/shop4'
  );
  expect(travelCommandFor(MARKET_STALL, 'shop4')).toBe('/warp shop4');
  expect(shopLocationPath(PLAYER_SHOP, 'The_Ark', 'Moon Base')).toBe(
    '/search/regions/PLAYER_SHOP/The_Ark/Moon%20Base'
  );
  expect(travelCommandFor(PLAYER_SHOP, 'Moon Base')).toBe(
    '/lands spawn Moon_Base'
  );
});

test('player shop cards use the typed detail route and land travel copy', () => {
  const { getByText } = render(
    <MemoryRouter>
      <Region
        region={{
          id: 1,
          name: 'Moon Base',
          server: 'The_Ark',
          type: PLAYER_SHOP,
          mayors: [{ name: 'Jesse' }],
          numChestShops: 3,
        }}
      />
    </MemoryRouter>
  );

  expect(getByText('Moon Base').closest('a').getAttribute('href')).toBe(
    '/search/regions/PLAYER_SHOP/The_Ark/Moon Base'
  );
  expect(getByText('Copy Land Spawn')).toBeTruthy();
});

test('docs limit player publishing to the owner of the land', () => {
  const { container } = render(<Docs />);
  const copy = container.textContent.replace(/\s+/g, ' ').trim();

  expect(copy).toContain('Stand inside a land you own and run /shopdb list.');
  expect(copy).toContain('Only the owner of that land can list it');
  expect(copy).toContain('trusted members and other non-owners cannot');
  expect(copy).toContain('/shopdb unlist');
  expect(copy).toContain("ShopDB starts refreshing the land's chest shops");
  expect(copy).toContain('Normal chest-shop interactions continue');
});

test('item tabs retain the correct API trade types', () => {
  expect(ITEMS_FOR_SALE_TAB).toEqual({
    label: 'Items for Sale',
    value: 'chest-shops-sold',
    tradeType: 'buy',
  });
  expect(ITEMS_BUYING_TAB).toEqual({
    label: 'Items Buying',
    value: 'chest-shops-bought',
    tradeType: 'sell',
  });
});
