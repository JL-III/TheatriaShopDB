import React from 'react';
import Alert from '@material-ui/lab/Alert';

import './alert-error.css';

export const AlertError = ({ errorMessage, className, retry }) => {
  return (
    <div className={`alert-error ${className}`}>
      <Alert severity='error'>
        Oh no! An error occurred: ${errorMessage}
        <br />
        Please{' '}
        <span className='link pointer' onClick={retry}>
          try again
        </span>
        {'. '}
        If the error persits after a few minutes, please report it via Discord.
      </Alert>
    </div>
  );
};
