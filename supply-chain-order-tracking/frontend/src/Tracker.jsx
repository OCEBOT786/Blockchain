import { useEffect, useState } from "react";
import { supabase } from "./lib/supabase";
import "./Tracker.css";

// same mock data as the html version — vendor portal + verify still run on
// this until the backend / smart contract side is ready. real orders from
// supabase get added on top of it (see loadRealOrders below)
const initialOrders = {
  "SC-10492": {
    customer: "Alex Kim",
    origin: "Hamilton, NZ",
    destination: "Auckland CBD",
    status: "Shipped",
    storedHash: "a91f7c3e9b2d",
    onchainHash: "a91f7c3e9b2d",
    history: [
      { status: "Order placed", time: "2026-09-28T09:14:00" },
      { status: "Packed", time: "2026-09-29T14:02:00" },
      { status: "Shipped", time: "2026-09-30T08:47:00" },
    ],
  },
  "SC-10493": {
    customer: "Priya Nair",
    origin: "Wellington, NZ",
    destination: "Christchurch",
    status: "Delivered",
    storedHash: "3b2e88d1f4a0",
    onchainHash: "9c4de77201aa",
    history: [
      { status: "Order placed", time: "2026-09-25T10:05:00" },
      { status: "Packed", time: "2026-09-25T16:40:00" },
      { status: "Shipped", time: "2026-09-26T07:22:00" },
      { status: "Delivered", time: "2026-09-27T11:58:00" },
    ],
  },
};

const STEPS = ["Order placed", "Packed", "Shipped", "Delivered"];

function formatTime(t) {
  const d = new Date(t);
  return d.toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function getTime(order, status) {
  const entry = order.history.find((h) => h.status === status);
  return entry ? formatTime(entry.time) : "";
}

// ---- supabase <-> tracker translation ----
// the orders table doesn't have customer / origin / status columns, so:
//  - customer + origin are saved in the "notes" column
//  - destination is the "delivery_address" column
//  - status isn't in the database (it'll come from the blockchain later),
//    so a real order just shows "Order placed" for now
// product_name and quantity are required by the database but the form
// doesn't ask for them, so these placeholders are used until we decide what to do
const PLACEHOLDER_PRODUCT = "General goods";
const PLACEHOLDER_QUANTITY = 1;

function displayId(uuid) {
  return "SC-" + uuid.slice(0, 8).toUpperCase();
}

function notesFromForm(customer, origin) {
  return "Customer: " + customer + "\nOrigin: " + origin;
}

function readNote(notes, key) {
  const match = (notes || "").match(new RegExp("^" + key + ": (.*)$", "m"));
  return match ? match[1] : "-";
}

function rowToOrder(row) {
  return {
    customer: readNote(row.notes, "Customer"),
    origin: readNote(row.notes, "Origin"),
    destination: row.delivery_address || "-",
    status: "Order placed",
    storedHash: "pending",
    onchainHash: "pending",
    history: [{ status: "Order placed", time: row.created_at }],
  };
}

export default function Tracker() {
  // in the html/js version this stuff lived in global `var`s and got pushed
  // into the page by hand with document.getElementById(...).textContent = ...
  // in react it's all "state" instead — you just update the state and the
  // page re-renders itself to match, no manual DOM poking
  const [orders, setOrders] = useState(initialOrders);
  const [activeTab, setActiveTab] = useState("customer");
  const [currentOrderId, setCurrentOrderId] = useState("SC-10492");

  // customer tab - search box
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [searchErr, setSearchErr] = useState("");

  // vendor tab
  const [vendorId, setVendorId] = useState("SC-10492");
  const [vendorStatus, setVendorStatus] = useState("Shipped");
  const [vendorMsg, setVendorMsg] = useState("");

  // place order tab
  const [placeName, setPlaceName] = useState("");
  const [placeOrigin, setPlaceOrigin] = useState("");
  const [placeDest, setPlaceDest] = useState("");
  const [placeMsg, setPlaceMsg] = useState("");

  // verify tab
  const [verifyId, setVerifyId] = useState("SC-10492");
  const [verifyResult, setVerifyResult] = useState(null);

  // load the logged-in user's real orders from supabase (newest first)
  useEffect(() => {
    let cancelled = false;

    supabase
      .from("orders")
      .select("id, delivery_address, notes, created_at")
      .order("created_at", { ascending: false })
      .then(({ data, error }) => {
        if (cancelled || error || !data || data.length === 0) return;

        const real = {};
        data.forEach((row) => {
          real[displayId(row.id)] = rowToOrder(row);
        });
        setOrders((old) => ({ ...old, ...real }));
        setCurrentOrderId(displayId(data[0].id));
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const currentOrder = orders[currentOrderId];

  function handleSearch() {
    const id = searchInput.trim().toUpperCase();
    if (!orders[id]) {
      setSearchErr("couldn't find that order id");
      return;
    }
    setSearchErr("");
    setCurrentOrderId(id);
    setSearchOpen(false);
    setSearchInput("");
  }

  function handleUpdateStatus() {
    const id = vendorId.trim().toUpperCase();
    const order = orders[id];
    if (!order) {
      setVendorMsg("no order with that id exists yet");
      return;
    }

    const alreadyLogged = getTime(order, vendorStatus) !== "";
    const updated = {
      ...order,
      status: vendorStatus,
      history: alreadyLogged
        ? order.history
        : [...order.history, { status: vendorStatus, time: new Date().toISOString() }],
    };

    setOrders({ ...orders, [id]: updated });
    setVendorMsg("updated " + id + " to " + vendorStatus);
  }

  async function handlePlaceOrder() {
    if (!placeName.trim() || !placeOrigin.trim() || !placeDest.trim()) {
      setPlaceMsg("fill in all the fields first");
      return;
    }

    setPlaceMsg("placing order...");

    const { data: userData } = await supabase.auth.getUser();
    const user = userData?.user;
    if (!user) {
      setPlaceMsg("you need to be logged in to place an order");
      return;
    }

    // save it in supabase (the database makes the id + created_at itself)
    const { data: row, error } = await supabase
      .from("orders")
      .insert({
        buyer_id: user.id,
        product_name: PLACEHOLDER_PRODUCT,
        quantity: PLACEHOLDER_QUANTITY,
        delivery_address: placeDest.trim(),
        notes: notesFromForm(placeName.trim(), placeOrigin.trim()),
      })
      .select("id, delivery_address, notes, created_at")
      .single();

    if (error) {
      // only accounts with the buyer role are allowed to insert orders
      setPlaceMsg("couldn't place the order: " + error.message);
      return;
    }

    const id = displayId(row.id);
    setOrders((old) => ({ ...old, [id]: rowToOrder(row) }));
    setPlaceMsg("order placed, id is " + id);
    setPlaceName("");
    setPlaceOrigin("");
    setPlaceDest("");
  }

  function handleVerify() {
    const id = verifyId.trim().toUpperCase();
    const order = orders[id];
    if (!order) {
      setVerifyResult({ found: false, id });
      return;
    }
    setVerifyResult({
      found: true,
      ok: order.storedHash === order.onchainHash,
      storedHash: order.storedHash,
      onchainHash: order.onchainHash,
    });
  }

  const currentStepIdx = STEPS.indexOf(currentOrder.status);

  return (
    <div className="container">
      <div className="header">
        <h3 style={{ margin: 0 }}>Tracker</h3>
        <div className="tabs">
          <button
            className={activeTab === "customer" ? "active" : ""}
            onClick={() => setActiveTab("customer")}
          >
            Customer Tracking
          </button>
          <button
            className={activeTab === "vendor" ? "active" : ""}
            onClick={() => setActiveTab("vendor")}
          >
            Vendor Portal
          </button>
          <button
            className={activeTab === "place" ? "active" : ""}
            onClick={() => setActiveTab("place")}
          >
            Place Order
          </button>
          <button
            className={activeTab === "verify" ? "active" : ""}
            onClick={() => setActiveTab("verify")}
          >
            Verify
          </button>
        </div>
      </div>

      {activeTab === "customer" && (
        <div className="card">
          <div className="order_row">
            <div>
              <p className="label">Order</p>
              <h3 style={{ margin: 0 }}>#{currentOrderId}</h3>
            </div>
            <button className="trackbtn" onClick={() => setSearchOpen(!searchOpen)}>
              Track another order
            </button>
          </div>

          {searchOpen && (
            <div className="searchrow">
              <input
                type="text"
                placeholder="enter order id e.g. SC-10493"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
              />
              <button onClick={handleSearch}>Go</button>
            </div>
          )}
          {searchErr && <div className="errtext">{searchErr}</div>}

          <div className="timeline">
            <div className="fill" style={{ width: (currentStepIdx / 3) * 100 + "%" }} />
            {STEPS.map((stepName, i) => {
              const done = i <= currentStepIdx;
              const here = i === currentStepIdx;
              return (
                <div key={stepName} className={"step" + (done ? " done" : "") + (here ? " here" : "")}>
                  <div className="circle">{done ? "✓" : ""}</div>
                  {stepName === "Order placed" ? "Placed" : stepName}
                  <div className="steptime">{done ? getTime(currentOrder, stepName) : ""}</div>
                </div>
              );
            })}
          </div>

          <div className="bottominfo">
            <div>
              <p className="label">Origin</p>
              <span>{currentOrder.origin}</span>
            </div>
            <div>
              <p className="label">Destination</p>
              <span>{currentOrder.destination}</span>
            </div>
          </div>
        </div>
      )}

      {activeTab === "vendor" && (
        <div className="card">
          <h4 style={{ marginTop: 0 }}>Update an order</h4>
          <label>Order ID</label>
          <input type="text" value={vendorId} onChange={(e) => setVendorId(e.target.value)} />

          <label>New Status</label>
          <select value={vendorStatus} onChange={(e) => setVendorStatus(e.target.value)}>
            {STEPS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <div className="pill">Current status: {orders[vendorId.trim().toUpperCase()]?.status || "?"}</div>

          <button type="button" className="btnmain" onClick={handleUpdateStatus}>
            Update Status
          </button>
          <div className="note">{vendorMsg}</div>
        </div>
      )}

      {activeTab === "place" && (
        <div className="card">
          <h4 style={{ marginTop: 0 }}>Place a new order</h4>
          <label>Customer Name</label>
          <input
            type="text"
            placeholder="e.g. Priya Nair"
            value={placeName}
            onChange={(e) => setPlaceName(e.target.value)}
          />

          <label>Origin</label>
          <input
            type="text"
            placeholder="e.g. Hamilton, NZ"
            value={placeOrigin}
            onChange={(e) => setPlaceOrigin(e.target.value)}
          />

          <label>Destination</label>
          <input
            type="text"
            placeholder="e.g. Auckland CBD"
            value={placeDest}
            onChange={(e) => setPlaceDest(e.target.value)}
          />

          <button type="button" className="btnmain" onClick={handlePlaceOrder}>
            Place Order
          </button>
          <div className="note">{placeMsg}</div>
        </div>
      )}

      {activeTab === "verify" && (
        <div className="card">
          <h4 style={{ marginTop: 0 }}>Verify an order</h4>
          <p className="note" style={{ marginTop: 0 }}>
            checks the order's off-chain data against the hash anchored on-chain, this is just
            running on mock hashes for now until the smart contract side is hooked up
          </p>
          <label>Order ID</label>
          <input type="text" value={verifyId} onChange={(e) => setVerifyId(e.target.value)} />
          <button type="button" className="btnmain" onClick={handleVerify}>
            Verify
          </button>

          {verifyResult && !verifyResult.found && (
            <div className="verifybox bad">no order found with id {verifyResult.id}</div>
          )}
          {verifyResult && verifyResult.found && (
            <div className={"verifybox" + (verifyResult.ok ? "" : " bad")}>
              {verifyResult.ok ? "Verified ✓" : "Tampered ⚠"}
              <br />
              stored hash: <code>{verifyResult.storedHash}</code>
              <br />
              on-chain hash: <code>{verifyResult.onchainHash}</code>
              <br />
              {verifyResult.ok
                ? "both match, this order's data hasn't been tampered with."
                : "these don't match, the off-chain record has been changed since it was anchored."}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
