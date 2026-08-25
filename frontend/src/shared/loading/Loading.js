import React from 'react';
import LinearProgress from '@material-ui/core/LinearProgress';

import './loading.css';

export const Loading = ({ className }) => {
  return <LinearProgress className={className} />;
};
