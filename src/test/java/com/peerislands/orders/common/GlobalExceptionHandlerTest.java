package com.peerislands.orders.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {
    @Autowired
    MockMvc mvc;

    @Test
    void resourceNotFoundMapsTo404() throws Exception {
        mvc.perform(get("/probe/not-found"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.status").value(404))
           .andExpect(jsonPath("$.error").exists())
           .andExpect(jsonPath("$.message").exists())
           .andExpect(jsonPath("$.path").value("/probe/not-found"))
           .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void illegalTransitionMapsTo400() throws Exception {
        mvc.perform(get("/probe/illegal-transition"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void insufficientStockMapsTo409() throws Exception {
        mvc.perform(get("/probe/insufficient-stock"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void duplicateSkuMapsTo409() throws Exception {
        mvc.perform(get("/probe/duplicate-sku"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void activeReservationMapsTo409() throws Exception {
        mvc.perform(get("/probe/active-reservation"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void validationErrorsMapsTo400() throws Exception {
        mvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unexpectedExceptionMapsTo500() throws Exception {
        mvc.perform(get("/probe/unexpected"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.status").value(500));
    }
}
