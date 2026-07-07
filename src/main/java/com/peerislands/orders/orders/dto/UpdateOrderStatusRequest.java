package com.peerislands.orders.orders.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusRequest {
    @NotNull
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
