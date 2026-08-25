import React from 'react';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Radio from '@material-ui/core/Radio';

import './filters.css';

export const Filter = ({ title, children }) => {
  return (
    <div className='filter pt-5'>
      <h4 className='txt-sm weight-bold color-secondary pb-1 uppercase'>{title}</h4>
      {children}
    </div>
  );
};

export const TradeTypeFilter = ({ value, setValue }) => {
  return (
    <Filter title='Trade Type'>
      <RadioGroup value={value} onChange={setValue}>
        <FormControlLabel value='buy' control={<Radio />} label='Buy' />
        <FormControlLabel value='sell' control={<Radio />} label='Sell' />
      </RadioGroup>
    </Filter>
  );
};

export const ServerFilter = ({ value, setValue }) => {
  return (
    <Filter title='Server'>
      <RadioGroup value={value} onChange={setValue}>
        <FormControlLabel value='all' control={<Radio />} label='All' />
        <FormControlLabel value='main' control={<Radio />} label='Main' />
        <FormControlLabel
          value='main-north'
          control={<Radio />}
          label='Main North'
        />
        <FormControlLabel
          value='main-east'
          control={<Radio />}
          label='Main East'
        />
      </RadioGroup>
    </Filter>
  );
};
