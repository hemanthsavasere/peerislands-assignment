package com.peerislands.orders.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/probe")
@Profile("test")
public class ProbeController {
    @GetMapping("/not-found")
    public void notFound() {
        throw new ResourceNotFoundException("not found probe");
    }

    @GetMapping("/illegal-transition")
    public void illegalTransition() {
        throw new IllegalOrderTransitionException("bad transition probe");
    }

    @GetMapping("/insufficient-stock")
    public void insufficientStock() {
        throw new InsufficientStockException("insufficient stock probe");
    }

    @GetMapping("/duplicate-sku")
    public void duplicateSku() {
        throw new DuplicateSkuException("duplicate sku probe");
    }

    @GetMapping("/active-reservation")
    public void activeReservation() {
        throw new ActiveReservationException("active reservation probe");
    }

    @PostMapping("/validation")
    public void validation(@Valid @RequestBody ProbeBody body) {
    }

    @GetMapping("/unexpected")
    public void unexpected() {
        throw new IllegalStateException("unexpected probe");
    }

    public static class ProbeBody {
        @NotBlank
        private String value;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
