package io.fares.bind.kafka.cli;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication
public class Application {

  static {
    // classgraph uses jul and somehow the root logger does not get properly cleared and re-installed by SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static void main(String[] args) {
    run(Application.class, args);
  }

}
