import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.VITE_SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_PUBLISHABLE_KEY;

if (!supabaseUrl || !supabaseKey) {
  console.error('Missing Supabase environment variables.');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseKey);

const email = 'spencerv179+logistics@gmail.com';
const password = 'TestPassword123!';

const { data, error } = await supabase.auth.signUp({
  email,
  password,
  options: {
    data: {
      full_name: 'Test Logistics Provider',
      role: 'logistics_provider',
      company_name: 'Test Logistics',
      wallet_address: '0x70997970C51812dc3A010C7d01b50e0d17dC79C8',
      phone: '0211111111'
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