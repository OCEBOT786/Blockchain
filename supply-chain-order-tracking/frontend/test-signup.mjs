import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.VITE_SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_PUBLISHABLE_KEY;

if (!supabaseUrl || !supabaseKey) {
  console.error('Missing Supabase environment variables.');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseKey);

const email = process.env.TEST_EMAIL;
const password = process.env.TEST_PASSWORD;

if (!email || !password) {
  console.error('Set TEST_EMAIL and TEST_PASSWORD before signing up.');
  process.exit(1);
}

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

if (!data.user?.id) {
  throw new Error('Signup returned no user ID.');
}

if (!data.session) {
  console.log('Confirm the email, then run test-login.mjs to check the profile.');
} else {
  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .select('id, role')
    .eq('id', data.user.id)
    .single();

  if (profileError || profile?.role !== 'buyer') {
    throw new Error('Buyer profile was not created with the expected role.');
  }
  console.log('Buyer profile created automatically.');
}
