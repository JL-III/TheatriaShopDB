import { configureStore } from '@reduxjs/toolkit';
import chestShopsReducer from './chestShopsSlice';
import regionsReducer from './regionsSlice';
import playersReducer from './playersSlice';
import playerReducer from './playerSlice';
import regionReducer from './regionSlice';

export default configureStore({
  reducer: {
    chestShops: chestShopsReducer,
    regions: regionsReducer,
    players: playersReducer,
    player: playerReducer,
    region: regionReducer,
  },
});
