package com.peerislands.orders.inventory;

import java.util.UUID;

public interface ReservationLookup {
    boolean hasActiveReservation(UUID productId);
}
