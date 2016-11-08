/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.config.MuleProperties.APP_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.APP_NAME_PROPERTY;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.UUID.getUUID;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.serializer.ArtifactObjectSerializer;
import org.mule.runtime.module.deployment.internal.application.ApplicationExtensionsManagerConfigurationBuilder;
import org.mule.runtime.module.deployment.internal.application.ApplicationMuleContextBuilder;
import org.mule.runtime.module.deployment.internal.domain.DomainMuleContextBuilder;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builder for creating an {@link ArtifactContext}. This is the preferred mechanism to create a {@code ArtifactContext} and a
 * {@link MuleContext} that can be retrieved from the {@link ArtifactContext} by calling {@link ArtifactContext#getMuleContext()}
 *
 * @since 4.0
 */
public class ArtifactContextBuilder {

  protected static final String EXECUTION_CLASSLOADER_WAS_NOT_SET = "Execution classloader was not set";
  protected static final String MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL =
      "MuleContext artifact properties cannot be null";
  protected static final String INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY = "installation directory must be a directory";
  protected static final String ONLY_APPLICATIONS_ARE_ALLOWED_TO_HAVE_A_PARENT_CONTEXT =
      "Only applications are allowed to have a parent context";
  protected static final String SERVICE_REPOSITORY_CANNOT_BE_NULL = "serviceRepository cannot be null";
  protected static final String CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL = "classLoaderRepository cannot be null";
  protected static final String CLASS_LOADER_REPOSITORY_WAS_NOT_SET = "classLoaderRepository was not set";

  private List<ArtifactPlugin> artifactPlugins = new ArrayList<>();
  private ArtifactType artifactType = APP;
  private String[] configurationFiles = new String[0];
  private ArtifactConfiguration artifactConfiguration;
  private Map<String, String> artifactProperties = new HashMap<>();
  private String artifactName = getUUID();
  private MuleContextBuilder muleContextBuilder;
  private ClassLoader executionClassLoader;
  private MuleContext parentContext;
  private File artifactInstallationDirectory;
  private MuleContextListener muleContextListener;
  private String defaultEncoding;
  private ServiceRepository serviceRepository = Collections::emptyList;
  private boolean enableLazyInit;
  private List<ConfigurationBuilder> additionalBuilders = emptyList();
  private ClassLoaderRepository classLoaderRepository;

  private ArtifactContextBuilder() {}

  /**
   * @return a new builder to create a {@link ArtifactContext} instance.
   */
  public static ArtifactContextBuilder newBuilder() {
    return new ArtifactContextBuilder();
  }

  /**
   * @return a new builder to create a {@link ArtifactContext} instance.
   */
  public static ArtifactContextBuilder newBuilder(ConfigurationBuilder... additionalBuilders) {
    final ArtifactContextBuilder builder = new ArtifactContextBuilder();
    builder.setAdditionalBuilders(asList(additionalBuilders));
    return builder;
  }

  private void setAdditionalBuilders(List<ConfigurationBuilder> additionalBuilders) {
    this.additionalBuilders = additionalBuilders;
  }

  /**
   * The {@code ArtifactType} defines the set of services that will be available in the {@code MuleContext}. For instance
   * {@code ArtifactType.DOMAIN} does not have any service required to execute flows.
   *
   * By default {@code ArtifactType.APP} will be used, making all services available.
   *
   * @param artifactType artifact type for which a {@code MuleContext} must be created.
   * @return the builder
   */
  public ArtifactContextBuilder setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
    return this;
  }

  /**
   * @param configurationFiles set of the artifact configuration files. These must be absolute paths.
   * @return the builder
   */
  public ArtifactContextBuilder setConfigurationFiles(String... configurationFiles) {
    this.configurationFiles = configurationFiles;
    return this;
  }

  /**
   * @param artifactConfiguration
   * @return
   */
  public ArtifactContextBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration) {
    this.artifactConfiguration = artifactConfiguration;
    return this;
  }

  /**
   * Allows to define a {@code MuleContext} which resources will be available to the context to be created. This is the mechanism
   * using for {@link Domain}s to define shared resources.
   *
   * @param parentContext {@code MuleContext} that is parent of the one to be created.
   * @return the builder
   */
  public ArtifactContextBuilder setParentContext(MuleContext parentContext) {
    this.parentContext = parentContext;
    return this;
  }

  /**
   * The artifact properties define key value pairs that can be referenced from within the configuration files.
   *
   * @param artifactProperties properties use for the artifact configuration
   * @return the builder
   */
  public ArtifactContextBuilder setArtifactProperties(Map<String, String> artifactProperties) {
    checkArgument(artifactProperties != null, MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL);
    this.artifactProperties = artifactProperties;
    return this;
  }

  /**
   * Sets a meaningful name to identify the artifact. If not provided a UUID will be used.
   *
   * @param artifactName name to use to identify the artifact.
   * @return the builder
   */
  public ArtifactContextBuilder setArtifactName(String artifactName) {
    this.artifactName = artifactName;
    return this;
  }

  /**
   * Allows to set a listener that will be notified when the {@code MuleContext} is created, initialized or configured.
   *
   * @param muleContextListener listener of {@code MuleContext} notifications.
   * @return the builder
   */
  public ArtifactContextBuilder setMuleContextListener(MuleContextListener muleContextListener) {
    this.muleContextListener = muleContextListener;
    return this;
  }

  /**
   * Sets the file location where the artifact is installed. Must be a directory.
   *
   * @param location directory where the artifact is installed
   * @return the builder
   */
  public ArtifactContextBuilder setArtifactInstallationDirectory(File location) {
    checkArgument(location.isDirectory(), INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY);
    this.artifactInstallationDirectory = location;
    return this;
  }

  /**
   * Sets the classloader that must be used to execute all {@code MuleContext} tasks such as running flows, doing connection
   * retries, etc.
   *
   * @param classloader classloader to use for executing logic within the {@code MuleContext}
   * @return the builder
   */
  public ArtifactContextBuilder setExecutionClassloader(ClassLoader classloader) {
    this.executionClassLoader = classloader;
    return this;
  }

  /**
   * Sets the default encoding for the {@code MuleContext} if the use did not define one explicitly within the configuration.
   *
   * @param defaultEncoding default encoding to use within the {@code MuleContext}
   * @return the builder
   */
  public ArtifactContextBuilder setDefaultEncoding(String defaultEncoding) {
    this.defaultEncoding = defaultEncoding;
    return this;
  }

  /**
   * Provides a list of {@link ArtifactPlugin} that describe all the extensions that need to be accessible by the
   * {@code MuleContext} to be created. It may also be that the configuration files make use of this extensions.
   *
   * @param artifactPlugins collection of artifact extensions that define resources as part of the {@code MuleContext} to be
   *        created.
   * @return the builder
   */
  public ArtifactContextBuilder setArtifactPlugins(List<ArtifactPlugin> artifactPlugins) {
    this.artifactPlugins = artifactPlugins;
    return this;
  }

  /**
   * Provides a {@link ServiceRepository} containing all the services that will be accessible from the {@link MuleContext} to be
   * created.
   *
   * @param serviceRepository repository of available services. Non null.
   * @return the builder
   */
  public ArtifactContextBuilder setServiceRepository(ServiceRepository serviceRepository) {
    checkArgument(serviceRepository != null, SERVICE_REPOSITORY_CANNOT_BE_NULL);
    this.serviceRepository = serviceRepository;
    return this;
  }

  /**
   * Allows to lazily create the artifact resources.
   *
   * @param enableLazyInit when true the artifact resources from the mule configuration won't be created at startup. The artifact
   *        components from the configuration will be created on demand when requested. For instance, when using
   *        {@link ArtifactContext#getConnectivityTestingService()} and then invoking
   *        {@link org.mule.runtime.core.api.connectivity.ConnectivityTestingService#testConnection(String)} will cause the
   *        creation of the component requested to do test connectivity, if it was not already created. when false, the
   *        application will be created completely at startup.
   * @return the builder
   */
  public ArtifactContextBuilder setEnableLazyInit(boolean enableLazyInit) {
    this.enableLazyInit = enableLazyInit;
    return this;
  }

  /**
   * Provides a {@link ClassLoaderRepository} containing all registered class loaders on the container.
   *
   * @param classLoaderRepository repository of available class loaders. Non null.
   * @return the builder
   */
  public ArtifactContextBuilder setClassLoaderRepository(ClassLoaderRepository classLoaderRepository) {
    checkState(classLoaderRepository != null, CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL);
    this.classLoaderRepository = classLoaderRepository;
    return this;
  }

  /**
   * @return the {@code MuleContext} created with the provided configuration
   * @throws ConfigurationException when there's a problem creating the {@code MuleContext}
   * @throws InitialisationException when a certain configuration component failed during initialisation phase
   */
  public ArtifactContext build() throws InitialisationException, ConfigurationException {
    checkState(executionClassLoader != null, EXECUTION_CLASSLOADER_WAS_NOT_SET);
    checkState(classLoaderRepository != null, CLASS_LOADER_REPOSITORY_WAS_NOT_SET);
    checkState(APP.equals(artifactType) || parentContext == null, ONLY_APPLICATIONS_ARE_ALLOWED_TO_HAVE_A_PARENT_CONTEXT);
    try {
      return withContextClassLoader(executionClassLoader, () -> {
        List<ConfigurationBuilder> builders = new LinkedList<>();
        builders.addAll(additionalBuilders);
        builders.add(new ArtifactBootstrapServiceDiscovererConfigurationBuilder(artifactPlugins));
        builders.add(new ApplicationExtensionsManagerConfigurationBuilder(artifactPlugins));
        builders.add(createConfigurationBuilderFromApplicationProperties());
        ArtifactConfigurationProcessor artifactConfigurationProcessor = ArtifactConfigurationProcessor.discover();
        AtomicReference<ArtifactContext> artifactContext = new AtomicReference<>();
        builders.add(new ConfigurationBuilder() {

          public boolean isConfigured;

          @Override
          public void configure(MuleContext muleContext) throws ConfigurationException {
            ArtifactContextConfiguration.ArtifactContextConfigurationBuilder artifactContextConfigurationBuilder =
                ArtifactContextConfiguration.builder()
                    .setMuleContext(muleContext)
                    .setConfigResources(configurationFiles)
                    .setArtifactConfiguration(artifactConfiguration)
                    .setArtifactProperties(artifactProperties)
                    .setArtifactType(artifactType)
                    .setEnableLazyInitialization(enableLazyInit)
                    .setServiceConfigurators(asList(new ContainerServicesMuleContextConfigurator(serviceRepository),
                                                    customizationService -> customizationService
                                                        .registerCustomServiceImpl(MuleProperties.OBJECT_CLASSLOADER_REPOSITORY,
                                                                                   classLoaderRepository)));
            if (parentContext != null) {
              artifactContextConfigurationBuilder.setParentContext(parentContext);
            }
            artifactContext
                .set(artifactConfigurationProcessor.createArtifactContext(artifactContextConfigurationBuilder.build()));
            isConfigured = true;
          }

          @Override
          public boolean isConfigured() {
            return isConfigured;
          }
        });
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        if (muleContextListener != null) {
          muleContextFactory.addListener(muleContextListener);
        }
        if (APP.equals(artifactType)) {
          muleContextBuilder = new ApplicationMuleContextBuilder(artifactName, artifactProperties, defaultEncoding);
        } else {
          muleContextBuilder = new DomainMuleContextBuilder(artifactName);
        }
        muleContextBuilder.setExecutionClassLoader(this.executionClassLoader);
        ArtifactObjectSerializer objectSerializer = new ArtifactObjectSerializer(classLoaderRepository);
        muleContextBuilder.setObjectSerializer(objectSerializer);

        try {
          muleContextFactory.createMuleContext(builders, muleContextBuilder);
          return artifactContext.get();
        } catch (InitialisationException e) {
          throw new ConfigurationException(e);
        }
      });
    } catch (MuleRuntimeException e) {
      // We need this exception to be thrown as they are since the are possible causes of connectivity errors
      if (e.getCause() instanceof InitialisationException) {
        throw (InitialisationException) e.getCause();
      }
      if (e.getCause() instanceof ConfigurationException) {
        throw (ConfigurationException) e.getCause();
      }
      throw e;
    }
  }

  protected ConfigurationBuilder createConfigurationBuilderFromApplicationProperties() {
    if (artifactInstallationDirectory != null) {
      artifactProperties.put(APP_HOME_DIRECTORY_PROPERTY, artifactInstallationDirectory.getAbsolutePath());
    }
    artifactProperties.put(APP_NAME_PROPERTY, artifactName);
    return new SimpleConfigurationBuilder(artifactProperties);
  }
}
