import { createClient } from '@supabase/supabase-js';
import { createBrowserClient, createServerClient } from '@supabase/ssr';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error('Missing Supabase environment variables');
}

// 싱글톤 패턴으로 클라이언트 사이드용 인스턴스 관리
let supabaseClient: ReturnType<typeof createBrowserClient> | null = null;

export function getSupabaseClient() {
  if (typeof window === 'undefined') {
    // 서버사이드에서는 매번 새로운 인스턴스 생성
    return createBrowserClient(supabaseUrl, supabaseAnonKey);
  }
  
  // 클라이언트사이드에서는 싱글톤 패턴 사용
  if (!supabaseClient) {
    supabaseClient = createBrowserClient(supabaseUrl, supabaseAnonKey);
  }
  
  return supabaseClient;
}

// 기존 export를 유지하여 호환성 보장
export const supabase = getSupabaseClient();

// 서버 사이드용 (React Router loader/action에서 사용)
export function createSupabaseServerClient(request: Request) {
  const response = new Response();
  
  const supabase = createServerClient(supabaseUrl, supabaseAnonKey, {
    cookies: {
      getAll() {
        const cookieHeader = request.headers.get('Cookie');
        if (!cookieHeader) return [];
        
        return cookieHeader
          .split(';')
          .map((cookie) => {
            const [name, ...rest] = cookie.trim().split('=');
            const value = rest.join('=');
            return { name, value };
          });
      },
      setAll(cookiesToSet) {
        cookiesToSet.forEach(({ name, value, options }) => {
          response.headers.append(
            'Set-Cookie',
            serialize(name, value, options)
          );
        });
      },
    },
  });
  
  return { supabase, response };
}

function serialize(name: string, value: string, options: any = {}): string {
  const pairs = [`${name}=${encodeURIComponent(value)}`];
  
  if (options.maxAge) pairs.push(`Max-Age=${options.maxAge}`);
  if (options.domain) pairs.push(`Domain=${options.domain}`);
  if (options.path) pairs.push(`Path=${options.path}`);
  if (options.httpOnly) pairs.push('HttpOnly');
  if (options.secure) pairs.push('Secure');
  if (options.sameSite) pairs.push(`SameSite=${options.sameSite}`);
  
  return pairs.join('; ');
}