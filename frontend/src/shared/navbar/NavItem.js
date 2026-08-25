import React from 'react';
import { Link } from 'react-router-dom';

import './navbar.css';

const NavItem = ({ selected, text, uri }) => {
  return (
    <Link
      to={uri}
      className={
        selected ? 'nav-item-selected color-primary weight-bold' : 'nav-item'
      }
    >
      {text}
    </Link>
  );
};

export default NavItem;
