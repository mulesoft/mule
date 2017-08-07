/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.spring.api.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.IMPORT_ELEMENT;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.MULE_IDENTIFIER;
import static org.mule.runtime.config.spring.internal.dsl.spring.BeanDefinitionFactory.SPRING_SINGLETON_OBJECT;
import static org.mule.runtime.config.spring.internal.dsl.spring.ComponentModelHelper.updateAnnotationValue;
import static org.mule.runtime.config.spring.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.config.spring.util.ComponentBuildingDefinitionUtils.registerComponentBuildingDefinitions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.config.spring.api.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.api.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.spring.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.api.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.api.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.config.spring.api.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.api.dsl.processor.xml.XmlApplicationServiceRegistry;
import org.mule.runtime.config.spring.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.spring.internal.dsl.model.MinimalApplicationModelGenerator;
import org.mule.runtime.config.spring.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.spring.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.spring.internal.dsl.model.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.config.spring.internal.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.spring.internal.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.spring.internal.processor.ComponentLocatorCreatePostProcessor;
import org.mule.runtime.config.spring.internal.processor.ContextExclusiveInjectorProcessor;
import org.mule.runtime.config.spring.internal.processor.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.spring.internal.processor.LifecycleStatePostProcessor;
import org.mule.runtime.config.spring.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.config.spring.internal.processor.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.spring.internal.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigResource;
import org.mule.runtime.core.api.config.RuntimeConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.registry.TransformerResolver;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.w3c.dom.Document;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context that allows resources to be loaded from the
 * Classpath of file system using the MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractXmlApplicationContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleArtifactContext.class);

  public static final String INNER_BEAN_PREFIX = "(inner bean)";

  protected final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry =
      new ComponentBuildingDefinitionRegistry();
  private final OptionalObjectsController optionalObjectsController;
  private final Map<String, String> artifactProperties;
  private final ArtifactDeclaration artifactDeclaration;
  private final XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader;
  private final Optional<ConfigurationProperties> parentConfigurationProperties;
  private final DefaultRegistry serviceDiscoverer;
  protected ApplicationModel applicationModel;
  protected MuleContext muleContext;
  private Resource[] artifactConfigResources;
  protected BeanDefinitionFactory beanDefinitionFactory;
  private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
  protected final XmlApplicationParser xmlApplicationParser;
  private ArtifactType artifactType;
  private List<ComponentIdentifier> componentNotSupportedByNewParsers = new ArrayList<>();
  private SpringConfigurationComponentLocator componentLocator = new SpringConfigurationComponentLocator();
  private List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext the {@link MuleContext} that own this context
   * @param artifactDeclaration the mule configuration defined programmatically
   * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *        org.mule.runtime.config.spring.internal.SpringRegistry
   * @param pluginsClassLoaders the classloades of the plugins included in the artifact, on hwich contexts the parsers will
   *        process.
   * @param parentConfigurationProperties
   * @since 3.7.0
   */
  public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             List<ClassLoader> pluginsClassLoaders,
                             Optional<ConfigurationProperties> parentConfigurationProperties)
      throws BeansException {
    this(muleContext, convert(artifactConfigResources), artifactDeclaration, optionalObjectsController,
         parentConfigurationProperties, artifactProperties,
         artifactType, pluginsClassLoaders);
  }

  public MuleArtifactContext(MuleContext muleContext, Resource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Optional<ConfigurationProperties> parentConfigurationProperties,
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             List<ClassLoader> pluginsClassLoaders) {
    checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = muleContext;
    this.artifactConfigResources = artifactConfigResources;
    this.optionalObjectsController = optionalObjectsController;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.artifactDeclaration = artifactDeclaration;
    this.parentConfigurationProperties = parentConfigurationProperties;
    this.xmlConfigurationDocumentLoader = newXmlConfigurationDocumentLoader();
    this.serviceDiscoverer = new DefaultRegistry(muleContext);

    registerComponentBuildingDefinitions(serviceRegistry, MuleArtifactContext.class.getClassLoader(),
                                         componentBuildingDefinitionRegistry,
                                         getExtensionModels(muleContext.getExtensionManager()),
                                         (componentBuildingDefinitionProvider -> componentBuildingDefinitionProvider
                                             .getComponentBuildingDefinitions()));

    for (ClassLoader pluginArtifactClassLoader : pluginsClassLoaders) {
      registerComponentBuildingDefinitions(serviceRegistry, pluginArtifactClassLoader, componentBuildingDefinitionRegistry,
                                           getExtensionModels(muleContext.getExtensionManager()),
                                           (componentBuildingDefinitionProvider -> componentBuildingDefinitionProvider
                                               .getComponentBuildingDefinitions()));
    }

    xmlApplicationParser = createApplicationParser(pluginsClassLoaders);
    this.beanDefinitionFactory =
        new BeanDefinitionFactory(componentBuildingDefinitionRegistry, muleContext.getErrorTypeRepository());

    createApplicationModel();
    validateAllConfigElementHaveParsers();
  }

  private static Optional<Set<ExtensionModel>> getExtensionModels(ExtensionManager extensionManager) {
    return ofNullable(extensionManager == null ? null
        : extensionManager.getExtensions());
  }

  protected XmlConfigurationDocumentLoader newXmlConfigurationDocumentLoader() {
    return schemaValidatingDocumentLoader();
  }

  private XmlApplicationParser createApplicationParser(List<ClassLoader> pluginsClassLoaders) {
    ExtensionManager extensionManager = muleContext.getExtensionManager();

    ServiceRegistry customRegistry = extensionManager != null
        ? new XmlApplicationServiceRegistry(serviceRegistry, DslResolvingContext.getDefault(extensionManager.getExtensions()))
        : serviceRegistry;

    return new XmlApplicationParser(customRegistry, pluginsClassLoaders);
  }

  private void validateAllConfigElementHaveParsers() {
    applicationModel.executeOnEveryComponentTree(componentModel -> {
      Optional<ComponentIdentifier> parentIdentifierOptional = ofNullable(componentModel.getParent())
          .flatMap(parentComponentModel -> ofNullable(parentComponentModel.getIdentifier()));
      if (!beanDefinitionFactory.hasDefinition(componentModel.getIdentifier(), parentIdentifierOptional)) {
        componentNotSupportedByNewParsers.add(componentModel.getIdentifier());
        throw new RuntimeException(format("Invalid config '%s'", componentModel.getIdentifier()));
      }
    });
  }

  private void createApplicationModel() {
    try {
      ArtifactConfig artifactConfig = resolveArtifactConfig();
      Set<ExtensionModel> extensions =
          muleContext.getExtensionManager() != null ? muleContext.getExtensionManager().getExtensions() : emptySet();
      ResourceProvider externalResourceProvider = new ClassLoaderResourceProvider(muleContext.getExecutionClassLoader());
      applicationModel = new ApplicationModel(artifactConfig, artifactDeclaration, extensions,
                                              artifactProperties, parentConfigurationProperties,
                                              of(componentBuildingDefinitionRegistry),
                                              true, externalResourceProvider);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ArtifactConfig resolveArtifactConfig() throws IOException {
    ArtifactConfig.Builder applicationConfigBuilder = new ArtifactConfig.Builder();
    applicationConfigBuilder.setArtifactProperties(this.artifactProperties);

    List<Pair<String, InputStream>> initialConfigFiles = new ArrayList<>();
    for (Resource artifactConfigResource : artifactConfigResources) {
      initialConfigFiles.add(new Pair<>(getFilename(artifactConfigResource), artifactConfigResource.getInputStream()));
    }

    List<ConfigFile> configFiles = new ArrayList<>();
    recursivelyResolveConfigFiles(initialConfigFiles, configFiles).stream()
        .forEach(applicationConfigBuilder::addConfigFile);

    applicationConfigBuilder.setApplicationName(muleContext.getConfiguration().getId());
    return applicationConfigBuilder.build();
  }

  private List<ConfigFile> recursivelyResolveConfigFiles(List<Pair<String, InputStream>> configFilesToResolve,
                                                         List<ConfigFile> alreadyResolvedConfigFiles) {

    DefaultConfigurationPropertiesResolver propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(), new SystemPropertiesConfigurationProvider());

    ImmutableList.Builder<ConfigFile> resolvedConfigFilesBuilder =
        ImmutableList.<ConfigFile>builder().addAll(alreadyResolvedConfigFiles);
    configFilesToResolve.stream()
        .filter(fileNameInputStreamPair -> !alreadyResolvedConfigFiles.stream()
            .anyMatch(configFile -> configFile.getFilename().equals(fileNameInputStreamPair.getFirst())))
        .forEach(fileNameInputStreamPair -> {
          Document document =
              xmlConfigurationDocumentLoader.loadDocument(muleContext.getExtensionManager() == null ? emptySet()
                  : muleContext.getExtensionManager().getExtensions(),
                                                          fileNameInputStreamPair.getFirst(),
                                                          fileNameInputStreamPair.getSecond());
          ConfigLine mainConfigLine = xmlApplicationParser.parse(document.getDocumentElement()).get();
          ConfigFile configFile = new ConfigFile(fileNameInputStreamPair.getFirst(), asList(mainConfigLine));
          resolvedConfigFilesBuilder.add(configFile);
          try {
            fileNameInputStreamPair.getSecond().close();
          } catch (IOException e) {
            throw new MuleRuntimeException(e);
          }
        });

    ImmutableSet.Builder<String> importedFiles = ImmutableSet.builder();
    for (ConfigFile configFile : resolvedConfigFilesBuilder.build()) {
      List<ConfigLine> rootConfigLines = configFile.getConfigLines();
      ConfigLine muleRootElementConfigLine = rootConfigLines.get(0);
      importedFiles.addAll(muleRootElementConfigLine.getChildren().stream()
          .filter(configLine -> configLine.getNamespace().equals(CORE_PREFIX)
              && configLine.getIdentifier().equals(IMPORT_ELEMENT))
          .map(configLine -> {
            SimpleConfigAttribute fileConfigAttribute = configLine.getConfigAttributes().get("file");
            if (fileConfigAttribute == null) {
              throw new RuntimeConfigurationException(
                                                      createStaticMessage(format("<import> does not have a file attribute defined. At file '%s', at line %s",
                                                                                 configFile.getFilename(),
                                                                                 configLine.getLineNumber())));
            }
            return fileConfigAttribute.getValue();
          })
          .map(value -> (String) propertyResolver.resolveValue(value))
          .filter(fileName -> !alreadyResolvedConfigFiles.stream()
              .anyMatch(solvedConfigFile -> solvedConfigFile.getFilename().equals(fileName)))
          .collect(toList()));
    }

    Set<String> importedConfigurationFiles = importedFiles.build();

    if (importedConfigurationFiles.isEmpty()) {
      return resolvedConfigFilesBuilder.build();
    }

    List<Pair<String, InputStream>> newConfigFilesToResolved = importedConfigurationFiles.stream()
        .map(importedFileName -> {
          InputStream resourceAsStream = muleContext.getExecutionClassLoader().getResourceAsStream(importedFileName);
          if (resourceAsStream == null) {
            throw new RuntimeConfigurationException(createStaticMessage(format("Could not find imported resource '%s'",
                                                                               importedFileName)));
          }
          return (Pair<String, InputStream>) new Pair(importedFileName, resourceAsStream);
        }).collect(toList());

    return recursivelyResolveConfigFiles(newConfigFilesToResolved, resolvedConfigFilesBuilder.build());
  }

  private String getFilename(Resource resource) {
    if (resource instanceof ByteArrayResource) {
      return resource.getDescription();
    }
    return resource.getFilename();
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);

    registerEditors(beanFactory);

    registerAnnotationConfigProcessors((BeanDefinitionRegistry) beanFactory, beanFactory);

    addBeanPostProcessors(beanFactory,
                          new MuleContextPostProcessor(muleContext),
                          new GlobalNamePostProcessor(),
                          new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext.getRegistry(), beanFactory),
                          new DiscardedOptionalBeanPostProcessor(optionalObjectsController,
                                                                 (DefaultListableBeanFactory) beanFactory),
                          new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState()),
                          new ComponentLocatorCreatePostProcessor(componentLocator));

    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);

    prepareObjectProviders();
  }



  private void prepareObjectProviders() {
    MuleArtifactObjectProvider muleArtifactObjectProvider = new MuleArtifactObjectProvider(this);
    ImmutableObjectProviderConfiguration providerConfiguration =
        new ImmutableObjectProviderConfiguration(applicationModel.getConfigurationProperties(),
                                                 muleArtifactObjectProvider);
    for (ConfigurableObjectProvider objectProvider : objectProviders) {
      objectProvider.configure(providerConfiguration);
    }
  }

  /**
   * Process all the {@link ObjectProvider}s from the {@link ApplicationModel} to get their beans and register them inside the
   * spring bean factory so they can be used for dependency injection.
   *
   * @param beanFactory the spring bean factory where the objects will be registered.
   */
  private void registerObjectFromObjectProviders(ConfigurableListableBeanFactory beanFactory) {
    this.objectProviders.addAll(lookObjectProviders());
    ((ObjectProviderAwareBeanFactory) beanFactory).setObjectProviders(objectProviders);
    for (ObjectProvider objectProvider : objectProviders) {
      beanFactory
          .registerSingleton(objectProvider instanceof NamedObject ? ((NamedObject) objectProvider).getName() : UUID.getUUID(),
                             objectProvider);
    }
  }

  private List<ConfigurableObjectProvider> lookObjectProviders() {
    List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();
    applicationModel.executeOnEveryRootElement(componentModel -> {
      if (componentModel.getType() != null && ConfigurableObjectProvider.class.isAssignableFrom(componentModel.getType())) {
        objectProviders.add((ConfigurableObjectProvider) componentModel.getObjectInstance());
      }
    });
    return objectProviders;
  }

  private void registerEditors(ConfigurableListableBeanFactory beanFactory) {
    MulePropertyEditorRegistrar registrar = new MulePropertyEditorRegistrar();
    registrar.setMuleContext(muleContext);
    beanFactory.addPropertyEditorRegistrar(registrar);
  }

  private void addBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, BeanPostProcessor... processors) {
    for (BeanPostProcessor processor : processors) {
      beanFactory.addBeanPostProcessor(processor);
    }
  }

  @Override
  public void close() {
    super.close();
    beanDefinitionFactory.destroy();
  }

  public static Resource[] convert(ConfigResource[] resources) {
    Resource[] configResources = new Resource[resources.length];
    for (int i = 0; i < resources.length; i++) {
      ConfigResource resource = resources[i];
      if (resource.getUrl() != null) {
        configResources[i] = new UrlResource(resource.getUrl());
      } else {
        try {
          configResources[i] = new ByteArrayResource(IOUtils.toByteArray(resource.getInputStream()), resource.getResourceName());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return configResources;
  }

  @Override
  protected Resource[] getConfigResources() {
    return addAll(artifactConfigResources);
  }

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    createInitialApplicationComponents(beanFactory);
  }

  protected void createInitialApplicationComponents(DefaultListableBeanFactory beanFactory) {
    createApplicationComponents(beanFactory, applicationModel, true);
  }

  @Override
  public void destroy() {
    try {
      super.destroy();
    } catch (Exception e) {
      for (ObjectProvider objectProvider : objectProviders) {
        disposeIfNeeded(objectProvider, LOGGER);
      }
      throw new MuleRuntimeException(e);
    }
  }

  protected List<String> createApplicationComponents(DefaultListableBeanFactory beanFactory, ApplicationModel applicationModel,
                                                     boolean mustBeRoot) {
    List<String> createdComponentModels = new ArrayList<>();
    applicationModel.executeOnEveryMuleComponentTree(cm -> {
      SpringComponentModel componentModel = (SpringComponentModel) cm;
      if (!mustBeRoot || componentModel.isRoot()) {
        if (componentModel.getIdentifier().equals(MULE_IDENTIFIER) || !componentModel.isEnabled()) {
          return;
        }
        if (componentModel.getNameAttribute() != null) {
          createdComponentModels.add(componentModel.getNameAttribute());
        }
        beanDefinitionFactory
            .resolveComponentRecursively(componentModel.getParent() != null
                ? (SpringComponentModel) componentModel.getParent()
                : (SpringComponentModel) applicationModel
                    .getRootComponentModel(), componentModel, beanFactory, (resolvedComponentModel, registry) -> {
                      SpringComponentModel resolvedSpringComponentModel = (SpringComponentModel) resolvedComponentModel;
                      if (resolvedComponentModel.isRoot()) {
                        String nameAttribute = resolvedComponentModel.getNameAttribute();
                        if (resolvedComponentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
                          nameAttribute = OBJECT_MULE_CONFIGURATION;
                        } else if (nameAttribute == null) {
                          // This may be a configuration that does not requires a name.
                          nameAttribute = uniqueValue(resolvedSpringComponentModel.getBeanDefinition().getBeanClassName());
                        }
                        registry.registerBeanDefinition(nameAttribute, resolvedSpringComponentModel.getBeanDefinition());
                        postProcessBeanDefinition(componentModel, registry, nameAttribute);
                      }
                    }, null);
      }
    });
    registerObjectFromObjectProviders(beanFactory);
    return createdComponentModels;
  }

  @Override
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    super.customizeBeanFactory(beanFactory);
    new SpringMuleContextServiceConfigurator(muleContext, applicationModel.getConfigurationProperties(), artifactType,
                                             optionalObjectsController, beanFactory, componentLocator, serviceDiscoverer)
                                                 .createArtifactServices();
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    Optional<ComponentModel> configurationOptional =
        applicationModel.findComponentDefinitionModel(ApplicationModel.CONFIGURATION_IDENTIFIER);
    if (configurationOptional.isPresent()) {
      return;
    }
    BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
    beanDefinitionRegistry.registerBeanDefinition(OBJECT_MULE_CONFIGURATION,
                                                  genericBeanDefinition(MuleConfigurationConfigurator.class).getBeanDefinition());
  }

  private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
    registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      ConfigurationClassPostProcessor.class, null);
    registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      RequiredAnnotationBeanPostProcessor.class, null);
    registerInjectorProcessor(beanFactory);
  }

  protected void registerInjectorProcessor(ConfigurableListableBeanFactory beanFactory) {
    MuleInjectorProcessor muleInjectorProcessor = null;
    if (artifactType.equals(ArtifactType.APP)) {
      muleInjectorProcessor = new MuleInjectorProcessor();
    } else if (artifactType.equals(ArtifactType.DOMAIN)) {
      muleInjectorProcessor = new ContextExclusiveInjectorProcessor(this);
    }
    if (muleInjectorProcessor != null) {
      muleInjectorProcessor.setBeanFactory(beanFactory);
      beanFactory.addBeanPostProcessor(muleInjectorProcessor);
    }
  }

  private void registerAnnotationConfigProcessor(BeanDefinitionRegistry registry, String key, Class<?> type, Object source) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
    beanDefinition.setSource(source);
    registerPostProcessor(registry, beanDefinition, key);
  }

  protected void registerPostProcessor(BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName) {
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(beanName, definition);
  }

  @Override
  protected DefaultListableBeanFactory createBeanFactory() {
    // Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
    DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(getInternalParentBeanFactory());
    beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(),
                                                                             optionalObjectsController));

    return beanFactory;
  }

  /**
   * {@inheritDoc} This implementation returns {@code false} if the context hasn't been initialised yet, in opposition to the
   * default implementation which throws an exception
   */
  @Override
  public boolean isRunning() {
    try {
      return super.isRunning();
    } catch (IllegalStateException e) {
      return false;
    }
  }

  /**
   * Forces the registration of instances of {@link TransformerResolver} and {@link Converter} to be created, so that
   * {@link PostRegistrationActionsPostProcessor} can work its magic and add them to the transformation graph
   */
  protected static void postProcessBeanDefinition(SpringComponentModel resolvedComponent, BeanDefinitionRegistry registry,
                                                  String beanName) {
    if (Converter.class.isAssignableFrom(resolvedComponent.getType())) {
      GenericBeanDefinition converterBeanDefinitionCopy = new GenericBeanDefinition(resolvedComponent.getBeanDefinition());
      converterBeanDefinitionCopy.setScope(SPRING_SINGLETON_OBJECT);
      registry.registerBeanDefinition(beanName + "-" + "converter", converterBeanDefinitionCopy);
    }
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  public OptionalObjectsController getOptionalObjectsController() {
    return optionalObjectsController;
  }

  public void initializeComponent(Location location) {
    MinimalApplicationModelGenerator minimalApplicationModelGenerator =
        new MinimalApplicationModelGenerator(this.applicationModel, componentBuildingDefinitionRegistry);
    ApplicationModel minimalApplicationModel = minimalApplicationModelGenerator.getMinimalModel(location);
    createApplicationComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalApplicationModel, false);
  }

  public ConnectivityTestingService getConnectivityTestingService() {
    return muleContext.getRegistry().lookupObject(OBJECT_CONNECTIVITY_TESTING_SERVICE);
  }

  public MetadataService getMetadataService() {
    return muleContext.getRegistry().get(OBJECT_METADATA_SERVICE);
  }

  public ValueProviderService getValueProviderService() {
    return muleContext.getRegistry().get(OBJECT_VALUE_PROVIDER_SERVICE);
  }

  /**
   * Returns a prototype chain of processors mutating the root container name of the set of beans created from that prototype
   * object.
   * 
   * @param name the bean name
   * @param rootContainerName the new root container name.
   */
  public synchronized void getPrototypeBeanWithRootContainer(String name, String rootContainerName) {
    BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(name);
    checkState(beanDefinition.isPrototype(), format("Bean with name %s is not a prototype", name));
    updateBeanDefinitionRootContainerName(rootContainerName, beanDefinition);
  }

  private void updateBeanDefinitionRootContainerName(String rootContainerName, BeanDefinition beanDefinition) {
    updateAnnotationValue(ROOT_CONTAINER_NAME_KEY, rootContainerName, beanDefinition);
    for (PropertyValue propertyValue : beanDefinition.getPropertyValues().getPropertyValueList()) {
      Object value = propertyValue.getValue();
      processBeanValue(rootContainerName, value);
    }

    for (ConstructorArgumentValues.ValueHolder valueHolder : beanDefinition.getConstructorArgumentValues()
        .getGenericArgumentValues()) {
      processBeanValue(rootContainerName, valueHolder.getValue());
    }
  }

  private void processBeanValue(String rootContainerName, Object value) {
    if (value instanceof BeanDefinition) {
      updateBeanDefinitionRootContainerName(rootContainerName, (BeanDefinition) value);
    } else if (value instanceof ManagedList) {
      ManagedList managedList = (ManagedList) value;
      for (int i = 0; i < managedList.size(); i++) {
        Object itemValue = managedList.get(i);
        if (itemValue instanceof BeanDefinition) {
          updateBeanDefinitionRootContainerName(rootContainerName, (BeanDefinition) itemValue);
        }
      }
    } else if (value instanceof ManagedMap) {
      ManagedMap managedMap = (ManagedMap) value;
      managedMap.forEach((key, mapValue) -> processBeanValue(rootContainerName, mapValue));
    }
  }

}
