import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'

const root = createRoot(document.getElementById('root'))
const missingSupabaseConfig = [
  'VITE_SUPABASE_URL',
  'VITE_SUPABASE_PUBLISHABLE_KEY',
].filter((name) => !import.meta.env[name])

if (missingSupabaseConfig.length) {
  root.render(
    <main style={{ margin: 'auto', padding: '2rem', maxWidth: 560, textAlign: 'left' }}>
      <h1 style={{ fontSize: '1.75rem' }}>Supabase setup needed</h1>
      <p>Set these values in <code>frontend/.env</code> using the shared Supabase project:</p>
      <ul>
        {missingSupabaseConfig.map((name) => <li key={name}><code>{name}</code></li>)}
      </ul>
      <p>Then restart <code>npm run dev</code> and reload this page.</p>
    </main>,
  )
} else {
  import('./App.jsx').then(({ default: App }) => {
    root.render(
      <StrictMode>
        <App />
      </StrictMode>,
    )
  })
}
