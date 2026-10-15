package com.supplychain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class SupabaseServiceTest {
    private static final String URL = "http://localhost:9999";
    private static final String ID = "87654321-4321-4321-8321-cba987654321";

    @Test void validatesTokenAndReadsRoleFromProtectedProfile() {
        RestTemplate http = new RestTemplate(new JdkClientHttpRequestFactory());
        MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
        server.expect(once(), requestTo(URL + "/auth/v1/user"))
                .andExpect(header("Authorization", "Bearer valid-token"))
                .andExpect(header("apikey", "public-test-key"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URL + "/rest/v1/profiles?select=id,role,wallet_address&id=eq." + ID))
                .andExpect(header("Authorization", "Bearer valid-token"))
                .andRespond(withSuccess("[{\"id\":\"" + ID + "\",\"role\":\"buyer\",\"wallet_address\":null}]", MediaType.APPLICATION_JSON));
        SupabaseService.User user = new SupabaseService(URL, "public-test-key", http).authenticate("valid-token");
        assertEquals(ID, user.id());
        assertEquals("buyer", user.role());
        server.verify();
    }

    @Test void rejectsInvalidTokenBeforeProfileQuery() {
        RestTemplate http = new RestTemplate(new JdkClientHttpRequestFactory());
        MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo(URL + "/auth/v1/user")).andRespond(withUnauthorizedRequest());
        assertThrows(HttpClientErrorException.class,
                () -> new SupabaseService(URL, "public-test-key", http).authenticate("invalid-token"));
        server.verify();
    }

    @Test void rejectsMalformedUrlWithoutExposingConfiguration() {
        String malformed = URL + "\nSECRET_MARKER=local-only";
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new SupabaseService(malformed, "public-test-key", new RestTemplate()).authenticate("token"));
        assertEquals("Supabase backend URL is invalid", error.getMessage());
    }
}
