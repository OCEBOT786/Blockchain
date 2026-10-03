import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.VITE_SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_PUBLISHABLE_KEY;

const supabase = createClient(
  supabaseUrl,
  supabaseKey
);

const email = process.env.TEST_EMAIL;
const password = process.env.TEST_PASSWORD;

const { data, error } = await supabase.auth.signInWithPassword({
  email,
  password
});

if (error) {
  console.error('Login failed:');
  console.error(error);
  process.exit(1);
}

console.log('Login successful.');
console.log('User ID:', data.user.id);
console.log('Email:', data.user.email);

const { data: profile, error: profileError } = await supabase
  .from('profiles')
  .select('*')
  .eq('id', data.user.id)
  .single();

if (profileError) {
  console.error('Profile retrieval failed:');
  console.error(profileError);
  process.exit(1);
}

if (profile.role !== 'buyer') {
  throw new Error('The buyer test account has an unexpected profile role.');
}

console.log('Profile retrieved successfully:');
console.log(profile);
