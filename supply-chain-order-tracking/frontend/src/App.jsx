import { useEffect, useState } from "react";
import { supabase } from "./lib/supabase";
import Auth, { ROLES } from "./Auth";
import Tracker from "./Tracker";
import "./Auth.css";

function roleLabel(role) {
  const found = ROLES.find((r) => r.value === role);
  return found ? found.label : role;
}

function App() {
  const [session, setSession] = useState(null);
  const [checking, setChecking] = useState(true);
  const [profile, setProfile] = useState(null);
  const [profileErr, setProfileErr] = useState("");

  // keep track of who is logged in (also restores the session after a refresh)
  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setChecking(false);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, newSession) => {
      setSession(newSession);
    });

    return () => subscription.unsubscribe();
  }, []);

  // load the logged-in user's profile (name + role) from the profiles table
  const userId = session?.user?.id;
  useEffect(() => {
    if (!userId) {
      setProfile(null);
      setProfileErr("");
      return;
    }

    let cancelled = false;
    supabase
      .from("profiles")
      .select("id, role, full_name, company_name")
      .eq("id", userId)
      .single()
      .then(({ data, error }) => {
        if (cancelled) return;
        if (error) {
          setProfile(null);
          setProfileErr(error.message);
        } else {
          setProfile(data);
          setProfileErr("");
        }
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  async function handleSignOut() {
    await supabase.auth.signOut();
  }

  if (checking) {
    return (
      <div className="container">
        <div className="note">Loading...</div>
      </div>
    );
  }

  if (!session) {
    return <Auth />;
  }

  return (
    <>
      <div className="userbar">
        <div>
          {profile ? (
            <>
              Signed in as <span className="who">{profile.full_name}</span> ·{" "}
              {roleLabel(profile.role)}
            </>
          ) : profileErr ? (
            <>Signed in as {session.user.email} · couldn't load profile</>
          ) : (
            <>Signed in as {session.user.email}</>
          )}
        </div>
        <button type="button" className="trackbtn" onClick={handleSignOut}>
          Sign out
        </button>
      </div>

      <Tracker />
    </>
  );
}

export default App;
