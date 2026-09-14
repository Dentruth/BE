package com.dentruth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class DentruthApplication {

	public static void main(String[] args) {
		loadDotEnvIntoSystemProperties();
		SpringApplication.run(DentruthApplication.class, args);
	}

	/**
	 * IntelliJ Run 버튼 등 셸을 거치지 않는 실행 방식에서는 .env가 프로세스 환경변수로 주입되지 않아
	 * ${DB_URL} 같은 플레이스홀더 해석이 실패한다. 이미 환경변수/시스템 프로퍼티로 설정된 값은 덮어쓰지 않는다.
	 */
	private static void loadDotEnvIntoSystemProperties() {
		Path envFile = Path.of(".env");
		if (!Files.isRegularFile(envFile)) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(envFile);
			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}

				int separatorIndex = trimmed.indexOf('=');
				if (separatorIndex <= 0) {
					continue;
				}

				String key = trimmed.substring(0, separatorIndex).trim();
				String value = trimmed.substring(separatorIndex + 1).trim();
				if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}

				if (System.getenv(key) == null && System.getProperty(key) == null) {
					System.setProperty(key, value);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load .env file", e);
		}
	}

}
