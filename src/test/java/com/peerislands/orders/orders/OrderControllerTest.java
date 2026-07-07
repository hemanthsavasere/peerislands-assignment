package com.peerislands.orders.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peerislands.orders.inventory.InventoryService;
import com.peerislands.orders.inventory.dto.CreateProductRequest;
import com.peerislands.orders.orders.dto.CreateOrderItemRequest;
import com.peerislands.orders.orders.dto.CreateOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @Autowired InventoryService inventoryService;
    @Autowired ObjectMapper objectMapper;

    private String productSku;

    @BeforeEach
    void seedProduct() {
        CreateProductRequest r = new CreateProductRequest();
        r.setName("Widget");
        r.setSku("W-1");
        r.setAvailableStock(10);
        r.setUnitPrice(new BigDecimal("5.00"));
        inventoryService.create(r);
        productSku = "W-1";
    }

    private String orderJson(int qty) throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName("Alice");
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSku(productSku);
        item.setQuantity(qty);
        req.setItems(List.of(item));
        return objectMapper.writeValueAsString(req);
    }

    @Test
    void createReturns201() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(2)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists())
           .andExpect(jsonPath("$.status").value("PENDING"))
           .andExpect(jsonPath("$.items[0].productName").value("Widget"));
    }

    @Test
    void unknownSkuReturns404() throws Exception {
        String body = "{\"customerName\":\"Bob\",\"items\":[{\"productSku\":\"NOPE\",\"quantity\":1}]}";
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isNotFound());
    }

    @Test
    void insufficientStockReturns409() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(999)))
           .andExpect(status().isConflict());
    }

    @Test
    void emptyItemsReturns400() throws Exception {
        String body = "{\"customerName\":\"Bob\",\"items\":[]}";
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturns200() throws Exception {
        String created = mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(1)))
                            .andReturn().getResponse().getContentAsString();
        String id = extractOrderId(created);
        mvc.perform(get("/api/v1/orders/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        mvc.perform(get("/api/v1/orders/00000000-0000-0000-0000-000000000000"))
           .andExpect(status().isNotFound());
    }

    @Test
    void listWithFilterReturns200() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(1)));
        mvc.perform(get("/api/v1/orders?status=PENDING"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void listWithUnknownStatusReturns400() throws Exception {
        mvc.perform(get("/api/v1/orders?status=GARBLED"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void putStatusProcessingReturns200() throws Exception {
        String created = mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(1)))
                            .andReturn().getResponse().getContentAsString();
        String id = extractOrderId(created);
        mvc.perform(put("/api/v1/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void putStatusCancelledReturns400() throws Exception {
        String created = mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(1)))
                            .andReturn().getResponse().getContentAsString();
        String id = extractOrderId(created);
        mvc.perform(put("/api/v1/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void putStatusPendingReturns400() throws Exception {
        String created = mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(1)))
                            .andReturn().getResponse().getContentAsString();
        String id = extractOrderId(created);
        mvc.perform(put("/api/v1/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void cancelReturns200() throws Exception {
        String created = mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(1)))
                            .andReturn().getResponse().getContentAsString();
        String id = extractOrderId(created);
        mvc.perform(post("/api/v1/orders/" + id + "/cancel"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelNotFoundReturns404() throws Exception {
        mvc.perform(post("/api/v1/orders/00000000-0000-0000-0000-000000000000/cancel"))
           .andExpect(status().isNotFound());
    }

    private String extractOrderId(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asText();
    }
}
