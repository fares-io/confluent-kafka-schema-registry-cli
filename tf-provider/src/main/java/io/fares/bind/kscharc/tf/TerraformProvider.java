package io.fares.bind.kscharc.tf;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.grpc.server.service.GrpcService;
import terraform.plugin.v6.*;

@GrpcService
public class TerraformProvider extends ProviderGrpc.ProviderImplBase implements ApplicationContextAware {

  private static final Logger log = LoggerFactory.getLogger(TerraformProvider.class);

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void hello(@NotNull Hello.Request request, @NotNull StreamObserver<Hello.Response> responseObserver) {
    Hello.Response response = Hello.Response.newBuilder()
      .setGreeting("Hello " + request.getName())
      .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getMetadata(GetMetadata.Request request, @NotNull StreamObserver<GetMetadata.Response> responseObserver) {

    GetMetadata.Response response = GetMetadata.Response.newBuilder()
      // can show plan for destroy but for now not going to allow schema caching
      .setServerCapabilities(ServerCapabilities.newBuilder()
        .setPlanDestroy(true)
        .setGetProviderSchemaOptional(false)
        .build())
      // diagnostics
      // define data sources
      .addDataSources(GetMetadata.DataSourceMetadata.newBuilder().setTypeName("kscharc_model"))
//      .addDataSources(GetMetadata.DataSourceMetadata.newBuilder().setTypeName("schema"))
//      .addDataSources(GetMetadata.DataSourceMetadata.newBuilder().setTypeName("topic"))
      .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getProviderSchema(GetProviderSchema.Request request, @NotNull StreamObserver<GetProviderSchema.Response> responseObserver) {

    GetProviderSchema.Response response = GetProviderSchema.Response.newBuilder()
      // the definition of the provider block schema
      .setProvider(Schema.newBuilder()
        .setVersion(1)
        .setBlock(Schema.NestedBlock.newBuilder().getBlockBuilder().setDescription("kscharc provider description"))
      )
      // the definition of the data sources schema
      .putDataSourceSchemas("kscharc_model", Schema.newBuilder()
        .setVersion(1)
        .setBlock(Schema.NestedBlock.newBuilder().getBlockBuilder()
          .setVersion(1)
          .setDescription("kscharc model datasource description"))
        .build()
      )

      // diagnostics

      // server capabilities
      .setServerCapabilities(ServerCapabilities.newBuilder().setPlanDestroy(true).setGetProviderSchemaOptional(false))
      .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();

  }

  @Override
  public void validateProviderConfig(ValidateProviderConfig.Request request, StreamObserver<ValidateProviderConfig.Response> responseObserver) {
    ValidateProviderConfig.Response response = ValidateProviderConfig.Response.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void validateDataResourceConfig(ValidateDataResourceConfig.Request request, StreamObserver<ValidateDataResourceConfig.Response> responseObserver) {
    ValidateDataResourceConfig.Response response = ValidateDataResourceConfig.Response.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void validateResourceConfig(ValidateResourceConfig.Request request, StreamObserver<ValidateResourceConfig.Response> responseObserver) {
    ValidateResourceConfig.Response response = ValidateResourceConfig.Response.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void configureProvider(ConfigureProvider.Request request, StreamObserver<ConfigureProvider.Response> responseObserver) {

    log.atDebug().setMessage("terraform version {}").addArgument(request::getTerraformVersion).log();

    DynamicValue config = request.getConfig();

    log.atDebug().setMessage("configuration type {}").addArgument(() -> {
      if (config.getMsgpack() != ByteString.EMPTY) {
        return "msgpack";
      } else if (config.getJson() != ByteString.EMPTY) {
        return "json";
      } else {
        return "empty";
      }
    }).log();

    ConfigureProvider.Response response = ConfigureProvider.Response.newBuilder()
      .addDiagnostics(
        Diagnostic.newBuilder()
          .setSeverityValue(Diagnostic.Severity.WARNING_VALUE)
          .setSummary("terraform version " + request.getTerraformVersion() + " too low")
          .setDetail("example warning in response to the provider block configuration values")
          .setAttribute(AttributePath.newBuilder()
            .addSteps(AttributePath.Step.newBuilder()
              .setAttributeName("kscharc_provider_attribute_1")))
      )
      .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void readDataSource(ReadDataSource.Request request, StreamObserver<ReadDataSource.Response> responseObserver) {
    ReadDataSource.Response response = ReadDataSource.Response.newBuilder().build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void stopProvider(StopProvider.Request request, StreamObserver<StopProvider.Response> responseObserver) {
    StopProvider.Response response = StopProvider.Response.newBuilder().build();
    responseObserver.onNext(response);
    if (applicationContext instanceof ConfigurableApplicationContext ctx) {
      int exitCode = SpringApplication.exit(ctx, () -> 0);
      System.exit(exitCode);
    }
    responseObserver.onCompleted();
  }

}
