package com.peerislands.orders.orders;

import com.peerislands.orders.common.IllegalOrderTransitionException;

public class IllegalOrderStateException extends IllegalOrderTransitionException {
    public IllegalOrderStateException(String message) {
        super(message);
    }
}
