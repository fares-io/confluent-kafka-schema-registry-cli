package io.fares.bind.kscharc.tf;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {
  org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration.class
})
@Import({
  ApplicationConfig.class,
  ApplicationRuntimeHints.class
})
public class Application {

  static {
    // classgraph uses jul and somehow the root logger does not get properly cleared and re-installed by SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
