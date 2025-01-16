package io.fares.bind.kscharc.tf;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycleEvent;

public class GrcpServerStartListener implements ApplicationListener<GrpcServerLifecycleEvent> {

  private final boolean providerDebug;

  private final byte[] clientCertificate;

  public GrcpServerStartListener() {
    this(false, null);
  }

  public GrcpServerStartListener(boolean providerDebug, byte[] clientCertificate) {
    this.providerDebug = providerDebug;
    this.clientCertificate = clientCertificate;
  }


  @Override
  public void onApplicationEvent(@NotNull GrpcServerLifecycleEvent event) {


    if (providerDebug) {

      long pid = ProcessHandle.current().pid();

      String address = String.format(
        "export TF_REATTACH_PROVIDERS='{\"%s\":{\"Protocol\":\"%s\",\"ProtocolVersion\":%d,\"Pid\":%d,\"Test\":true,\"Addr\":{\"Network\":\"%s\",\"String\":\"%s:%d\"}}}'",
        "local/fares-io/kscharc",
        "grpc", // protocol
        6, // protocol version
        pid,
        "tcp", // network
        "127.0.0.1", // host
        event.getServer().getPort());

      System.out.println(address);

    } else {

      String address = String.format("%d|%d|%s|%s:%d|%s|%s",
        1,                            // 0 core protocol version
        6,                            // 1 plugin protocol version
        "tcp",                        // 2 network
        "127.0.0.1",                  // 3 host
        event.getServer().getPort(),  // 3 host port
        "grpc",                       // 4 communication protocol
        new String(clientCertificate) // 5 base64 encoded server certificate
      );

      System.out.println(address);

    }

  }

}
