import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL,
  process.env.VITE_SUPABASE_PUBLISHABLE_KEY
);

// login as the buyer
const { data: loginData, error: loginError } =
  await supabase.auth.signInWithPassword({
    email: process.env.TEST_EMAIL,
    password: process.env.TEST_PASSWORD
  });

if (loginError) {
  console.error('Login failed:', loginError);
  process.exit(1);
}

console.log('Logged in as buyer:', loginData.user.email);

if (!process.env.LOGISTICS_USER_ID) {
  throw new Error('Set LOGISTICS_USER_ID before assigning an order.');
}

let orderQuery = supabase
  .from('orders')
  .select('id, blockchain_order_id')
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

// Assign this buyer's linked order.
const { data: order, error: updateError } =
  await supabase
    .from('orders')
    .update({
      logistics_provider_id:
        process.env.LOGISTICS_USER_ID
    })
    .eq('id', targetOrder.id)
    .select()
    .single();

if (updateError) {
  console.error('Assignment failed:');
  console.error(updateError);
  process.exit(1);
}

console.log('\nLogistics provider assigned successfully:');
if (order.logistics_provider_id !== process.env.LOGISTICS_USER_ID) {
  throw new Error('The logistics assignment did not persist.');
}

console.log(order);
