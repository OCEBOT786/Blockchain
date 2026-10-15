package com.supplychain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderWorkflowTest {
    private static final UUID ID = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    private static final String BUYER = "87654321-4321-4321-8321-cba987654321";
    private static final String LOGISTICS = "97654321-4321-4321-8321-cba987654321";
    private static final String WALLET = "0x1111111111111111111111111111111111111111";
    private static final String CONTRACT = "0x2222222222222222222222222222222222222222";
    private static final String TX = "0x" + "aa".repeat(32);
    private SupabaseService supabase;
    private BlockchainGateway chain;
    private OrderMetadata metadata;
    private OrderWorkflow workflow;

    @BeforeEach void setup() {
        supabase = mock(SupabaseService.class);
        chain = mock(BlockchainGateway.class);
        metadata = new OrderMetadata();
        workflow = new OrderWorkflow(supabase, chain, metadata);
        when(chain.contractAddress()).thenReturn(CONTRACT);
    }

    private Map<String, Object> row() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", ID.toString()); row.put("buyer_id", BUYER);
        row.put("logistics_provider_id", LOGISTICS);
        row.put("product_name", "Coffee Beans"); row.put("product_description", "Premium coffee");
        row.put("quantity", 20); row.put("delivery_address", "123 Test Street, Auckland");
        row.put("blockchain_order_id", null); row.put("blockchain_tx_hash", null);
        row.put("contract_address", null); row.put("updated_at", "2026-10-16T00:00:00Z");
        return row;
    }
    private Map<String, Object> linked(Map<String, Object> row) {
        Map<String, Object> linked = new HashMap<>(row);
        linked.put("blockchain_order_id", 1);
        linked.put("blockchain_tx_hash", TX);
        linked.put("contract_address", CONTRACT);
        return linked;
    }
    private BlockchainGateway.ChainOrder chainOrder(Map<String, Object> row, int status) {
        return new BlockchainGateway.ChainOrder(BigInteger.ONE, WALLET, status, BigInteger.TEN, metadata.hash(row));
    }

    @Test void buyerCreatesAndLinksVerifiedChainRecord() throws Exception {
        var row = row(); var linked = linked(row); var actual = chainOrder(row, 0);
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row, row);
        when(chain.findByHash(metadata.hash(row))).thenReturn(BigInteger.ZERO);
        when(chain.createOrder(WALLET, metadata.hash(row))).thenReturn(new BlockchainGateway.Created(actual, TX));
        when(supabase.linkOrder(eq("token"), eq(ID), anyString(), eq(1L), eq(TX), eq(CONTRACT))).thenReturn(linked);
        when(chain.getOrder(BigInteger.ONE)).thenReturn(actual);
        assertEquals(1, workflow.link("token", ID).get("blockchain_order_id"));
        verify(chain).createOrder(WALLET, metadata.hash(row));
        verify(supabase).linkOrder(eq("token"), eq(ID), anyString(), eq(1L), eq(TX), eq(CONTRACT));
    }

    @Test void rejectsWrongBuyerBeforeBlockchainCall() throws Exception {
        var row = row();
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(LOGISTICS, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row);
        assertThrows(SecurityException.class, () -> workflow.link("token", ID));
        verify(chain, never()).createOrder(anyString(), anyString());
        verify(chain, never()).findByHash(anyString());
    }

    @Test void recoversExistingChainOrderInsteadOfSubmittingDuplicate() throws Exception {
        var row = row(); var actual = chainOrder(row, 0);
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row, row);
        when(chain.findByHash(metadata.hash(row))).thenReturn(BigInteger.ONE);
        when(chain.recoverOrder(BigInteger.ONE, metadata.hash(row))).thenReturn(new BlockchainGateway.Created(actual, TX));
        when(supabase.linkOrder(eq("token"), eq(ID), anyString(), eq(1L), eq(TX), eq(CONTRACT))).thenReturn(linked(row));
        when(chain.getOrder(BigInteger.ONE)).thenReturn(actual);
        assertEquals(1, workflow.link("token", ID).get("blockchain_order_id"));
        verify(chain, never()).createOrder(anyString(), anyString());
    }

    @Test void rejectsChangedMetadataBeforeDatabaseLink() throws Exception {
        var row = row(); var changed = row(); changed.put("quantity", 21); var actual = chainOrder(row, 0);
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row, changed);
        when(chain.findByHash(metadata.hash(row))).thenReturn(BigInteger.ZERO);
        when(chain.createOrder(WALLET, metadata.hash(row))).thenReturn(new BlockchainGateway.Created(actual, TX));
        assertThrows(IllegalStateException.class, () -> workflow.link("token", ID));
        verify(supabase, never()).linkOrder(anyString(), any(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test void verifiesRealHashAndDetectsMismatch() throws Exception {
        var row = linked(row()); var actual = chainOrder(row, 1);
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row);
        when(chain.getOrder(BigInteger.ONE)).thenReturn(actual);
        when(chain.recoverOrder(BigInteger.ONE, actual.metadataHash())).thenReturn(new BlockchainGateway.Created(actual, TX));
        assertEquals("VERIFIED", workflow.verify("token", ID).get("status"));
        row.put("quantity", 21);
        assertEquals("MISMATCH", workflow.verify("token", ID).get("status"));
    }

    @Test void reportsUnlinkedAndIncompleteReferencesWithoutClaimingVerification() {
        var row = row();
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row);
        assertEquals("NOT_LINKED", workflow.verify("token", ID).get("status"));
        row.put("blockchain_tx_hash", TX);
        assertEquals("VERIFICATION_ERROR", workflow.verify("token", ID).get("status"));
        verifyNoInteractions(chain);
    }

    @Test void rejectsMismatchedCreationTransactionReference() throws Exception {
        var row = linked(row()); var actual = chainOrder(row, 0);
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "buyer", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row);
        when(chain.getOrder(BigInteger.ONE)).thenReturn(actual);
        when(chain.recoverOrder(BigInteger.ONE, actual.metadataHash())).thenReturn(
                new BlockchainGateway.Created(actual, "0x" + "bb".repeat(32)));
        assertEquals("VERIFICATION_ERROR", workflow.verify("token", ID).get("status"));
    }

    @Test void onlyAssignedLogisticsCanUpdateAndReadConfirmedStatus() throws Exception {
        var row = linked(row()); var before = chainOrder(row, 0); var after = chainOrder(row, 1);
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(LOGISTICS, "logistics_provider", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row);
        when(chain.getOrder(BigInteger.ONE)).thenReturn(before, after);
        when(chain.updateStatus(BigInteger.ONE, 1)).thenReturn(after);
        assertEquals(1, workflow.update("token", ID, 1).get("onchain_status"));
        verify(chain).updateStatus(BigInteger.ONE, 1);
    }

    @Test void unassignedLogisticsCannotTriggerTransaction() throws Exception {
        var row = linked(row());
        when(supabase.authenticate("token")).thenReturn(new SupabaseService.User(BUYER, "logistics_provider", WALLET));
        when(supabase.getOrder("token", ID)).thenReturn(row);
        assertThrows(SecurityException.class, () -> workflow.update("token", ID, 1));
        verify(chain, never()).updateStatus(any(), anyInt());
    }
}
