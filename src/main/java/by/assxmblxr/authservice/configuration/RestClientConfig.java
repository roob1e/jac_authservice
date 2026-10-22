package by.assxmblxr.authservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
  @Bean
  public RestClient userServiceRestClient(RestClient.Builder builder, @Value("${user-service.url}") String url) {
    return builder
            .baseUrl(url)
            .build();
  }
}
