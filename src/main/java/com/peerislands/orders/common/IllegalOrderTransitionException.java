package com.peerislands.orders.common;

public class IllegalOrderTransitionException extends RuntimeException {
    public IllegalOrderTransitionException(String message) {
        super(message);
    }
}
