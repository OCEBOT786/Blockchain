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

// assign logistics provider to blockchain order #1
const { data: order, error: updateError } =
  await supabase
    .from('orders')
    .update({
      logistics_provider_id:
        process.env.LOGISTICS_USER_ID
    })
    .eq('blockchain_order_id', 1)
    .select()
    .single();

if (updateError) {
  console.error('Assignment failed:');
  console.error(updateError);
  process.exit(1);
}

console.log('\nLogistics provider assigned successfully:');
console.log(order);