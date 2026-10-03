import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.VITE_SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_PUBLISHABLE_KEY;

if (!supabaseUrl || !supabaseKey) {
  console.error('Missing Supabase environment variables.');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseKey);

const email = process.env.WHOLESALER_TEST_EMAIL;
const password = process.env.WHOLESALER_TEST_PASSWORD;

const { data, error } = await supabase.auth.signUp({
  email,
  password,
  options: {
    data: {
        full_name: 'Test Wholesaler',
        role: 'wholesaler',
        company_name: 'Test Wholesale',
        wallet_address: '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC',
        phone: '0212222222'
    }
  }
});

if (error) {
  console.error('Signup failed:');
  console.error(error);
  process.exit(1);
}

console.log('Signup successful.');
console.log('User ID:', data.user?.id);
console.log('Email:', data.user?.email);