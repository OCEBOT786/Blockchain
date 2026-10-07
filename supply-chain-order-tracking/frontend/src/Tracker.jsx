import { useState } from "react";
import "./Tracker.css";

// same mock data as the html version — will get replaced with real
// supabase / backend calls once that's ready
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

  function handlePlaceOrder() {
    if (!placeName.trim() || !placeOrigin.trim() || !placeDest.trim()) {
      setPlaceMsg("fill in all the fields first");
      return;
    }

    const id = "SC-" + Math.floor(10000 + Math.random() * 90000);
    const newOrder = {
      customer: placeName.trim(),
      origin: placeOrigin.trim(),
      destination: placeDest.trim(),
      status: "Order placed",
      storedHash: "pending",
      onchainHash: "pending",
      history: [{ status: "Order placed", time: new Date().toISOString() }],
    };

    setOrders({ ...orders, [id]: newOrder });
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
