package com.example.inventory.controller;

import com.example.inventory.model.ItemRequest;
import com.example.inventory.model.ItemResponse;
import com.example.inventory.repository.ItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class ItemApiIntegrationTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:10.11")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("Successfully create and then retrieve an item")
    void createAndGetItemTest() throws Exception {
        // 1. Create Item
        ItemRequest request = new ItemRequest();
        request.setName("Test Laptop");
        request.setCategory("Electronics");
        request.setStatus("In Stock");
        request.setLocation("Office A");

        String jsonResponse = mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Laptop"))
                .andReturn().getResponse().getContentAsString();

        ItemResponse response = objectMapper.readValue(jsonResponse, ItemResponse.class);
        Long id = response.getId();

        // 2. Get Item by ID
        mockMvc.perform(get("/api/v1/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Laptop"))
                .andExpect(jsonPath("$.category").value("Electronics"));
    }

    @Test
    @DisplayName("Should return 404 with custom error code when item not found")
    void itemNotFoundTest() throws Exception {
        mockMvc.perform(get("/api/v1/items/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("I001"))
                .andExpect(jsonPath("$.errorMessage").value(containsString("Item not found")));
    }

    @Test
    @DisplayName("Should return 400 when creating item without required name")
    void validationErrorTest() throws Exception {
        ItemRequest invalidRequest = new ItemRequest(); // Missing name
        
        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("C001"))
                .andExpect(jsonPath("$.errorDetails[0].field").value("name"));
    }
}
