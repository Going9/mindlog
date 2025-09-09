import { redirect } from 'react-router';
import { createSupabaseServerClient } from './supabase';

export async function requireAuth(request: Request) {
  const { supabase } = createSupabaseServerClient(request);
  
  const {
    data: { user },
    error,
  } = await supabase.auth.getUser();

  if (error || !user) {
    throw redirect('/login');
  }

  return { user };
}

export async function getOptionalAuth(request: Request) {
  const { supabase } = createSupabaseServerClient(request);
  
  const {
    data: { user },
  } = await supabase.auth.getUser();

  return { user };
}

export async function logout(request: Request) {
  const { supabase, response } = createSupabaseServerClient(request);
  
  await supabase.auth.signOut();
  
  return redirect('/login', {
    headers: response.headers,
  });
}