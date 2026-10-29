package by.assxmblxr.authservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {
  @Bean
  public WebClient userServiceWebClient(
          WebClient.Builder builder,
          @Value("${user-service.url}") String url,
          @Value("${user-service.timeout:5s}") Duration timeout
  ) {
    HttpClient httpClient = HttpClient.create().responseTimeout(timeout);
    return builder
            .baseUrl(url)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
  }
}
