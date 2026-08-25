// The ShopDB plugin serves this site and the API from the same origin, so by
// default the API is wherever the site was loaded from. REACT_APP_BACKEND
// still overrides it at build time (used by `npm start` via .env.development).
export const BACKEND = process.env.REACT_APP_BACKEND || `${window.location.origin}/api/v3`;
