import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL,
  process.env.VITE_SUPABASE_PUBLISHABLE_KEY
);

const email = process.env.TEST_EMAIL;
const password = process.env.TEST_PASSWORD;



// login
const { data: loginData, error: loginError } =
  await supabase.auth.signInWithPassword({
    email,
    password
  });

if (loginError) {
  console.error('Login failed:', loginError);
  process.exit(1);
}

const user = loginData.user;

console.log('Logged in as:', user.email);
console.log('User ID:', user.id);


// create off chain order
const { data: order, error: orderError } =
  await supabase
    .from('orders')
    .insert({
      buyer_id: user.id,

      product_name: 'Coffee Beans',

      product_description:
        'Premium coffee beans for supply-chain testing',

      quantity: 20,

      delivery_address:
        '123 Test Street, Auckland',

      notes:
        'Test order for COMP726'
    })
    .select()
    .single();


if (orderError) {
  console.error('Order creation failed:');
  console.error(orderError);
  process.exit(1);
}


console.log('\nOrder created successfully:');
console.log(order);