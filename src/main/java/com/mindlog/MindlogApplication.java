package com.mindlog;

import io.github.cdimascio.dotenv.Dotenv; // 이 임포트가 반드시 필요합니다.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MindlogApplication {

	public static void main(String[] args) {
		// .env 파일의 내용을 시스템 환경 변수로 강제 로드
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing() // .env 파일이 없어도 에러 내지 않음
				.load();

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

		SpringApplication.run(MindlogApplication.class, args);
	}
}