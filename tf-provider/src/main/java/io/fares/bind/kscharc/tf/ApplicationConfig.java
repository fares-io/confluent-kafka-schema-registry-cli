package io.fares.bind.kscharc.tf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import java.io.IOException;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApplicationProperties.class)
class ApplicationConfig {

  @Bean
  GrpcExceptionHandler globalInterceptor() {
    return exception -> {
      if (exception instanceof IllegalArgumentException) {
        return Status.INVALID_ARGUMENT.withDescription(exception.getMessage());
      }
      return null;
    };
  }

  @Bean
  GrcpServerStartListener grcpServerStartListener(ApplicationProperties properties, ResourceLoader resourceLoader) throws IOException {
    // we need to serve the server pub certificate in base64
    Resource certFile = resourceLoader.getResource("classpath:certificates/server.b64");
//    byte[] certBase64 = Base64.getEncoder().encode(certFile.getContentAsByteArray());
    return new GrcpServerStartListener(properties.isProviderDebug(), certFile.getContentAsByteArray());
  }

  @Bean
  JsonFactory jsonFactory() {
    return new MessagePackFactory();
  }

  @Bean
  @Primary
  ObjectMapper messagePackObjectMapper(JsonFactory jsonFactory) {
    return new ObjectMapper(jsonFactory);
  }

}

@ConfigurationProperties(prefix = "kscharc")
class ApplicationProperties {

  boolean providerDebug = false;

  public boolean isProviderDebug() {
    return providerDebug;
  }

  public void setProviderDebug(boolean providerDebug) {
    this.providerDebug = providerDebug;
  }

}

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(ApplicationRuntimeHints.class)
class ApplicationRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.resources()
      .registerPattern("certificates/*")
      .registerPattern("*.proto");
  }

}
