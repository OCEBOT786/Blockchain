import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL,
  process.env.VITE_SUPABASE_PUBLISHABLE_KEY
);


const { data: loginData, error: loginError } =
  await supabase.auth.signInWithPassword({
    email: process.env.TEST_EMAIL,
    password: process.env.TEST_PASSWORD
  });

if (loginError) {
  console.error('Login failed:', loginError);
  process.exit(1);
}

console.log('Logged in as:', loginData.user.email);

await supabase.realtime.setAuth(
  loginData.session.access_token
);



let orderQuery = supabase
  .from('orders')
  .select('id, notes')
  .eq('buyer_id', loginData.user.id)
  .not('blockchain_order_id', 'is', null);

if (process.env.TEST_ORDER_ID) {
  orderQuery = orderQuery.eq('id', process.env.TEST_ORDER_ID);
}

const { data: order, error: orderError } = await orderQuery
  .order('created_at', { ascending: false })
  .limit(1)
  .single();

if (orderError) {
  console.error('Cannot read the linked order:', orderError);
  process.exit(1);
}

console.log('Order is visible through the authenticated query.');
console.log('Order ID:', order.id);
console.log('Current notes:', order.notes);


const channel = supabase
  .channel('orders-realtime-test')
  .on(
    'postgres_changes',
    {
      event: 'UPDATE',
      schema: 'public',
      table: 'orders',
      filter: `id=eq.${order.id}`
    },
    (payload) => {
      console.log('\nRealtime update received:');
      console.log(payload.new);
    }
  )
  .subscribe((status) => {
    console.log('Realtime status:', status);
  });

console.log('\nListening for this order\'s updates...');
console.log('Leave this terminal running.');

process.on('SIGINT', async () => {
  await supabase.removeChannel(channel);
  process.exit(0);
});
