import React from 'react';
import {Link} from 'react-router-dom';

import './home.css';

const features = [
  {
    slug: 'chest-shops',
    eyebrow: 'Marketplace',
    title: 'Chest Shops',
    description:
      'Search every listed buy and sell offer, then copy the coordinates and head in-game.',
    action: 'Search shops',
    uri: '/search/chest-shops',
    image: '/img/theatria/chest-shops.jpg',
  },
  {
    slug: 'documentation',
    eyebrow: 'Guide',
    title: 'Documentation',
    description:
      'Learn the search tools, listing details, and the small things that make ShopDB useful.',
    action: 'Read the guide',
    uri: '/docs',
    image: '/img/theatria/documentation.jpg',
  },
  {
    slug: 'regions',
    eyebrow: 'Places',
    title: 'Regions',
    description:
      'See the towns and districts behind the shops, with every listing grouped by place.',
    action: 'Explore regions',
    uri: '/search/regions',
    image: '/img/theatria/regions.jpg',
  },
  {
    slug: 'players',
    eyebrow: 'People',
    title: 'Players',
    description:
      'Find a player, then browse the chest shops and listings they own.',
    action: 'Browse players',
    uri: '/search/players',
    image: '/img/theatria/players.jpg',
  },
];

export const Home = () => {
  return (
    <main id='backdrop'>
      <section id='intro' aria-labelledby='home-title'>
        <picture className='hero-media'>
          <img
            src='/img/theatria/hero-theatria-1280.jpg'
            srcSet='/img/theatria/hero-theatria-1280.jpg 1280w, /img/theatria/hero-theatria-2560.jpg 2560w'
            sizes='100vw'
            width='2560'
            height='1431'
            alt=''
            loading='eager'
            decoding='async'
          />
        </picture>

        <div className='hero-shade' aria-hidden='true' />

        <div className='container hero-content'>
          <p className='hero-kicker'>Theatria&apos;s player marketplace</p>
          <h1 id='home-title'>Theatria ShopDB</h1>
          <p className='hero-lede'>
            Find the right trade without crossing the whole world. Search chest
            shops, compare listings, and see who is selling where.
          </p>

          <div className='hero-actions'>
            <Link
              to='/search/chest-shops'
              className='hero-button hero-button-primary'
            >
              Search chest shops
              <span aria-hidden='true'>&rarr;</span>
            </Link>
            <Link
              to='/search/regions'
              className='hero-button hero-button-secondary'
            >
              Explore regions
            </Link>
          </div>
        </div>

        <p className='hero-capture' aria-hidden='true'>
          Captured in Theatria
        </p>
      </section>

      <section id='features' aria-labelledby='features-title'>
        <div className='container'>
          <header className='features-intro'>
            <div>
              <p className='section-kicker'>Browse your way</p>
              <h2 id='features-title'>The market, mapped back to the world.</h2>
            </div>
            <p className='features-lede'>
              Start with an item, a place, or a player. ShopDB keeps the paths
              between them close at hand.
            </p>
          </header>

          <div className='feature-grid'>
            {features.map((feature, index) => (
              <Link
                to={feature.uri}
                className={`feature feature-${feature.slug}`}
                key={feature.slug}
              >
                <img
                  className='feature-media'
                  src={feature.image}
                  alt=''
                  width='1280'
                  height={feature.slug === 'regions' ? '715' : '782'}
                  loading='lazy'
                  decoding='async'
                />
                <span className='feature-shade' aria-hidden='true' />
                <div className='feature-copy'>
                  <div className='feature-meta'>
                    <span>{String(index + 1).padStart(2, '0')}</span>
                    <span>{feature.eyebrow}</span>
                  </div>
                  <div className='feature-text'>
                    <h3>{feature.title}</h3>
                    <p className='feature-description'>{feature.description}</p>
                    <span className='feature-action'>
                      {feature.action}
                      <span aria-hidden='true'>&rarr;</span>
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
};
