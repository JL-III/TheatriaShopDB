package com.playtheatria.shopdb.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopRescannerFollowUpRequestTest {

    @Test
    void overlappingRequestsCoalesceIntoExactlyOneFollowUp() {
        ShopRescanner.FollowUpRequest request = new ShopRescanner.FollowUpRequest();

        request.queue();
        request.queue();
        request.queue();

        assertTrue(request.consume());
        assertFalse(request.consume());
    }

    @Test
    void cancellationClearsQueuedFollowUp() {
        ShopRescanner.FollowUpRequest request = new ShopRescanner.FollowUpRequest();

        request.queue();
        request.clear();

        assertFalse(request.consume());

        // A publication request arriving while the cancelled scan winds down
        // is newer than the cancellation and must still be honored.
        request.queue();
        assertTrue(request.consume());
    }
}
