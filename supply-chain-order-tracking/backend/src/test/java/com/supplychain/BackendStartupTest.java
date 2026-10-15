package com.supplychain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendStartupTest {
    @Autowired TestRestTemplate http;

    @Test void startsAndRejectsUnauthenticatedOrdersRequest() {
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/orders", String.class).getStatusCode());
    }
}
