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

let orderQuery = supabase
  .from('orders')
  .select('id')
  .eq('buyer_id', loginData.user.id)
  .not('blockchain_order_id', 'is', null);

if (process.env.TEST_ORDER_ID) {
  orderQuery = orderQuery.eq('id', process.env.TEST_ORDER_ID);
}

const { data: targetOrder, error: lookupError } = await orderQuery
  .order('created_at', { ascending: false })
  .limit(1)
  .single();

if (lookupError) {
  throw new Error(`Could not find a linked order: ${lookupError.message}`);
}

const { data, error } = await supabase
  .from('orders')
  .update({
    notes: `Realtime test ${new Date().toISOString()}`
  })
  .eq('id', targetOrder.id)
  .select()
  .single();

if (error) {
  console.error('Update failed:', error);
  process.exit(1);
}

console.log('Order updated successfully:');
console.log(data);
