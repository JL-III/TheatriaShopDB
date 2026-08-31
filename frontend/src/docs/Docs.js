import React from 'react'
import './docs.css'


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
                    <div className='text pt-3'>
                        <b>Interactions include:</b>
                        <ul className='ul'>
                            <li>A chest shop is created</li>
                            <li>A chest shop is opened and closed</li>
                            <li>A chest shop is used</li>
                            <li>A chest shop is destroyed</li>
                        </ul>
                    </div>
                    <p className='text pt-3'>
                        After each interaction, the chest shop information (including the shop owner and shop location)
                        is sent to and updated in ShopDB in specified intervals. <b>By default, shops are updated every ten minutes.</b>
                    </p>

                    <hr className='hr' />
                </section>

                <section id='usage' className='container'>
                    <h1 className='title'>Usage</h1>
                    <p className='desc pt-5 pb-5'>Which chest shops are available in ShopDB?</p>
                    <p className='text'>
                        ShopDB includes server market stalls and player shops on listed lands.
                    </p>
                    <p className='text pt-3'>
                        <b>Market stalls</b> are located at <span className='code'>/market</span> and are managed by
                        server administrators. Players do not need to list or unlist market stalls.
                    </p>
                    <p className='text pt-3'>
                        <b>Player shops</b> can be listed by the land owner. Stand inside a land you own and run{' '}
                        <span className='code'>/shopdb list</span>. Only the owner of that land can list it;
                        trusted members and other non-owners cannot make its shops public. To remove the land
                        from ShopDB, its owner can stand inside it and run <span className='code'>/shopdb unlist</span>.
                    </p>
                    <p className='text pt-3'>
                        ShopDB starts refreshing the land&apos;s chest shops after it is listed. Normal chest-shop
                        interactions continue to keep those listings up to date.
                    </p>
                    <hr className='hr' />
                </section>

                <section id='equality' className='container'>
                    <h1 className='title'>Equality</h1>
                    <p className='desc pt-5 pb-5'>Which chest shops show up first?</p>
                    <p className='text'>
                        The sorting criteria selected determines the order in which shops are listed.
                        <i> Best price</i>, selected by default, sorts chest shops by the best value (cheapest per individual item for sale, or
                        most money offered per individual item a shop is buying). <b>However,</b> many times players trade items for the same price as others.
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
                        enabled by default, filters out chest shops that have the same item, owner, shop location, and price. This
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
                        If you have an idea or find a bug, please open a <span className='code'>#staff-support-ticket</span> in Discord.
                    </p>
                </section>
            </div>
        </>
    )
}
