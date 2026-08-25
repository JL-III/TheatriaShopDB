import React from 'react';

import './home.css';
import {Link} from "react-router-dom";

export const Home = () => {
  return (
    <>
      <div id='backdrop' className='vh-100 center'>
        <section id='intro' className='background pt-100 pb-100'>
          <div className='container'>
            <h1>Theatria ShopDB</h1>
            <p>
              Find items to buy or sell on Theatria.
            </p>
          </div>
        </section>

        <section id='features' className='background-dark pt-100 pb-100'>
          <div className='container'>
            <div className='flex flex-around pb-50'>
              <div className='background p-5 feature'>
                <h3 className='txt-md weight-lite title'>
                  Chest Shops
                </h3>
                <p className='description'>
                  Search for items to buy or sell.
                </p>

                <Link to='/search/chest-shops' className='button'>
                  Search
                </Link>
              </div>

              <div className='background p-5 feature'>
                <h3 className='txt-md weight-lite title'>
                  Documentation
                </h3>
                <p className='description'>
                  Learn how ShopDB works and how to use it.
                </p>

                <Link to='/docs' className='button'>
                  Learn
                </Link>
              </div>
            </div>

            <div className='flex flex-around'>
              <div className='background p-5 feature'>
                <h3 className='txt-md weight-lite title'>
                  Regions
                </h3>

                <p className='description'>
                  View towns and chest shops they contain.
                </p>

                <Link to='/search/regions' className='button'>
                    Explore
                </Link>
              </div>

              <div className='background p-5 feature'>
                <h3 className='txt-md weight-lite title'>
                  Players
                </h3>
                <p className='description'>
                  View players and chest shops they own.
                </p>

                <Link to='/search/players' className='button'>
                  Stalk
                </Link>
              </div>
            </div>
          </div>
        </section>
      </div>
    </>
  );
};
