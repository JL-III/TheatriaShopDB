import React from 'react';

import './breadcrumbs.css';

export const Breadcrumb = ({ children }) => {
  return <div className='breadcrumb'>{children}</div>;
};

export const Breadcrumbs = ({ children }) => {
  return (
    <div id='breadcrumbs'>
      <div className='container flex flex-center breadcrumb-container'>
        {children}
      </div>
    </div>
  );
};
