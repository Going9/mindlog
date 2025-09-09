import { useEffect, useState } from 'react';
import { supabase } from '~/lib/supabase';

export default function AuthCallbackPage() {
  const [isProcessing, setIsProcessing] = useState(true);

  useEffect(() => {
    const handleAuthCallback = async () => {
      try {
        // URL에서 hash fragment 처리
        const hashFragment = window.location.hash;
        if (hashFragment) {
          const { data, error } = await supabase.auth.getSession();
          
          if (error) {
            console.error('Auth callback error:', error);
            window.location.href = '/login';
            return;
          }

          if (data.session) {
            // 로그인 성공 시 홈으로 리다이렉트
            window.location.href = '/';
          } else {
            // 세션이 없으면 로그인 페이지로
            window.location.href = '/login';
          }
        } else {
          // hash가 없으면 로그인 페이지로
          window.location.href = '/login';
        }
      } catch (error) {
        console.error('Auth callback error:', error);
        window.location.href = '/login';
      } finally {
        setIsProcessing(false);
      }
    };

    // 약간의 지연을 주어 React Router가 완전히 초기화될 때까지 기다림
    const timer = setTimeout(handleAuthCallback, 100);
    
    return () => clearTimeout(timer);
  }, []);

  if (!isProcessing) {
    return null;
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto mb-4"></div>
        <p className="text-gray-600">로그인 처리 중...</p>
      </div>
    </div>
  );
}