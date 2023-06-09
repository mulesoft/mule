/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.api.service.config.ContainerServiceConfigurator;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.serializer.ArtifactObjectSerializer;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationMuleContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultMuleApplication;
import org.mule.runtime.module.deployment.impl.internal.application.PolicyMuleContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainMuleContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.policy.ArtifactExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.DefaultExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.config.MuleProperties.*;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.*;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.getMuleContext;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.isConfigLess;

/**
 * Builder for creating an {@link ArtifactContext}. This is the preferred mechanism to create a {@code ArtifactContext} and a
 * {@link MuleContext} that can be retrieved from the {@link ArtifactContext} by calling {@link ArtifactContext#getMuleContext()}
 *
 * @since 4.0
 */
public class VoltronArtifactContextBuilder {

  protected static final String EXECUTION_CLASSLOADER_WAS_NOT_SET = "Execution classloader was not set";
  protected static final String MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL =
      "MuleContext artifact properties cannot be null";
  protected static final String INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY = "installation directory must be a directory";
  protected static final String ONLY_APPLICATIONS_OR_POLICIES_ARE_ALLOWED_TO_HAVE_A_PARENT_ARTIFACT =
      "Only applications or policies are allowed to have a parent artifact";
  protected static final String SERVICE_REPOSITORY_CANNOT_BE_NULL = "serviceRepository cannot be null";
  protected static final String EXTENSION_MODEL_LOADER_REPOSITORY_CANNOT_BE_NULL =
      "extensionModelLoaderRepository cannot be null";
  protected static final String CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL = "classLoaderRepository cannot be null";
  protected static final String CLASS_LOADER_REPOSITORY_WAS_NOT_SET = "classLoaderRepository was not set";
  protected static final String SERVICE_CONFIGURATOR_CANNOT_BE_NULL = "serviceConfigurator cannot be null";

  private ArtifactConfigurationProcessor artifactConfigurationProcessor;
  private Map<String, String> artifactProperties = new HashMap<>();
  private String artifactName = getUUID();
  private MuleContextBuilder muleContextBuilder;
  private ClassLoader executionClassLoader;
  private MuleContextListener muleContextListener;
  private String defaultEncoding;
  private ServiceRepository serviceRepository = Collections::emptyList;
  private ExtensionModelLoaderRepository extensionModelLoaderRepository = loaderDescriber -> empty();
  private List<ConfigurationBuilder> additionalBuilders = emptyList();
  private ClassLoaderRepository classLoaderRepository;
  private final List<ServiceConfigurator> serviceConfigurators = new ArrayList<>();
  private ExtensionManagerFactory extensionManagerFactory;
  private DeployableArtifact<?> parentArtifact;
  private Optional<Properties> properties = empty();
  private String dataFolderName;
  private LockFactory runtimeLockFactory;
  private MemoryManagementService memoryManagementService;
  private ExpressionLanguageMetadataService expressionLanguageMetadataService;

  private VoltronArtifactContextBuilder() {}

  /**
   * @return a new builder to create a {@link ArtifactContext} instance.
   */
  public static VoltronArtifactContextBuilder newBuilder() {
    return new VoltronArtifactContextBuilder();
  }

  /**
   * @return a new builder to create a {@link ArtifactContext} instance.
   */
  public static VoltronArtifactContextBuilder newBuilder(ConfigurationBuilder... additionalBuilders) {
    final VoltronArtifactContextBuilder builder = new VoltronArtifactContextBuilder();
    builder.setAdditionalBuilders(asList(additionalBuilders));
    return builder;
  }

  private void setAdditionalBuilders(List<ConfigurationBuilder> additionalBuilders) {
    this.additionalBuilders = additionalBuilders;
  }

  public VoltronArtifactContextBuilder setProperties(Optional<Properties> properties) {
    this.properties = properties;
    return this;
  }

  /**
   * @param folderName the folder name to use to store data in the file system related to the application.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setDataFolderName(String folderName) {
    this.dataFolderName = folderName;
    return this;
  }

  /**
   * Configures the {@link ArtifactConfigurationProcessor} to use.
   * 
   * @param artifactConfigurationProcessor the processor to use for building the application model.
   * @return the builder
   * 
   * @since 4.5
   */
  public VoltronArtifactContextBuilder setArtifactConfigurationProcessor(ArtifactConfigurationProcessor artifactConfigurationProcessor) {
    this.artifactConfigurationProcessor = artifactConfigurationProcessor;
    return this;
  }

  /**
   * Allows to define a parent artifact which resources will be available to the context to be created. This is the mechanism
   * using for {@link Domain}s to define shared resources.
   *
   * @param parentArtifact artifact parent of the one being created.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setParentArtifact(DeployableArtifact<?> parentArtifact) {
    this.parentArtifact = parentArtifact;
    return this;
  }

  /**
   * The artifact properties define key value pairs that can be referenced from within the configuration files.
   *
   * @param artifactProperties properties use for the artifact configuration
   * @return the builder
   */
  public VoltronArtifactContextBuilder setArtifactProperties(Map<String, String> artifactProperties) {
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
  public VoltronArtifactContextBuilder setArtifactName(String artifactName) {
    this.artifactName = artifactName;
    return this;
  }

  /**
   * Allows to set a listener that will be notified when the {@code MuleContext} is created, initialized or configured.
   *
   * @param muleContextListener listener of {@code MuleContext} notifications.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setMuleContextListener(MuleContextListener muleContextListener) {
    this.muleContextListener = muleContextListener;
    return this;
  }

  /**
   * Sets the classloader that must be used to execute all {@code MuleContext} tasks such as running flows, doing connection
   * retries, etc.
   *
   * @param classloader classloader to use for executing logic within the {@code MuleContext}
   * @return the builder
   */
  public VoltronArtifactContextBuilder setExecutionClassloader(ClassLoader classloader) {
    this.executionClassLoader = classloader;
    return this;
  }

  /**
   * Sets the default encoding for the {@code MuleContext} if the use did not define one explicitly within the configuration.
   *
   * @param defaultEncoding default encoding to use within the {@code MuleContext}
   * @return the builder
   */
  public VoltronArtifactContextBuilder setDefaultEncoding(String defaultEncoding) {
    this.defaultEncoding = defaultEncoding;
    return this;
  }

  /**
   * Provides a {@link ServiceRepository} containing all the services that will be accessible from the {@link MuleContext} to be
   * created.
   *
   * @param serviceRepository repository of available services. Non null.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setServiceRepository(ServiceRepository serviceRepository) {
    checkArgument(serviceRepository != null, SERVICE_REPOSITORY_CANNOT_BE_NULL);
    this.serviceRepository = serviceRepository;
    return this;
  }

  /**
   * Sets a {@link ExtensionModelLoaderRepository} that allows to retrieve the available extension loaders.
   *
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setExtensionModelLoaderRepository(ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    checkArgument(extensionModelLoaderRepository != null, EXTENSION_MODEL_LOADER_REPOSITORY_CANNOT_BE_NULL);
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    return this;
  }

  /**
   * Provides a {@link ClassLoaderRepository} containing all registered class loaders on the container.
   *
   * @param classLoaderRepository repository of available class loaders. Non null.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setClassLoaderRepository(ClassLoaderRepository classLoaderRepository) {
    checkState(classLoaderRepository != null, CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL);
    this.classLoaderRepository = classLoaderRepository;
    return this;
  }

  /**
   * Adds a service configurator to configure the created context.
   *
   * @param serviceConfigurator used to configure the create context. Non null.
   * @return the builder
   */
  public VoltronArtifactContextBuilder withServiceConfigurator(ServiceConfigurator serviceConfigurator) {
    checkState(serviceConfigurator != null, SERVICE_CONFIGURATOR_CANNOT_BE_NULL);
    this.serviceConfigurators.add(serviceConfigurator);
    return this;
  }

  /**
   * @param runtimeLockFactory {@link LockFactory} for the runtime that can be shared along deployable artifacts to synchronize
   *                           access on different deployable artifacts to the same resources.
   * @return the builder
   */
  public VoltronArtifactContextBuilder setRuntimeLockFactory(LockFactory runtimeLockFactory) {
    this.runtimeLockFactory = runtimeLockFactory;
    return this;
  }

  private Map<String, String> merge(Map<String, String> properties, Properties deploymentProperties) {
    if (deploymentProperties == null) {
      return properties;
    }

    Map<String, String> mergedProperties = new HashMap<>(properties);
    for (Map.Entry<Object, Object> entry : deploymentProperties.entrySet()) {
      mergedProperties.put(entry.getKey().toString(), entry.getValue().toString());
    }

    return mergedProperties;
  }

  /**
   * @return the {@code MuleContext} created with the provided configuration
   * @throws ConfigurationException  when there's a problem creating the {@code MuleContext}
   * @throws InitialisationException when a certain configuration component failed during initialisation phase
   */
  public ArtifactContext build() throws InitialisationException, ConfigurationException {
    checkState(executionClassLoader != null, EXECUTION_CLASSLOADER_WAS_NOT_SET);
    checkState(classLoaderRepository != null, CLASS_LOADER_REPOSITORY_WAS_NOT_SET);
    // checkState(POLICY.equals(artifactType) || APP.equals(artifactType) || parentArtifact == null,
    // ONLY_APPLICATIONS_OR_POLICIES_ARE_ALLOWED_TO_HAVE_A_PARENT_ARTIFACT);
    try {
      return withContextClassLoader(executionClassLoader, () -> {
        List<ConfigurationBuilder> builders = new LinkedList<>(additionalBuilders);
        // TODO review if we need this based on the plugins shipped with integration orchestartor
        // builders.add(new ArtifactBootstrapServiceDiscovererConfigurationBuilder(artifactPlugins));
        boolean hasEmptyParentDomain = isConfigLess(parentArtifact);
        if (extensionManagerFactory == null) {
          // TODO commenting this for now until we check that the domain approach works for the parent mule context
          // MuleContext parentMuleContext = getMuleContext(parentArtifact).orElse(null);
          // if (parentMuleContext == null || hasEmptyParentDomain) {
          // extensionManagerFactory =
          // new ArtifactExtensionManagerFactory(emptyList(), extensionModelLoaderRepository,
          // new DefaultExtensionManagerFactory());
          // } else {
          extensionManagerFactory = new CompositeArtifactExtensionManagerFactory(parentArtifact, extensionModelLoaderRepository,
                                                                                 emptyList(),
                                                                                 new DefaultExtensionManagerFactory());
          // }

        }

        // TODO review if we need this
        builders.add(new ArtifactExtensionManagerConfigurationBuilder(emptyList(),
                                                                      extensionManagerFactory));
        // TODO review if we need this for voltron shipped plugins or app
        builders.add(createConfigurationBuilderFromApplicationProperties());

        AtomicReference<ArtifactContext> artifactContext = new AtomicReference<>();
        builders.add(new ConfigurationBuilder() {

          @Override
          public void configure(MuleContext muleContext) throws ConfigurationException {
            if (serviceRepository != null) {
              // TODO we should be able to reuse one of this for all the apps - probably already settings services in parent
              // MuleContext
              serviceConfigurators.add(new ContainerServiceConfigurator(serviceRepository.getServices()));
            }
            // if (classLoaderRepository != null) {
            // serviceConfigurators.add(customizationService -> customizationService
            // .registerCustomServiceImpl(OBJECT_CLASSLOADER_REPOSITORY, classLoaderRepository));
            // }
            ArtifactContextConfiguration.ArtifactContextConfigurationBuilder artifactContextConfigurationBuilder =
                ArtifactContextConfiguration.builder()
                    .setMuleContext(muleContext)
                    .setArtifactProperties(merge(artifactProperties, muleContext.getDeploymentProperties()))
                    .setArtifactType(APP)
                    .setEnableLazyInitialization(false)
                    .setDisableXmlValidations(true)
                    .setServiceConfigurators(serviceConfigurators)
                    .setRuntimeLockFactory(runtimeLockFactory)
                    .setMemoryManagementService(memoryManagementService)
                    .setExpressionLanguageMetadataService(expressionLanguageMetadataService);

            // if (parentArtifact != null && parentArtifact.getArtifactContext() != null) {
            artifactContextConfigurationBuilder.setParentArtifactContext(parentArtifact.getArtifactContext());
            // }

            artifactContext
                .set(artifactConfigurationProcessor.createArtifactContext(artifactContextConfigurationBuilder.build()));
            ((DefaultMuleConfiguration) muleContext.getConfiguration()).setDataFolderName(dataFolderName);
          }

          @Override
          public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
            // Nothing to do
          }
        });
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        if (muleContextListener != null) {
          muleContextFactory.addListener(muleContextListener);
        }
        muleContextBuilder = new ApplicationMuleContextBuilder(artifactName, artifactProperties, defaultEncoding);
        muleContextBuilder.setExecutionClassLoader(this.executionClassLoader);
        ArtifactObjectSerializer objectSerializer = new ArtifactObjectSerializer(classLoaderRepository);
        muleContextBuilder.setObjectSerializer(objectSerializer);
        muleContextBuilder.setDeploymentProperties(properties);

        if (parentArtifact != null) {
          builders.add(new ConnectionManagerConfigurationBuilder(parentArtifact));
        } else {
          builders.add(new ConnectionManagerConfigurationBuilder());
        }

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
    artifactProperties.put(APP_NAME_PROPERTY, artifactName);
    return new SimpleConfigurationBuilder(artifactProperties);
  }

  public VoltronArtifactContextBuilder setExtensionManagerFactory(ExtensionManagerFactory extensionManagerFactory) {
    this.extensionManagerFactory = extensionManagerFactory;

    return this;
  }

  public VoltronArtifactContextBuilder setMemoryManagementService(MemoryManagementService memoryManagementService) {
    this.memoryManagementService = memoryManagementService;
    return this;
  }

  public VoltronArtifactContextBuilder setExpressionLanguageMetadataService(ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
    return this;
  }
}
