package io.fares.bind.kscharc.tf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;
import terraform.plugin.v6.GetMetadata;
import terraform.plugin.v6.ProviderGrpc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TerraformProviderApplicationTest {

  @Autowired
  private ProviderGrpc.ProviderBlockingStub stub;

  @Test
  @DirtiesContext
  void contextLoads() {
  }

  @Test
  @DirtiesContext
  void serverResponds() {
    GetMetadata.Response response = stub.getMetadata(GetMetadata.Request.newBuilder().build());
    assertThat(response)
      .isNotNull()
      .extracting(GetMetadata.Response::hasServerCapabilities).isEqualTo(true);
    assertThat(response.hasServerCapabilities()).isTrue();
    assertThat(response.getServerCapabilities().getPlanDestroy()).isTrue();
  }

  @TestConfiguration
  static class ExtraConfiguration {

    @Bean
    @Lazy
    ProviderGrpc.ProviderBlockingStub stub(GrpcChannelFactory channels, @LocalGrpcPort int port) {
      return ProviderGrpc.newBlockingStub(channels.createChannel("0.0.0.0:" + port).build());
    }

  }

}
