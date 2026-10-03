import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.VITE_SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_PUBLISHABLE_KEY;

if (!supabaseUrl || !supabaseKey) {
  console.error('Missing Supabase environment variables.');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseKey);

const email = process.env.LOGISTICS_TEST_EMAIL;
const password = process.env.LOGISTICS_TEST_PASSWORD;

if (!email || !password) {
  console.error('Set LOGISTICS_TEST_EMAIL and LOGISTICS_TEST_PASSWORD before signing up.');
  process.exit(1);
}

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

if (!data.user?.id) {
  throw new Error('Signup returned no user ID.');
}

if (!data.session) {
  console.log('Confirm the email, then log in to check the profile.');
} else {
  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .select('id, role')
    .eq('id', data.user.id)
    .single();

  if (profileError || profile?.role !== 'logistics_provider') {
    throw new Error('Logistics profile was not created with the expected role.');
  }
  console.log('Logistics provider profile created automatically.');
}
