import { useState } from "react";
import { supabase } from "./lib/supabase";
import "./Auth.css";

// the three roles the database accepts (profiles.role check constraint)
export const ROLES = [
  { value: "buyer", label: "Buyer" },
  { value: "wholesaler", label: "Wholesaler" },
  { value: "logistics_provider", label: "Logistics provider" },
];

const WALLET_PATTERN = /^0x[a-fA-F0-9]{40}$/;

function Auth() {
  const [mode, setMode] = useState("login"); // "login" or "register"

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // register-only fields
  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState("buyer");
  const [company, setCompany] = useState("");
  const [phone, setPhone] = useState("");
  const [wallet, setWallet] = useState("");

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [info, setInfo] = useState("");

  function switchMode(next) {
    setMode(next);
    setErr("");
    setInfo("");
  }

  async function handleLogin(e) {
    e.preventDefault();
    setErr("");
    setInfo("");

    if (!email.trim() || !password) {
      setErr("enter your email and password");
      return;
    }

    setBusy(true);
    const { error } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password,
    });
    setBusy(false);

    // on success App.jsx picks up the new session by itself
    if (error) setErr(error.message);
  }

  async function handleRegister(e) {
    e.preventDefault();
    setErr("");
    setInfo("");

    if (!fullName.trim()) {
      setErr("enter your full name");
      return;
    }
    if (!email.trim()) {
      setErr("enter your email");
      return;
    }
    if (password.length < 6) {
      setErr("password needs to be at least 6 characters");
      return;
    }
    if (wallet.trim() && !WALLET_PATTERN.test(wallet.trim())) {
      setErr("wallet address should look like 0x followed by 40 characters");
      return;
    }

    // only send the optional fields that were actually filled in —
    // wallet_address is unique in the database, so an empty string would clash
    const meta = { full_name: fullName.trim(), role };
    if (company.trim()) meta.company_name = company.trim();
    if (phone.trim()) meta.phone = phone.trim();
    if (wallet.trim()) meta.wallet_address = wallet.trim();

    setBusy(true);
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: { data: meta },
    });
    setBusy(false);

    if (error) {
      setErr(error.message);
      return;
    }

    // if the project needs email confirmation there is no session yet
    if (!data.session) {
      setPassword("");
      setMode("login");
      setInfo("Account created. Check your email to confirm it, then log in.");
    }
    // otherwise the user is already logged in and App.jsx takes over
  }

  return (
    <div className="container">
      <div className="header">
        <h3 style={{ margin: 0 }}>Tracker</h3>
        <div className="tabs">
          <button
            type="button"
            className={mode === "login" ? "active" : ""}
            onClick={() => switchMode("login")}
          >
            Log in
          </button>
          <button
            type="button"
            className={mode === "register" ? "active" : ""}
            onClick={() => switchMode("register")}
          >
            Register
          </button>
        </div>
      </div>

      {mode === "login" && (
        <form className="card" onSubmit={handleLogin}>
          <h4 style={{ marginTop: 0 }}>Log in</h4>

          <label>Email</label>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label>Password</label>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button type="submit" className="btnmain" disabled={busy}>
            {busy ? "Logging in..." : "Log in"}
          </button>
          {info && <div className="infotext">{info}</div>}
          {err && <div className="errtext">{err}</div>}
        </form>
      )}

      {mode === "register" && (
        <form className="card" onSubmit={handleRegister}>
          <h4 style={{ marginTop: 0 }}>Create an account</h4>

          <label>Full name</label>
          <input
            type="text"
            autoComplete="name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />

          <label>Email</label>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label>Password</label>
          <input
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <label>I am a</label>
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            {ROLES.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>

          <label>Company name (optional)</label>
          <input
            type="text"
            value={company}
            onChange={(e) => setCompany(e.target.value)}
          />

          <label>Phone (optional)</label>
          <input
            type="tel"
            autoComplete="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />

          <label>Wallet address (optional)</label>
          <input
            type="text"
            placeholder="0x..."
            value={wallet}
            onChange={(e) => setWallet(e.target.value)}
          />

          <button type="submit" className="btnmain" disabled={busy}>
            {busy ? "Creating account..." : "Register"}
          </button>
          {err && <div className="errtext">{err}</div>}
        </form>
      )}
    </div>
  );
}

export default Auth;
