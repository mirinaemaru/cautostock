package maru.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Trading System MVP (B안) - KIS OpenAPI 기반 자동매매 시스템
 *
 * @see /md/docs/ 문서를 설계 기준으로 사용
 */
@SpringBootApplication
@EnableScheduling
public class TradingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingSystemApplication.class, args);
	}
}
