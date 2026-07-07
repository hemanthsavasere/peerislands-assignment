package com.peerislands.orders.inventory;

import com.peerislands.orders.orders.OrderService;
import com.peerislands.orders.orders.dto.CreateOrderItemRequest;
import com.peerislands.orders.orders.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orderService;
    @Autowired InventoryService inventoryService;

    private String createProductJson(String sku) {
        return "{\"name\":\"Widget\",\"sku\":\"" + sku + "\",\"availableStock\":10,\"unitPrice\":9.99}";
    }

    @Test
    void createReturns201AndLocation() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductJson("W-1")))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists())
           .andExpect(jsonPath("$.sku").value("W-1"));
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(createProductJson("W-DUP")))
           .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(createProductJson("W-DUP")))
           .andExpect(status().isConflict());
    }

    @Test
    void validationErrorsReturn400() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"sku\":\"\",\"availableStock\":-1,\"unitPrice\":-1}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns200() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(createProductJson("W-L")));
        mvc.perform(get("/api/v1/products"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getByIdReturns200() throws Exception {
        String body = mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(createProductJson("W-G")))
                         .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/v1/products/" + id))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        mvc.perform(get("/api/v1/products/00000000-0000-0000-0000-000000000000"))
           .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns200() throws Exception {
        String body = mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(createProductJson("W-U")))
                         .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(put("/api/v1/products/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Widget Pro\",\"availableStock\":50,\"unitPrice\":12.50}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("Widget Pro"))
           .andExpect(jsonPath("$.availableStock").value(50));
    }

    @Test
    void deleteBlockedWhenActiveReservationExists() throws Exception {
        String created = mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                            .content(createProductJson("W-RES"))).andReturn().getResponse().getContentAsString();
        String pid = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName("Alice");
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSku("W-RES");
        item.setQuantity(1);
        req.setItems(List.of(item));
        orderService.create(req);

        mvc.perform(delete("/api/v1/products/" + pid))
           .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204() throws Exception {
        String body = mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(createProductJson("W-D")))
                         .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(delete("/api/v1/products/" + id))
           .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/products/" + id))
           .andExpect(status().isNotFound());
    }
}
