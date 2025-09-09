import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '~/common/components/ui/card';
import { SocialLoginButtons } from '../components/social-login-buttons';

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold">로그인</CardTitle>
          <CardDescription>
            소셜 계정으로 간편하게 로그인하세요
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-4">
          <SocialLoginButtons />
        </CardContent>
      </Card>
    </div>
  );
}