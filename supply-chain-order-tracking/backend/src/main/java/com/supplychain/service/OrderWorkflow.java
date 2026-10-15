package com.supplychain.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderWorkflow {
    private final SupabaseService supabase;
    private final BlockchainGateway chain;
    private final OrderMetadata metadata;

    public OrderWorkflow(SupabaseService supabase, BlockchainGateway chain, OrderMetadata metadata) {
        this.supabase = supabase;
        this.chain = chain;
        this.metadata = metadata;
    }

    private static void requireRole(SupabaseService.User user, String role) {
        if (!role.equals(user.role())) throw new SecurityException(role + " role required");
    }
    private static void requireBuyer(SupabaseService.User user, Map<String, Object> row) {
        requireRole(user, "buyer");
        if (!user.id().equals(row.get("buyer_id"))) throw new SecurityException("Order belongs to a different buyer");
    }
    private static BigInteger chainId(Map<String, Object> row) {
        return new BigInteger(String.valueOf(row.get("blockchain_order_id")));
    }
    private static boolean linked(Map<String, Object> row) { return row.get("blockchain_order_id") != null; }
    private static void completeReferences(Map<String, Object> row) {
        int present = (row.get("blockchain_order_id") != null ? 1 : 0)
                + (row.get("blockchain_tx_hash") != null ? 1 : 0)
                + (row.get("contract_address") != null ? 1 : 0);
        if (present != 0 && present != 3) throw new IllegalStateException("Order has incomplete blockchain references");
    }
    private void trustedReference(Map<String, Object> row) {
        if (!chain.contractAddress().equalsIgnoreCase(String.valueOf(row.get("contract_address"))))
            throw new IllegalStateException("Order references an untrusted contract address");
        String transactionHash = String.valueOf(row.get("blockchain_tx_hash"));
        if (!transactionHash.matches("0x[0-9a-fA-F]{64}")) throw new IllegalStateException("Order has an invalid transaction hash");
    }
    private static void matchOrder(BlockchainGateway.ChainOrder chainOrder, BigInteger id, String hash, String buyerWallet) {
        if (!chainOrder.id().equals(id) || !chainOrder.metadataHash().equalsIgnoreCase(hash) ||
                !chainOrder.customer().equalsIgnoreCase(buyerWallet))
            throw new IllegalStateException("Blockchain order does not match the Supabase order");
    }

    public synchronized Map<String, Object> link(String token, UUID id) throws Exception {
        SupabaseService.User buyer = supabase.authenticate(token);
        Map<String, Object> row = supabase.getOrder(token, id);
        requireBuyer(buyer, row);
        completeReferences(row);
        if (linked(row)) {
            trustedReference(row);
            String currentHash = metadata.hash(row);
            BlockchainGateway.Created origin = chain.recoverOrder(chainId(row), currentHash);
            if (!origin.transactionHash().equalsIgnoreCase((String) row.get("blockchain_tx_hash")))
                throw new IllegalStateException("Existing blockchain transaction reference does not match");
            return viewRow(row);
        }
        if (buyer.walletAddress() == null || !buyer.walletAddress().matches("0x[0-9a-fA-F]{40}") ||
                buyer.walletAddress().equalsIgnoreCase("0x0000000000000000000000000000000000000000"))
            throw new IllegalArgumentException("Buyer profile needs a valid nonzero wallet address");

        String hash = metadata.hash(row);
        BigInteger existing = chain.findByHash(hash);
        BlockchainGateway.Created created = existing.signum() > 0
                ? chain.recoverOrder(existing, hash)
                : chain.createOrder(buyer.walletAddress(), hash);
        matchOrder(created.order(), created.order().id(), hash, buyer.walletAddress());
        if (!created.transactionHash().matches("0x[0-9a-fA-F]{64}"))
            throw new IllegalStateException("Blockchain receipt has no valid transaction hash");

        Map<String, Object> latest = supabase.getOrder(token, id);
        requireBuyer(buyer, latest);
        completeReferences(latest);
        if (linked(latest)) {
            trustedReference(latest);
            if (!chainId(latest).equals(created.order().id()))
                throw new IllegalStateException("Order was linked to a different blockchain record");
            if (!created.transactionHash().equalsIgnoreCase((String) latest.get("blockchain_tx_hash")))
                throw new IllegalStateException("Order was linked to a different blockchain transaction");
            return viewRow(latest);
        }
        if (!metadata.hash(latest).equalsIgnoreCase(hash))
            throw new IllegalStateException("Order metadata changed while blockchain linking was pending; retry requires manual resolution");
        long numericId = created.order().id().longValueExact();
        Map<String, Object> saved = supabase.linkOrder(token, id, String.valueOf(latest.get("updated_at")),
                numericId, created.transactionHash(), chain.contractAddress());
        return viewRow(saved);
    }

    public Map<String, Object> get(String token, UUID id) throws Exception {
        supabase.authenticate(token);
        return viewRow(supabase.getOrder(token, id));
    }

    public List<Map<String, Object>> list(String token) throws Exception {
        supabase.authenticate(token);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : supabase.listOrders(token)) {
            try { result.add(viewRow(row)); }
            catch (Exception error) {
                Map<String, Object> item = new LinkedHashMap<>(row);
                item.put("chainError", "Blockchain read failed; check the configured local node and contract.");
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> viewRow(Map<String, Object> row) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>(row);
        completeReferences(row);
        if (linked(row)) {
            trustedReference(row);
            BlockchainGateway.ChainOrder onChain = chain.getOrder(chainId(row));
            if (!onChain.id().equals(chainId(row))) throw new IllegalStateException("Blockchain order ID mismatch");
            result.put("onchain_status", onChain.status());
            result.put("onchain_timestamp", onChain.timestamp().toString());
        }
        return result;
    }

    public Map<String, Object> verify(String token, UUID id) {
        supabase.authenticate(token);
        try {
            Map<String, Object> row = supabase.getOrder(token, id);
            completeReferences(row);
            if (!linked(row)) return Map.of("status", "NOT_LINKED", "orderId", id.toString());
            trustedReference(row);
            BigInteger chainOrderId = chainId(row);
            BlockchainGateway.ChainOrder actual = chain.getOrder(chainOrderId);
            if (!actual.id().equals(chainOrderId)) throw new IllegalStateException("Blockchain order ID mismatch");
            BlockchainGateway.Created origin = chain.recoverOrder(chainOrderId, actual.metadataHash());
            if (!origin.transactionHash().equalsIgnoreCase((String) row.get("blockchain_tx_hash")))
                throw new IllegalStateException("Creation transaction hash does not match");
            String currentHash = metadata.hash(row);
            return Map.of("status", currentHash.equalsIgnoreCase(actual.metadataHash()) ? "VERIFIED" : "MISMATCH",
                    "currentHash", currentHash, "onchainHash", actual.metadataHash(),
                    "blockchainOrderId", chainOrderId.toString(), "onchainStatus", actual.status());
        } catch (Exception error) {
            return Map.of("status", "VERIFICATION_ERROR", "message", "Could not verify this order against its trusted blockchain references.");
        }
    }

    public Map<String, Object> update(String token, UUID id, int status) throws Exception {
        SupabaseService.User user = supabase.authenticate(token);
        requireRole(user, "logistics_provider");
        Map<String, Object> row = supabase.getOrder(token, id);
        completeReferences(row);
        if (!user.id().equals(row.get("logistics_provider_id")))
            throw new SecurityException("Logistics provider is not assigned to this order");
        if (!linked(row)) throw new IllegalStateException("Order is not linked to blockchain");
        trustedReference(row);
        BigInteger idOnChain = chainId(row);
        BlockchainGateway.ChainOrder before = chain.getOrder(idOnChain);
        if (!before.metadataHash().equalsIgnoreCase(metadata.hash(row)))
            throw new IllegalStateException("Order metadata does not match blockchain; refusing status update");
        BlockchainGateway.ChainOrder after = chain.updateStatus(idOnChain, status);
        if (!after.id().equals(idOnChain) || !after.metadataHash().equalsIgnoreCase(before.metadataHash()))
            throw new IllegalStateException("Blockchain status update did not preserve the order");
        return viewRow(row);
    }
}
