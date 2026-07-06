package com.peerislands.orders.common;

public class ActiveReservationException extends RuntimeException {
    public ActiveReservationException(String message) {
        super(message);
    }
}
