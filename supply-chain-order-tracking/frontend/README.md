# Frontend

Client-facing web app and tracking interface. The Supabase/Auth setup,
environment variables, integration scripts, and frontend handoff are
documented in [the project README](../README.md).

## How to run it

1. Open a terminal inside the `frontend` folder
2. Copy `.env.example` to `.env` and fill in `VITE_SUPABASE_URL` and
   `VITE_SUPABASE_PUBLISHABLE_KEY` (the shared Supabase project values).
   Don't commit `.env`
3. `npm install`
4. `npm run dev`
5. Open the address it prints (usually http://localhost:5173)

If port 5173 is already in use, Vite picks the next one (5174) and prints it.
Restart the dev server after creating or changing `.env`.

## What the app does right now

- **Login / register** (`src/Auth.jsx`): sign up with name, email, password and a
  role (buyer, wholesaler, logistics provider). You have to be logged in to see
  anything, because the `orders` table gives no access to logged-out users
- **App.jsx**: shows the login page when logged out, and the tracker when logged
  in, with a "Signed in as" bar and a sign out button
- **Tracker** (`src/Tracker.jsx`): the 4 tabs
  - **Place Order**: saves a real order in Supabase (buyer accounts only)
  - **Customer Tracking**: loads the logged-in user's real orders
  - **Vendor Portal** and **Verify**: still on mock data until the backend and
    smart contract are connected

## How the tracker maps to the database

The tracker form and the `orders` table don't match 1 to 1, so `Tracker.jsx`
translates between them:

| Tracker | `orders` table |
|---|---|
| Destination | `delivery_address` |
| Customer name, Origin | saved together in `notes` |
| Order id (SC-XXXXXXXX) | first 8 characters of the order `id` |
| Status | not in the database, a real order always shows "Order placed" |
| Product / quantity | not in the form, placeholders ("General goods", 1) are used |

Order status will come from the blockchain later.

## Still to do

- Connect Vendor Portal and Verify to the backend / smart contract
- Decide on the statuses (the tracker has 4, the contract has 3)
- Real product and quantity fields on Place Order, or extra columns in the database
