package com.playtheatria.shopdb.updater;

import java.util.UUID;

/** Lands claim data reduced to the fields ShopDB needs. */
public record PlayerShopClaim(UpdaterRegion region, UUID ownerId) {
}
