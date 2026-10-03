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

const { data, error } = await supabase
  .from('orders')
  .update({
    notes: `Realtime test ${new Date().toISOString()}`
  })
  .eq('blockchain_order_id', 1)
  .select()
  .single();

if (error) {
  console.error('Update failed:', error);
  process.exit(1);
}

console.log('Order updated successfully:');
console.log(data);