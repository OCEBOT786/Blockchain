import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.VITE_SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_PUBLISHABLE_KEY;

if (!supabaseUrl || !supabaseKey) {
  console.error('Missing Supabase environment variables.');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseKey);

const email = 'spencerv179@gmail.com';
const password = 'TestPassword123!';

const { data, error } = await supabase.auth.signUp({
  email,
  password,
  options: {
    data: {
      full_name: 'Test Buyer',
      role: 'buyer',
      company_name: 'Test Company',
      wallet_address: '0x1111111111111111111111111111111111111111',
      phone: '0210000000'
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