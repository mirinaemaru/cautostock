package maru.trading.broker.kis.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * REST 클라이언트 설정 for KIS OpenAPI.
 *
 * PAPER 모드로 실제 KIS OpenAPI를 호출합니다.
 */
@Configuration
public class KisRestClientConfig {

    /**
     * KIS OpenAPI 호출용 RestTemplate.
     *
     * 특징:
     * - Connection timeout: 10초
     * - Read timeout: 30초
     * - 자동 리트라이 없음 (ApiRetryPolicy에서 처리)
     */
    @Bean
    public RestTemplate kisRestTemplate(RestTemplateBuilder builder,
                                         MappingJackson2HttpMessageConverter jacksonConverter) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10초
        factory.setReadTimeout(30000);     // 30초

        return builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .messageConverters(jacksonConverter)
                .build();
    }
}
