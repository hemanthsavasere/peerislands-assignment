package com.peerislands.orders.inventory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test")
public class NoOpReservationLookup implements ReservationLookup {
    @Override
    public boolean hasActiveReservation(UUID productId) {
        return false;
    }
}
