## Supabase Setup

Use the shared Supabase project for this repository.

Copy `frontend/.env.example` to `frontend/.env` and set:

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

using the shared project values.

The database schema, permissions, triggers, and Realtime configuration have already been set up in Supabase. `database/schema.sql` contains the current database definition for reference and reproducibility.

Keep `.env` local and do not commit it.

## User roles

The supported roles are `buyer`, `wholesaler`, and `logistics_provider`. Registration sends the selected role and profile metadata, including a full name. The auth trigger creates the corresponding `public.profiles` row.

## Frontend Supabase usage

Use [frontend/src/lib/supabase.js](frontend/src/lib/supabase.js) for Supabase access. The frontend needs register, login, logout, session handling, profile retrieval, role-based UI, and order creation and display.

## Order data

Supabase stores users and profiles, buyer, wholesaler, and logistics-provider assignments, product and delivery metadata, and each order's `blockchain_order_id`, `blockchain_tx_hash`, and `contract_address`. Blockchain status stays authoritative on-chain; there is no Supabase status field.

## Blockchain integration

Buyer creates an off-chain order -> a blockchain order is created -> its order ID, transaction hash, and contract address are saved to Supabase -> a logistics provider can be assigned -> an authorized blockchain operator can update the on-chain order status.

The local Hardhat node is temporary. For local blockchain development, set `LOCAL_RPC_URL` and `LOCAL_CONTRACT_ADDRESS` in `frontend/.env`. If the Hardhat node restarts, redeploy the contract and update `LOCAL_CONTRACT_ADDRESS`.

## Realtime

`public.orders` is enabled for Supabase Realtime. The frontend can subscribe to order changes for authenticated users who have access to those orders.

## Test scripts

The `frontend/test-*.mjs` files verified the integration and are optional reference examples. Teammates do not need to run them for normal frontend development; inspect them for Supabase Auth, order creation, blockchain linking, logistics assignment, or Realtime examples.

## Important files

- [database/schema.sql](database/schema.sql) — database schema, RLS, triggers, grants
- [frontend/src/lib/supabase.js](frontend/src/lib/supabase.js) — Supabase client
- [frontend/.env.example](frontend/.env.example) — environment variable template
- `frontend/test-*.mjs` — optional integration/reference scripts
- [smart_contracts/contracts/SupplyChainOrder.sol](smart_contracts/contracts/SupplyChainOrder.sol) — blockchain order contract
