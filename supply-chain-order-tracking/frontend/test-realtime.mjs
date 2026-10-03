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



const { data: order, error: orderError } =
  await supabase
    .from('orders')
    .select('*')
    .eq('blockchain_order_id', 1)
    .single();

if (orderError) {
  console.error('Cannot read Order #1:', orderError);
  process.exit(1);
}

console.log('Order #1 is visible through RLS.');
console.log('Current notes:', order.notes);


const channel = supabase
  .channel('orders-realtime-test')
  .on(
    'postgres_changes',
    {
      event: 'UPDATE',
      schema: 'public',
      table: 'orders',
      filter: 'blockchain_order_id=eq.1'
    },
    (payload) => {
      console.log('\nRealtime update received:');
      console.log(payload.new);
    }
  )
  .subscribe((status) => {
    console.log('Realtime status:', status);
  });

console.log('\nListening for Order #1 updates...');
console.log('Leave this terminal running.');

process.on('SIGINT', async () => {
  await supabase.removeChannel(channel);
  process.exit(0);
});