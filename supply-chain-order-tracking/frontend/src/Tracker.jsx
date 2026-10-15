import { useCallback, useEffect, useState } from 'react';
import { supabase } from './lib/supabase';
import { ordersApi } from './lib/backend';
import './Tracker.css';

const STATUS = ['Order Received', 'In Transit', 'Delivered'];
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const shortId = (id) => `SC-${id.slice(0, 8).toUpperCase()}`;
const showError = (error) => error instanceof Error ? error.message : String(error);

export default function Tracker({ session, profile }) {
  const [tab, setTab] = useState('customer');
  const [orders, setOrders] = useState([]);
  const [selected, setSelected] = useState('');
  const [search, setSearch] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [verifyResult, setVerifyResult] = useState(null);
  const [product, setProduct] = useState('');
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [delivery, setDelivery] = useState('');
  const [status, setStatus] = useState(1);
  const [logisticsId, setLogisticsId] = useState('');
  const [buyerWallet, setBuyerWallet] = useState('');

  useEffect(() => { setBuyerWallet(profile?.wallet_address || ''); }, [profile?.wallet_address]);

  const refresh = useCallback(async () => {
    const rows = await ordersApi.list();
    setOrders(rows);
    setSelected((previous) => rows.some((row) => row.id === previous) ? previous : (rows[0]?.id || ''));
  }, []);

  useEffect(() => {
    if (!session) return;
    let active = true;
    refresh().catch((error) => { if (active) setMessage(showError(error)); });
    const timer = setInterval(() => refresh().catch(() => {}), 10000);
    return () => { active = false; clearInterval(timer); };
  }, [session?.user?.id, refresh]);

  const current = orders.find((row) => row.id === selected);
  const isBuyer = profile?.role === 'buyer';
  const isLogistics = profile?.role === 'logistics_provider';

  async function act(action, success) {
    setLoading(true);
    setMessage('');
    try {
      await action();
      await refresh();
      if (success) setMessage(success);
    } catch (error) {
      setMessage(showError(error));
    } finally {
      setLoading(false);
    }
  }

  function selectOrder() {
    const term = search.trim().toLowerCase();
    const row = orders.find((item) => item.id.toLowerCase() === term || shortId(item.id).toLowerCase() === term);
    if (row) { setSelected(row.id); setMessage(''); }
    else setMessage('No accessible order matches that ID.');
  }

  async function placeOrder() {
    if (!isBuyer || !product.trim() || !Number.isSafeInteger(Number(quantity)) || Number(quantity) < 1) {
      setMessage('Enter a product name and a positive whole quantity.');
      return;
    }
    setLoading(true);
    setMessage('');
    try {
      const { data: row, error } = await supabase.from('orders').insert({
        buyer_id: session.user.id,
        product_name: product.trim(),
        product_description: description.trim() || null,
        quantity: Number(quantity),
        delivery_address: delivery.trim() || null,
      }).select('id').single();
      if (error) throw error;
      setSelected(row.id);
      setProduct(''); setDescription(''); setQuantity(1); setDelivery('');
      try {
        await ordersApi.link(row.id);
        setMessage(`Order ${shortId(row.id)} linked to the blockchain.`);
      } catch (linkError) {
        setMessage(`Order saved as ${shortId(row.id)}, but blockchain linking needs a retry: ${showError(linkError)}`);
      }
      await refresh();
    } catch (error) {
      setMessage(showError(error));
    } finally { setLoading(false); }
  }

  async function verify() {
    if (!current) return;
    setLoading(true); setVerifyResult(null); setMessage('');
    try { setVerifyResult(await ordersApi.verify(current.id)); }
    catch (error) { setMessage(showError(error)); }
    finally { setLoading(false); }
  }

  async function assignLogistics() {
    if (!current || !UUID.test(logisticsId.trim())) {
      setMessage('Enter the logistics provider’s full profile UUID.');
      return;
    }
    await act(async () => {
      const { data, error } = await supabase.from('orders')
        .update({ logistics_provider_id: logisticsId.trim() })
        .eq('id', current.id).select('id').single();
      if (error) throw error;
      if (!data) throw new Error('The logistics assignment was not saved.');
    }, 'Logistics provider assigned.');
  }

  async function saveWallet() {
    const wallet = buyerWallet.trim();
    if (!/^0x[0-9a-fA-F]{40}$/.test(wallet) || /^0x0{40}$/i.test(wallet)) {
      setMessage('Enter a valid nonzero buyer wallet address.');
      return;
    }
    await act(async () => {
      const { data, error } = await supabase.from('profiles')
        .update({ wallet_address: wallet }).eq('id', session.user.id).select('id').single();
      if (error) throw error;
      if (!data) throw new Error('Wallet address was not saved.');
    }, 'Buyer wallet address saved.');
  }

  return <div className="container">
    <div className="header">
      <h3 style={{ margin: 0 }}>Tracker</h3>
      <div className="tabs">
        <button className={tab === 'customer' ? 'active' : ''} onClick={() => setTab('customer')}>Customer Tracking</button>
        {isLogistics && <button className={tab === 'vendor' ? 'active' : ''} onClick={() => setTab('vendor')}>Logistics</button>}
        {isBuyer && <button className={tab === 'place' ? 'active' : ''} onClick={() => setTab('place')}>Place Order</button>}
        <button className={tab === 'verify' ? 'active' : ''} onClick={() => setTab('verify')}>Verify</button>
      </div>
    </div>

    {message && <div className="card" role="status">{message}</div>}
    {tab !== 'place' && <div className="card">
      <label>Accessible order</label>
      <select aria-label="Accessible order" value={selected} onChange={(event) => { setSelected(event.target.value); setVerifyResult(null); }}>
        {!orders.length && <option value="">No orders available</option>}
        {orders.map((row) => <option key={row.id} value={row.id}>{shortId(row.id)} · {row.product_name}</option>)}
      </select>
      <div className="searchrow">
        <input aria-label="Find order ID" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Full UUID or SC- ID" />
        <button onClick={selectOrder}>Go</button>
      </div>
    </div>}

    {tab === 'customer' && <div className="card">
      {!current ? <p>No accessible orders yet.</p> : <>
        <div className="order_row"><div><p className="label">Order</p><h3 style={{ margin: 0 }}>{shortId(current.id)}</h3></div><button className="trackbtn" onClick={() => act(async () => {}, 'Orders refreshed.')}>Refresh</button></div>
        <p className="note">UUID: <code>{current.id}</code></p>
        <div className="timeline"><div className="fill" style={{ width: current.onchain_status == null ? '0%' : `${current.onchain_status * 50}%` }} />
          {STATUS.map((name, index) => <div key={name} className={`step${current.onchain_status >= index ? ' done' : ''}${current.onchain_status === index ? ' here' : ''}`}><div className="circle">{current.onchain_status >= index ? '✓' : ''}</div>{name}</div>)}
        </div>
        <div className="bottominfo"><div><p className="label">Product</p>{current.product_name} × {current.quantity}</div><div><p className="label">Delivery</p>{current.delivery_address || 'Not specified'}</div></div>
        {current.chainError && <p className="errtext">Blockchain read failed: {current.chainError}</p>}
        {current.blockchain_order_id ? <p className="note">Blockchain order: {current.blockchain_order_id}<br />Transaction: <code>{current.blockchain_tx_hash}</code><br />Contract: <code>{current.contract_address}</code></p>
          : <><p className="note">This order is saved in Supabase but is not linked to the blockchain.</p>{isBuyer && <button className="btnmain" disabled={loading} onClick={() => act(() => ordersApi.link(current.id), 'Blockchain link confirmed.')}>Retry blockchain link</button>}</>}
        {isBuyer && <><label>Assign logistics provider by profile UUID</label>
          <input value={logisticsId} onChange={(event) => setLogisticsId(event.target.value)} placeholder={current.logistics_provider_id || 'Provider profile UUID'} />
          <button className="trackbtn" disabled={loading} onClick={assignLogistics}>Assign provider</button></>}
      </>}
    </div>}

    {tab === 'vendor' && isLogistics && <div className="card"><h4 style={{ marginTop: 0 }}>Update an assigned order</h4>
      <p className="note">Your provider profile UUID: <code>{session.user.id}</code>. Share it with the buyer for assignment.</p>
      <p className="note">Current status: {current?.onchain_status == null ? 'Not linked' : STATUS[current.onchain_status]}</p>
      <label>New status</label><select value={status} onChange={(event) => setStatus(Number(event.target.value))}><option value={1}>In Transit</option><option value={2}>Delivered</option></select>
      <button className="btnmain" disabled={loading || !current?.blockchain_order_id} onClick={() => act(() => ordersApi.updateStatus(current.id, status), 'On-chain status confirmed.')}>Update Status</button>
    </div>}

    {tab === 'place' && isBuyer && <div className="card"><h4 style={{ marginTop: 0 }}>Place a new order</h4>
      <label>Buyer wallet address</label><input value={buyerWallet} onChange={(event) => setBuyerWallet(event.target.value)} placeholder="0x..." />
      <button className="trackbtn" disabled={loading} onClick={saveWallet}>Save wallet address</button>
      <p className="note">The backend reads your saved profile wallet when linking. This field is a public address only.</p>
      <label>Product name</label><input value={product} onChange={(event) => setProduct(event.target.value)} placeholder="Coffee Beans" />
      <label>Description (optional)</label><input value={description} onChange={(event) => setDescription(event.target.value)} />
      <label>Quantity</label><input type="number" min="1" step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
      <label>Delivery address (optional)</label><input value={delivery} onChange={(event) => setDelivery(event.target.value)} />
      <button className="btnmain" disabled={loading} onClick={placeOrder}>Place Order</button>
    </div>}

    {tab === 'verify' && <div className="card"><h4 style={{ marginTop: 0 }}>Verify an order</h4>
      <p className="note">Compare this order's current Supabase metadata with the hash stored on the configured blockchain contract.</p>
      <button className="btnmain" disabled={loading || !current} onClick={verify}>Verify</button>
      {verifyResult && <div className={`verifybox${verifyResult.status === 'VERIFIED' ? '' : ' bad'}`} role="status">
        <strong>{verifyResult.status}</strong><br />
        {verifyResult.message || (verifyResult.status === 'NOT_LINKED' ? 'This order has no blockchain link.' : '')}
        {verifyResult.currentHash && <>Current metadata hash: <code>{verifyResult.currentHash}</code><br />On-chain hash: <code>{verifyResult.onchainHash}</code></>}
      </div>}
    </div>}
  </div>;
}
