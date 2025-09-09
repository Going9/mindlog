import { useEffect, useState } from 'react';
import type { User, Session } from '@supabase/supabase-js';
import { getSupabaseClient } from '~/lib/supabase';

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const supabase = getSupabaseClient();
    
    // 초기 사용자 정보 가져오기 - 보안상 getUser() 사용
    const getInitialAuth = async () => {
      try {
        const { data: { session } } = await supabase.auth.getSession();
        const { data: { user }, error } = await supabase.auth.getUser();
        
        if (error) {
          console.error('Auth error:', error);
          setSession(null);
          setUser(null);
        } else {
          setSession(session);
          setUser(user);
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        setSession(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    getInitialAuth();

    // 인증 상태 변경 리스너
    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      async (event, session) => {
        if (session) {
          // 세션이 있을 때는 실제 사용자 정보를 서버에서 검증
          const { data: { user }, error } = await supabase.auth.getUser();
          if (error) {
            console.error('User verification error:', error);
            setSession(null);
            setUser(null);
          } else {
            setSession(session);
            setUser(user);
            
            // 로그인 성공 시 프로필 생성
            if (event === 'SIGNED_IN' && user) {
              await createOrUpdateProfile(user);
            }
          }
        } else {
          setSession(null);
          setUser(null);
        }
        setLoading(false);
      }
    );

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  const signOut = async () => {
    const supabase = getSupabaseClient();
    const { error } = await supabase.auth.signOut();
    if (error) {
      console.error('Sign out error:', error);
      return { error };
    }
    return { error: null };
  };

  return {
    user,
    session,
    loading,
    signOut,
    isAuthenticated: !!session,
  };
}

async function createOrUpdateProfile(user: User) {
  try {
    const supabase = getSupabaseClient();
    
    // 프로필이 존재하는지 확인
    const { data: existingProfile } = await supabase
      .from('profiles')
      .select('id')
      .eq('id', user.id)
      .single();

    if (!existingProfile) {
      // 새 프로필 생성 - 데이터베이스 컬럼명에 맞게 snake_case 사용
      const profileData = {
        id: user.id,
        name: user.user_metadata?.full_name || user.user_metadata?.name || user.email?.split('@')[0] || 'Unknown',
        user_name: user.email?.split('@')[0] || `user_${Date.now()}`, // userName -> user_name
        avatar: user.user_metadata?.avatar_url || null,
      };

      const { error } = await supabase
        .from('profiles')
        .insert(profileData);

      if (error) {
        console.error('Profile creation error:', error);
      }
    }
  } catch (error) {
    console.error('Profile handling error:', error);
  }
}