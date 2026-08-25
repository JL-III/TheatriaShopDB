import React from 'react'
import './docs.css'
import {Link} from "react-router-dom";
import { HashLink } from 'react-router-hash-link';


export const Docs = () => {
    return (
        <>
            <div id='backdrop' className='pb-100'>
                <section id='getting-started' className='container pt-100'>
                    <h1 className='intro'>Getting Started</h1>
                    <p className='desc pt-5 pb-5'>This page is an overview of the ShopDB documentation.</p>

                    <p className='text'>
                        <b>ShopDB</b> is a Minecraft plugin and web application that allows players to
                        easily search for chest shops to buy or sell from.
                    </p>

                    <hr className='hr'/>
                </section>

                <section id='integration' className='container'>
                    <h1 className='title'>Integration</h1>
                    <p className='desc pt-5 pb-5'>How is ShopDB updated?</p>
                    <p className='text'>
                        Each time a player <b>interacts</b> with a chest shop, the chest shop data is captured and sent to ShopDB.
                    </p>
                    <p className='text pt-3'>
                        <b>Interactions include:</b>
                        <ul className='ul'>
                            <li>A chest shop is created</li>
                            <li>A chest shop is opened and closed</li>
                            <li>A chest shop is used</li>
                            <li>A chest shop is destroyed</li>
                        </ul>
                    </p>
                    <p className='text pt-3'>
                        After each interaction, the chest shop information (including the shop owner and region information)
                        is sent to and updated in ShopDB in specified intervals. <b>By default, shops are updated every ten minutes.</b>
                    </p>

                    <hr className='hr' />
                </section>

                <section id='usage' className='container'>
                    <h1 className='title'>Usage</h1>
                    <p className='desc pt-5 pb-5'>How can I add or remove my chest shops from ShopDB?</p>
                    <p className='text pt-3'>
                        <b>By default, all chest shops are hidden.</b> Two criteria must be met for chest shops to be shown:
                        <ul className='ul pt-3'>
                            <li>The chest shop must be in a WorldGuard region.</li>
                            <li>A region owner has <b>listed</b> the region.</li>
                        </ul>
                    </p>
                    <p className='text pt-3'>To check if a chest shop is in a region, go to the chest shop in-game and type: {' '}
                        <span className='code'>/rg i</span>.
                    </p>
                    <p className='text pt-3'>
                        To see if a region is listed, visit the <Link to='/search/regions' className='link'>regions</Link> page and search for it.
                        Under the region name is an indication of whether or not it is listed. If the region
                        is not displayed, ensure you have unchecked the 'Hide Unlisted Towns' option.
                    </p>
                    <p className='text pt-3'>
                        It is also possible that the region is not yet in ShopDB. Regions are now sent to or updated
                        in ShopDB after a chest shop within it has been <HashLink to='/docs#integration' className='link'>interacted</HashLink> with,{' '}
                        <b>or</b> after the region has been listed.
                    </p>
                </section>

                <section id='listing-regions' className='container pt-50'>
                    <h1 className='subtitle'>Listing a region</h1>
                    <p className='text pt-3'>
                        To list a region, run the command in-game <span className='code'>/shopdb <b>list</b> {`<region-name>`}</span>.
                        You <b>must be an owner</b> to list the region. After a region is listed, <b>all</b> chest shops
                        within it will be visible on ShopDB.
                    </p>
                </section>

                <section id='unlisting-regions' className='container pt-50'>
                    <h1 className='subtitle'>Unlisting a region</h1>
                    <p className='text pt-3'>
                        To unlist a region, run the command in-game <span className='code'>/shopdb <b>unlist</b> {`<region-name>`}</span>.
                        You <b>must be an owner</b> to unlist the region. After a region is unlisted, <b>all</b> chest shops
                        within it will be hidden on ShopDB.
                    </p>
                    <hr className='hr' />
                </section>

                <section id='subregions' className='container'>
                    <h1 className='title'>Subregions</h1>
                    <p className='desc pt-5 pb-5'>How are overlapping regions handled?</p>
                    <p className='text'>
                        Sometimes regions overlap. For example, all the chest shops in the market are also in spawn.
                        In ShopDB, chest shops are linked to only one region. In this case, the following criteria is used
                        to select which region the shop likely belongs to:

                        <ol className='ul pt-3'>
                            <li>The region which is listed will be linked.</li>
                            <li>If no region is listed, or both are listed, the <b>smallest</b> region will be chosen.</li>
                        </ol>
                    </p>
                    <p className='text pt-3'>
                        In most cases, this selection criteria results in the correct region being linked. However,
                        if a region is chosen incorrectly, this can be resolved by the region owners by ensuring that only
                        the correct region is listed. Then, chest shops will be correctly linked after{' '}
                        <HashLink to='/docs#integration' className='link'>interactions</HashLink> occur.
                    </p>
                    <hr className='hr' />
                </section>

                <section id='equality' className='container'>
                    <h1 className='title'>Equality</h1>
                    <p className='desc pt-5 pb-5'>Which chest shops show up first?</p>
                    <p className='text'>
                        The sorting criteria selected determines the order in which shops are listed.
                        <i> Best price</i>, selected by default, sorts chest shops by the best value (cheapest per individual item sold, or
                        most money offered per individual item purchased). <b>However,</b> many times players trade items for the same price as others.
                    </p>
                    <p className='text pt-3'>
                        In the case where the sorting criteria results in a tie, the results are randomized.
                        This way, no chest shops are favored or listed higher than other chest shops.
                    </p>
                    <hr className='hr' />
                </section>

                <section id='uniqueness' className='container'>
                    <h1 className='title'>Uniqueness</h1>
                    <p className='desc pt-5 pb-5'>Where are my other 100 dirt chest shops?</p>
                    <p className='text'>
                        To avoid filling search results with redundant chest shops, <i>hide identical shops</i>,
                        enabled by default, filters out chest shops that have the same item, owner, region, and price. This
                        can be disabled by unchecking the filter.
                    </p>
                    <p className='text pt-3'>
                        When the filter is enabled, the <b>most available</b> chest shop will be the unique shop selected
                        and shown. When purchasing, this prioritizes shops with greater quantity. When selling, this prioritizes
                        shops with the greatest space available.
                    </p>
                    <hr className='hr' />
                </section>

                <section id='feedback' className='container'>
                    <h1 className='title'>Feedback</h1>
                    <p className='desc pt-5 pb-5'>For bugs and suggestions</p>
                    <p className='text'>
                        ShopDB has evolved over the past two years, thanks to the support of the community.
                        If you have an idea, or find a bug, please share it on Discord in the <span className='code'>suggestions-discussion</span>
                        channel and tag <span className='code'>@bfrisco</span>
                    </p>
                </section>
            </div>
        </>
    )
}