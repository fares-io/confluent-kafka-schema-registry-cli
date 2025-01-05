package io.fares.bind.kafka.cli;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.fares.bind.kafka.KScharc;
import io.fares.bind.maven.Aether;
import io.fares.bind.maven.ClassLoaderProvider;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.*;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.repository.*;
import org.jetbrains.annotations.NotNull;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.command.annotation.EnableCommand;
import org.springframework.shell.jline.PromptProvider;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory.newClient;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.forceDeleteOnExit;
import static org.apache.commons.io.FileUtils.getUserDirectoryPath;
import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableConfigurationProperties({ ApplicationProperties.class, JFrogProperties.class, MavenProperties.class })
@EnableCommand({ MavenCommands.class, SchemaCommands.class })
class ApplicationConfiguration {

  private static final Logger log = getLogger(ApplicationConfiguration.class);

  private final MavenProperties mavenProperties;

  public ApplicationConfiguration(MavenProperties mavenProperties) {
    this.mavenProperties = mavenProperties;
  }

  // region maven system configuration

  /**
   * Create the module that is used to interact with the maven repository system.
   *
   * @param system   the repository system
   * @param session  the repository system session for the cli invocation
   * @param settings the maven settings used to configure authentication, proxies, mirrors etc.
   * @return the aether module
   */
  @Bean
  @NotNull Aether aether(RepositorySystem system, RepositorySystemSession session, Settings settings) {
    return new Aether(system, session, settings);
  }

  /**
   * The {@link RepositorySystem} provider used to create an embedded maven system.
   *
   * @return the supplier
   */
  @Bean
  @ConditionalOnMissingBean
  RepositorySystemSupplier repositorySystemSupplier() {
    return new RepositorySystemSupplier();
  }

  /**
   * Create the embedded maven repository system used to interact with local and remote maven repositories.
   *
   * @param supplier the supplier used to create the system
   * @return
   */
  @Bean(destroyMethod = "shutdown")
  RepositorySystem repositorySystem(RepositorySystemSupplier supplier) {
    return supplier.get();
  }

  @Bean
  RepositorySystemSession repositorySystemSession(RepositorySystem repositorySystem,
                                                  ProxySelector proxySelector,
                                                  AuthenticationSelector authenticationSelector) {

    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    // configure the listener used to report artifact downloads
    session.setTransferListener(new Slf4jMavenTransferListener(getLogger("io.fares.kscharc.cli")));

    // configure the proxy selector from the settings
    session.setProxySelector(proxySelector);

    // configure the authentication selector needed to talk to the repository
    session.setAuthenticationSelector(authenticationSelector);

    // last step is to set up the local repository manager for the maven session
    LocalRepository localRepository = new LocalRepository(mavenProperties.getLocalRepositoryLocation());
    LocalRepositoryManager localRepositoryManager = repositorySystem.newLocalRepositoryManager(session, localRepository);
    session.setLocalRepositoryManager(localRepositoryManager);

    log.atDebug().setMessage("local repository location: {}").addArgument(session::getLocalRepository).log();

    return session;

  }

  @Bean
  ProxySelector proxySelector(Settings settings) {

    Proxy proxyConfiguration = settings.getActiveProxy();

    if (proxyConfiguration != null) {
      final DefaultProxySelector settingsProxySelector = new DefaultProxySelector();
      settingsProxySelector.add(
        new org.eclipse.aether.repository.Proxy(
          proxyConfiguration.getProtocol(),
          proxyConfiguration.getHost(),
          proxyConfiguration.getPort(),
          new AuthenticationBuilder().addUsername(proxyConfiguration.getUsername()).addPassword(proxyConfiguration.getPassword()).build()
        ),
        proxyConfiguration.getNonProxyHosts()
      );
      return settingsProxySelector;
    } else {
      return new JreProxySelector();
    }

  }

  @Bean
  MirrorSelector mirrorSelector(Settings settings) {
    DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();

    settings.getMirrors().forEach(
      mirror -> mirrorSelector.add(mirror.getId(), mirror.getUrl(), null, true, mirror.isBlocked(), mirror.getMirrorOf(), mirror.getMirrorOfLayouts())
    );

    return mirrorSelector;

  }

  @Bean
  AuthenticationSelector authenticationSelector(Settings settings) {
    DefaultAuthenticationSelector authenticationSelector = new DefaultAuthenticationSelector();
    for (Server server : settings.getServers()) {
      authenticationSelector.add(server.getId(), new AuthenticationBuilder().addUsername(server.getUsername()).addPassword(server.getPassword()).build());
    }
    return authenticationSelector;
  }

  @Bean
  @ConditionalOnProperty(name = "maven.prefer-user-settings", havingValue = "true")
  Settings mavenSettingsFromUserContext() throws SettingsBuildingException {
    final SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
    final SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
    final String userSettingsFileName = System.getProperty("org.apache.maven.user-settings");
    if (userSettingsFileName == null) {
      File userSettingsFile = Path.of(System.getProperty("user.home"), ".m2", "settings.xml").toFile();
      request.setUserSettingsFile(userSettingsFile);
    } else {
      request.setUserSettingsFile(new File(userSettingsFileName));
    }
    final String global = System.getProperty("org.apache.maven.global-settings");
    if (global != null) {
      request.setGlobalSettingsFile(new File(global));
    }
    SettingsBuildingResult result = builder.build(request);
    return result.getEffectiveSettings();
  }

  @Bean
  @ConditionalOnProperty(name = "maven.prefer-user-settings", havingValue = "false", matchIfMissing = true)
  Settings mavenSettingsFromEnvironment(JFrogProperties properties) {

    Settings settings = new Settings();

    Repository repository = new Repository();
    repository.setId(properties.getRepositoryId());
    repository.setUrl(properties.getUrl());

    Server server = new Server();
    server.setId(repository.getId());
    server.setUsername(properties.getUsername());
    server.setPassword(properties.getReferenceToken());
    settings.addServer(server);

    Profile profile = new Profile();
    profile.setId("default");
    profile.setRepositories(singletonList(repository));
    settings.addProfile(profile);
    settings.addActiveProfile(profile.getId());

    return settings;

  }

  // endregion

  // region schema scanner configuration

  // TODO need a SchemaRegistryClient registryClient

  @Bean
  SchemaRegistryClient schemaRegistryClient() {
    return newClient(
      List.of("mock://localhost:8081"),
      1000,
      List.of(new AvroSchemaProvider(), new JsonSchemaProvider()),
      emptyMap(),
      emptyMap()
    );
  }

  @Bean
  KScharc kscharc(ClassLoaderProvider classLoaderProvider, SchemaRegistryClient schemaRegistryClient) {
    return new KScharc(classLoaderProvider::getClassLoader, schemaRegistryClient);
  }

  // endregion

  // region shell configuration

  @Bean
  ShellContextInitializer shellContextInitializer() {
    return new ShellContextInitializer();
  }

  @Bean
  public PromptProvider promptProvider() {
    return () -> new AttributedString("kscharc:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
  }

  // endregion

}

// region property settings

@ConfigurationProperties(prefix = "kscharc")
class ApplicationProperties {

}

@ConfigurationProperties(prefix = "maven")
class MavenProperties implements InitializingBean {

  /**
   * prefer the user settings from {@code ~/.m2/settings.xml} over a static configuration
   */
  private boolean preferUserSettings = true;

  // TODO property should be maven.local.repo

  /**
   * used to overwrite the location of the local repository, default {@code ~/.m2/repository}
   */
  private File localRepositoryLocation = Paths.get(getUserDirectoryPath())
    .resolve(".m2").resolve("repository").toAbsolutePath().toFile();

  private boolean preferTemporaryLocalRepository = true;

  public boolean isPreferUserSettings() {
    return preferUserSettings;
  }

  public void setPreferUserSettings(boolean preferUserSettings) {
    this.preferUserSettings = preferUserSettings;
  }

  public boolean isPreferTemporaryLocalRepository() {
    return preferTemporaryLocalRepository;
  }

  public void setPreferTemporaryLocalRepository(boolean preferTemporaryLocalRepository) {
    this.preferTemporaryLocalRepository = preferTemporaryLocalRepository;
  }

  public File getLocalRepositoryLocation() {
    return localRepositoryLocation;
  }

  public void setLocalRepositoryLocation(File localRepositoryLocation) {
    this.localRepositoryLocation = localRepositoryLocation;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

    // overwrite the default or any other provided settings with a temporary location
    if (preferTemporaryLocalRepository) {
      final File tempDirectory = Files.createTempDirectory("kscharc").toAbsolutePath().toFile();
      setLocalRepositoryLocation(tempDirectory);
      forceDeleteOnExit(tempDirectory);
    }

  }

}

@ConfigurationProperties(prefix = "jfrog")
class JFrogProperties implements InitializingBean {

  private String repositoryId = "artifactory";

  private String url;

  private String username;

  private String referenceToken;

  private String accessToken;

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getReferenceToken() {
    return referenceToken;
  }

  public void setReferenceToken(String referenceToken) {
    this.referenceToken = referenceToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

    if (url != null) {
      // when the url has no protocol, perhaps it's a local file
      URI uri = URI.create(url);
      if (uri.getScheme() == null) {
        // try to resolve the location to a local file
        File localFile = new File(uri.getPath());
        url = localFile.toURI().toString();
      }
    }
  }

}

// endregion
