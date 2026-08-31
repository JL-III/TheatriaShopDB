import React from 'react';

import NavItem from './NavItem';
import './navbar.css';
import {Drawer, IconButton} from "@material-ui/core";
import MenuIcon from '@material-ui/icons/Menu';
import {Link} from "react-router-dom";

export const Navbar = ({ selectedPage }) => {
  const [open, setOpen] = React.useState(false);

  return (
      <header id='navbar'>
        <div className='container flex flex-between flex-center'>
          <Link to='/home' className='link'>
            <img src='/img/logo.png' id='logo' alt='Logo' />
          </Link>

          <nav id='navigation'>
            <NavItem uri='/home' text='Home' selected={selectedPage === 'home'} />
            <NavItem
                uri='/search/chest-shops'
                text='Chest Shops'
                selected={selectedPage === 'search'}
            />
            <NavItem
                uri='/search/regions'
                text='Shop Locations'
                selected={selectedPage === 'regions'}
            />
            <NavItem
                uri='/search/players'
                text='Players'
                selected={selectedPage === 'players'}
            />
            <NavItem
                uri='/docs'
                text='Documentation'
                selected={selectedPage === 'documentation'}
            />
          </nav>

          <nav id='navigation-mobile'>
            <IconButton
                onClick={() => setOpen(!open)}
            >
              <MenuIcon />
            </IconButton>
            <Drawer className='drawer'
                    anchor='left'
                    handleClose={() => setOpen(false)}
                    onBackdropClick={() => setOpen(false)}
                    open={open}
            >
              <div className='nav-mobile-background'>
                <NavItem uri='/home' text='Home' selected={selectedPage === 'home'} />
                <NavItem
                    uri='/search/chest-shops'
                    text='Chest Shops'
                    selected={selectedPage === 'search'}
                />
                <NavItem
                    uri='/search/regions'
                    text='Shop Locations'
                    selected={selectedPage === 'regions'}
                />
                <NavItem
                    uri='/search/players'
                    text='Players'
                    selected={selectedPage === 'players'}
                />
                <NavItem
                    uri='/docs'
                    text='Documentation'
                    selected={selectedPage === 'documentation'}
                />
              </div>
            </Drawer>
          </nav>
        </div>
      </header>
  );
};
