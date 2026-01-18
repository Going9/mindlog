package com.mindlog.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtil {

    // 1. 코드 검증기 (Verifier) 생성
    // 역할: "진짜 비밀번호"를 만듭니다. 이건 절대 남에게 보여주면 안 됩니다.
    public static String generateCodeVerifier() {

        // SecureRandom: 일반 Random보다 훨씬 예측하기 어려운, 보안용 주사위를 준비합니다.
        SecureRandom secureRandom = new SecureRandom();

        // byte[32]: 32칸짜리 빈 상자를 준비합니다.
        byte[] codeVerifier = new byte[32];

        // nextBytes: 상자 32칸에 무작위 숫자들을 가득 채웁니다.
        secureRandom.nextBytes(codeVerifier);

        // Base64...encodeToString:
        // 컴퓨터만 아는 이진수(010101)를 URL에 넣을 수 있는 안전한 영어/숫자 문자열로 바꿉니다.
        // withoutPadding: 끝에 붙는 '=' 기호를 떼버립니다(URL에서 오류 날까 봐).
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    // 2. 코드 챌린지 (Challenge) 생성
    // 역할: Verifier를 Supabase에게 보여주기 위해 "변장"시킵니다.
    // 핵심: Challenge를 보고 Verifier를 유추할 수는 없어야 합니다. (일방통행)
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            // codeVerifier.getBytes: 아까 만든 비밀번호 문자열을 다시 바이트(데이터)로 바꿉니다.
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);

            // MessageDigest.getInstance("SHA-256"):
            // "SHA-256"이라는 '믹서기'를 가져옵니다.
            // 이 믹서기는 무엇을 넣든 완전히 갈아버려서 원래 재료를 알 수 없게 만듭니다.
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            // update: 믹서기에 비밀번호(bytes)를 붓습니다.
            messageDigest.update(bytes, 0, bytes.length);

            // digest: "갈아버려!" 버튼을 누릅니다. 결과물(digest)이 나옵니다.
            byte[] digest = messageDigest.digest();

            // Base64...: 갈아져 나온 결과물을 다시 문자열로 예쁘게 포장합니다.
            // 이게 바로 Supabase에게 먼저 보여줄 '변장한 암호(Challenge)'입니다.
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        } catch (Exception e) {
            // 혹시라도 자바가 SHA-256을 모르면 에러를 냅니다(그럴 일은 거의 없습니다).
            throw new RuntimeException("SHA-256 algorithm not supported", e);
        }
    }
}