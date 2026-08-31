import React from 'react';
import { BrowserRouter, Switch, Route, Redirect } from 'react-router-dom';

import { Home } from './home';
import { Navbar } from './shared/navbar';
import SearchChestShops from './search/SearchChestShops';
import SearchRegions from './search/SearchRegions';
import SearchPlayers from './search/SearchPlayers';
import SearchPlayer from './search/SearchPlayer';
import SearchRegion from './search/SearchRegion';
import {Docs} from "./docs";

function App() {
  return (
    <BrowserRouter>
      <Switch>
        <Route exact path="/home">
          <Navbar selectedPage="home" />
          <Home />
        </Route>

        <Route exact path="/search/chest-shops">
          <Navbar selectedPage="search" />
          <SearchChestShops />
        </Route>

        <Route exact path="/search/regions">
          <Navbar selectedPage="regions" />
          <SearchRegions />
        </Route>

        <Route exact path="/search/players">
          <Navbar selectedPage="players" />
          <SearchPlayers />
        </Route>

        <Route exact path="/search/players/:name">
          <Navbar selectedPage="players" />
          <SearchPlayer />
        </Route>

        <Route exact path="/search/regions/:type/:server/:name">
          <Navbar selectedPage="regions" />
          <SearchRegion />
        </Route>

        <Route exact path="/search/regions/:server/:name">
          <Navbar selectedPage="regions" />
          <SearchRegion />
        </Route>

        <Route exact path="/docs">
          <Navbar selectedPage="documentation" />
          <Docs />
        </Route>

        <Route path="/">
          <Redirect to="/home" />
        </Route>
      </Switch>
    </BrowserRouter>
  );
}

export default App;
