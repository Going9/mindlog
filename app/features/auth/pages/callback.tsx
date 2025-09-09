import type { LoaderFunctionArgs } from 'react-router';
import { redirect } from 'react-router';
import { createSupabaseServerClient } from '~/lib/supabase';
import { useEffect } from 'react';
import { getSupabaseClient } from '~/lib/supabase';

export async function loader({ request }: LoaderFunctionArgs) {
  const url = new URL(request.url);
  const code = url.searchParams.get('code');
  
  if (code) {
    const { supabase: serverSupabase, response } = createSupabaseServerClient(request);
    
    const { error } = await serverSupabase.auth.exchangeCodeForSession(code);
    
    if (error) {
      console.error('OAuth callback error:', error);
      return redirect('/login', {
        headers: response.headers,
      });
    }
    
    return redirect('/', {
      headers: response.headers,
    });
  }
  
  // code가 없으면 클라이언트에서 처리
  return null;
}

export default function AuthCallbackPage() {
  useEffect(() => {
    const handleAuthCallback = async () => {
      try {
        const supabase = getSupabaseClient();
        // 보안상 getUser()로 사용자 정보 검증
        const { data: { user }, error } = await supabase.auth.getUser();
        
        if (error) {
          console.error('Auth callback error:', error);
          window.location.href = '/login';
          return;
        }

        if (user) {
          window.location.href = '/';
        } else {
          window.location.href = '/login';
        }
      } catch (error) {
        console.error('Auth callback error:', error);
        window.location.href = '/login';
      }
    };

    // URL에서 hash fragment나 code 파라미터 확인
    const urlParams = new URLSearchParams(window.location.search);
    const hashFragment = window.location.hash;
    
    if (urlParams.get('code') || hashFragment) {
      const timer = setTimeout(handleAuthCallback, 100);
      return () => clearTimeout(timer);
    } else {
      window.location.href = '/login';
    }
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto mb-4"></div>
        <p className="text-gray-600">로그인 처리 중...</p>
      </div>
    </div>
  );
}