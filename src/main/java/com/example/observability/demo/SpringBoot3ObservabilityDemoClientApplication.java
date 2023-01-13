package com.example.observability.demo;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class SpringBoot3ObservabilityDemoClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringBoot3ObservabilityDemoClientApplication.class, args);
  }

  @Bean
  RestTemplate restTemplate(final RestTemplateBuilder builder) {
    return builder.build();
  }

  @Bean
  CommandLineRunner myCommandLineRunner(final ObservationRegistry registry, final RestTemplate restTemplate) {
    final var highCardinalityValues = new Random(); // Simulates potentially large number of values
    return args -> {
      String albumId = String.valueOf(highCardinalityValues.nextLong(100));

      // <demo.observation> is a "technical" name that does not depend on the context. It will be used to name e.g. Metrics
      Observation.createNotStarted("demo.observation", registry)
          // Low cardinality means that the number of potential values won't be big. Low cardinality entries will end up in e.g. Metrics
          .lowCardinalityKeyValue("lowCKV", "fakeValue")
          // High cardinality means that the number of potential values can be large. High cardinality entries will end up in e.g. Spans
          .highCardinalityKeyValue("albumId", albumId)
          // <command-line-runner> is a "contextual" name that gives more details within the provided context. It will be used to name e.g. Spans
          .contextualName("command-line-runner")
          // The following lambda will be executed with an observation scope (e.g. all the MDC entries will be populated with tracing information). Also, the observation will be started, stopped and if an error occurred it will be recorded on the observation
          .observe(() -> {
            log.info("Will send a request to the server"); // Since we're in an observation scope - this log line will contain tracing MDC entries ...
            String response = restTemplate.getForObject("http://localhost:8081/albums/{albumId}", String.class, albumId); // Boot's RestTemplate instrumentation creates a child span here
            log.info("Got response [{}]", response); // ... so will this line
          });

    };
  }
}
