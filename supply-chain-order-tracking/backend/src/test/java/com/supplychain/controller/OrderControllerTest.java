package com.supplychain.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import java.util.UUID;
import com.supplychain.service.OrderWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrderControllerTest {
    private static final UUID ID = UUID.fromString("12345678-1234-4234-8234-123456789abc");

    @Test void rejectsMissingTokenBeforeLink() throws Exception {
        OrderWorkflow workflow = mock(OrderWorkflow.class);
        MockMvcBuilders.standaloneSetup(new OrderController(workflow)).build()
                .perform(post("/api/orders/{id}/link", ID)).andExpect(status().isUnauthorized());
        verifyNoInteractions(workflow);
    }

    @Test void forwardsBearerTokenAndOrderIdToWorkflow() throws Exception {
        OrderWorkflow workflow = mock(OrderWorkflow.class);
        when(workflow.link("valid-token", ID)).thenReturn(Map.of("blockchain_order_id", 1));
        MockMvcBuilders.standaloneSetup(new OrderController(workflow)).build()
                .perform(post("/api/orders/{id}/link", ID).header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.blockchain_order_id").value(1));
        verify(workflow).link("valid-token", ID);
    }
}
