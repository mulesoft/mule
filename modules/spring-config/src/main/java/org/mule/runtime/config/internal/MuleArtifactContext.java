/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.BOTTOM_UP;
import static org.mule.runtime.ast.api.util.MuleAstUtils.recursiveStreamWithHierarchy;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.RAISE_ERROR_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.DEFAULT_GLOBAL_ELEMENTS;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.CORE_ERROR_NS;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_SINGLETON_OBJECT;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getArtifactComponentBuildingDefinitions;
import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getExtensionModelsComponentBuildingDefinitions;
import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getRuntimeComponentBuildingDefinitionProvider;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationProcessor.processXmlConfiguration;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.module.extension.internal.runtime.exception.ErrorMappingUtils.forEachErrorMappingDo;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.EnvironmentPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.internal.dsl.xml.XmlNamespaceInfoProviderSupplier;
import org.mule.runtime.config.internal.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.processor.ComponentLocatorCreatePostProcessor;
import org.mule.runtime.config.internal.processor.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.internal.processor.LifecycleStatePostProcessor;
import org.mule.runtime.config.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.config.internal.processor.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.internal.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.util.DefaultResourceLocator;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;
import org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader;
import org.mule.runtime.dsl.api.xml.parser.XmlParsingConfiguration;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.xml.sax.EntityResolver;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context that allows resources to be loaded from the
 * Classpath of file system using the MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractRefreshableConfigApplicationContext implements ArtifactConfigResolverContext {

  private static final Logger LOGGER = getLogger(MuleArtifactContext.class);

  public static final String INNER_BEAN_PREFIX = "(inner bean)";

  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry =
      new ComponentBuildingDefinitionRegistry();
  private final OptionalObjectsController optionalObjectsController;
  private final Map<String, String> artifactProperties;
  private final ArtifactDeclaration artifactDeclaration;
  private final XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader;
  private final Optional<ConfigurationProperties> parentConfigurationProperties;
  private final DefaultRegistry serviceDiscoverer;
  private final DefaultResourceLocator resourceLocator;
  private final ApplicationModel applicationModel;
  private final MuleContextWithRegistry muleContext;
  private final ConfigResource[] artifactConfigResources;
  private final BeanDefinitionFactory beanDefinitionFactory;
  private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
  private final ArtifactType artifactType;
  protected SpringConfigurationComponentLocator componentLocator = new SpringConfigurationComponentLocator(componentName -> {
    try {
      BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(componentName);
      return beanDefinition.isPrototype();
    } catch (NoSuchBeanDefinitionException e) {
      return false;
    }
  });
  protected List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();
  private org.mule.runtime.core.internal.registry.Registry originalRegistry;
  private final ExtensionManager extensionManager;
  private final boolean disableXmlValidations;

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext                                the {@link MuleContext} that own this context
   * @param artifactDeclaration                        the mule configuration defined programmatically
   * @param optionalObjectsController                  the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *                                                   org.mule.runtime.config.internal.SpringRegistry
   * @param pluginsClassLoaders                        the classloades of the plugins included in the artifact, on hwich contexts the parsers will
   *                                                   process.
   * @param parentConfigurationProperties
   * @param disableXmlValidations                      {@code true} when loading XML configs it will not apply validations.
   * @param runtimeComponentBuildingDefinitionProvider provider for the runtime
   *                                                   {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s
   * @since 3.7.0
   */
  public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             List<ClassLoader> pluginsClassLoaders,
                             Optional<ConfigurationProperties> parentConfigurationProperties,
                             boolean disableXmlValidations)
      throws BeansException {
    this(muleContext, artifactConfigResources, artifactDeclaration, optionalObjectsController,
         parentConfigurationProperties, artifactProperties,
         artifactType, pluginsClassLoaders, disableXmlValidations);
  }

  public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Optional<ConfigurationProperties> parentConfigurationProperties,
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             List<ClassLoader> pluginsClassLoaders, boolean disableXmlValidations) {
    checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = (MuleContextWithRegistry) muleContext;
    this.artifactConfigResources = artifactConfigResources;
    this.optionalObjectsController = optionalObjectsController;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.artifactDeclaration = artifactDeclaration;
    this.parentConfigurationProperties = parentConfigurationProperties;
    this.disableXmlValidations = disableXmlValidations;
    this.xmlConfigurationDocumentLoader = disableXmlValidations ? noValidationDocumentLoader() : schemaValidatingDocumentLoader();
    this.serviceDiscoverer = new DefaultRegistry(muleContext);
    this.resourceLocator = new DefaultResourceLocator();
    originalRegistry = ((MuleRegistryHelper) getMuleRegistry()).getDelegate();

    getRuntimeComponentBuildingDefinitionProvider().getComponentBuildingDefinitions()
        .forEach(componentBuildingDefinitionRegistry::register);

    extensionManager = muleContext.getExtensionManager();
    final Set<ExtensionModel> extensions = getExtensions();
    getExtensionModelsComponentBuildingDefinitions(extensions, DslResolvingContext.getDefault(extensions))
        .forEach(componentBuildingDefinitionRegistry::register);

    for (ClassLoader pluginArtifactClassLoader : pluginsClassLoaders) {
      getArtifactComponentBuildingDefinitions(pluginArtifactClassLoader)
          .forEach(componentBuildingDefinitionRegistry::register);
    }

    this.beanDefinitionFactory =
        new BeanDefinitionFactory(muleContext.getConfiguration().getId(), componentBuildingDefinitionRegistry);

    this.applicationModel = createApplicationModel();
  }

  protected MuleRegistry getMuleRegistry() {
    return this.muleContext.getRegistry();
  }

  private void validateAllConfigElementHaveParsers() {
    applicationModel.recursiveStream().forEach(componentModel -> {
      if (!beanDefinitionFactory.hasDefinition(componentModel.getIdentifier())) {
        throw new RuntimeException(format("Invalid config '%s'. No definition parser found for that config",
                                          componentModel.getIdentifier()));
      }
    });
  }

  private ApplicationModel createApplicationModel() {
    try {
      DefaultConfigurationPropertiesResolver propertyResolver =
          new DefaultConfigurationPropertiesResolver(empty(), new ConfigurationPropertiesProvider() {

            ConfigurationPropertiesProvider parentProvider = new EnvironmentPropertiesConfigurationProvider();

            @Override
            public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
              final String propertyValue = artifactProperties.get(configurationAttributeKey);

              if (propertyValue == null) {
                return parentProvider.getConfigurationProperty(configurationAttributeKey);
              }
              return of(new ConfigurationProperty() {

                @Override
                public Object getSource() {
                  return this;
                }

                @Override
                public Object getRawValue() {
                  return propertyValue;
                }

                @Override
                public String getKey() {
                  return configurationAttributeKey;
                }
              });
            }

            @Override
            public String getDescription() {
              return "Deployment properties";
            }
          });

      List<ConfigFile> configFiles = processXmlConfiguration(new XmlParsingConfiguration() {

        @Override
        public ParsingPropertyResolver getParsingPropertyResolver() {
          return propertyKey -> (String) propertyResolver.resolveValue(propertyKey);
        }

        @Override
        public ConfigResource[] getArtifactConfigResources() {
          return MuleArtifactContext.this.getArtifactConfigResources();
        }

        @Override
        public ResourceLocator getResourceLocator() {
          return MuleArtifactContext.this.resourceLocator;
        }

        @Override
        public Supplier<SAXParserFactory> getSaxParserFactory() {
          return () -> XMLSecureFactories.createDefault().getSAXParserFactory();
        }

        @Override
        public XmlConfigurationDocumentLoader getXmlConfigurationDocumentLoader() {
          return xmlConfigurationDocumentLoader;
        }

        @Override
        public EntityResolver getEntityResolver() {
          return new ModuleDelegatingEntityResolver(getExtensions());
        }

        @Override
        public List<XmlNamespaceInfoProvider> getXmlNamespaceInfoProvider() {
          return XmlNamespaceInfoProviderSupplier.createFromExtensionModels(getExtensions(), of(cl -> serviceRegistry
              .lookupProviders(XmlNamespaceInfoProvider.class, cl).stream().collect(toList())));
        }
      });

      ArtifactConfig artifactConfig = new ArtifactConfig.Builder()
          .setApplicationName(getArtifactName())
          .addConfigFiles(configFiles).build();

      ResourceProvider externalResourceProvider = new ClassLoaderResourceProvider(muleContext.getExecutionClassLoader());
      return new ApplicationModel(artifactConfig, artifactDeclaration, getExtensions(),
                                  artifactProperties, parentConfigurationProperties,
                                  externalResourceProvider);
    } catch (MuleRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  public void initialize() {
    applicationModel.prepareAstForRuntime(getExtensions());
    validateAllConfigElementHaveParsers();
  }

  @Override
  protected void prepareRefresh() {
    super.prepareRefresh();
    // As this is the only way we get this context initialized we will register the error types at this point
    registerErrorTypes();
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    beanFactory.setBeanExpressionResolver(null);

    registerEditors(beanFactory);

    registerAnnotationConfigProcessors((BeanDefinitionRegistry) beanFactory, beanFactory);

    addBeanPostProcessors(beanFactory,
                          new MuleContextPostProcessor(muleContext),
                          new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext
                              .getRegistry(), beanFactory),
                          new DiscardedOptionalBeanPostProcessor(optionalObjectsController,
                                                                 (DefaultListableBeanFactory) beanFactory),
                          new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState()),
                          new ComponentLocatorCreatePostProcessor(componentLocator));

    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);

    prepareObjectProviders();
  }

  protected void prepareObjectProviders() {
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
  protected void registerObjectFromObjectProviders(ConfigurableListableBeanFactory beanFactory) {
    ((ObjectProviderAwareBeanFactory) beanFactory).setObjectProviders(objectProviders);
  }

  private List<Pair<ComponentAst, Optional<String>>> lookObjectProvidersComponentModels(ArtifactAst applicationModel,
                                                                                        Map<ComponentAst, SpringComponentModel> springComponentModels) {
    return applicationModel.topLevelComponentsStream()
        .filter(componentModel -> {
          final SpringComponentModel springModel = springComponentModels.get(componentModel);
          return springModel != null && springModel.getType() != null
              && ConfigurableObjectProvider.class.isAssignableFrom(springModel.getType());
        })
        .map(componentModel -> new Pair<>(componentModel, componentModel.getComponentId()))
        .collect(toList());
  }

  private void registerEditors(ConfigurableListableBeanFactory beanFactory) {
    MulePropertyEditorRegistrar registrar = new MulePropertyEditorRegistrar();
    registrar.setMuleContext(muleContext);
    beanFactory.addPropertyEditorRegistrar(registrar);
  }

  protected void addBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, BeanPostProcessor... processors) {
    for (BeanPostProcessor processor : processors) {
      beanFactory.addBeanPostProcessor(processor);
    }
  }

  @Override
  public void close() {
    if (isRunning()) {
      super.close();
      applicationModel.close();
    }
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
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    createApplicationComponents(beanFactory, applicationModel, true);
  }

  private void registerErrorTypes() {
    Set<String> syntheticErrorNamespaces = new HashSet<>();

    applicationModel.recursiveStream()
        .forEach(componentModel -> resolveErrorTypes(componentModel, syntheticErrorNamespaces));
  }

  private void resolveErrorTypes(ComponentAst componentModel, Set<String> syntheticErrorNamespaces) {
    componentModel.directChildrenStream()
        .forEach(innerComponent -> {
          processRaiseError(innerComponent, syntheticErrorNamespaces);
          resolveErrorTypes(innerComponent, syntheticErrorNamespaces);
        });

    registerErrorMappings(componentModel, syntheticErrorNamespaces);
  }

  private void registerErrorMappings(ComponentAst componentModel, Set<String> syntheticErrorNamespaces) {
    forEachErrorMappingDo(componentModel, mappings -> mappings
        .forEach(mapping -> {
          ComponentIdentifier source = buildFromStringRepresentation(mapping.getSource());

          if (!muleContext.getErrorTypeRepository().lookupErrorType(source).isPresent()) {
            throw new MuleRuntimeException(createStaticMessage("Could not find error '%s'.", source));
          }

          resolveErrorType(mapping.getTarget(), syntheticErrorNamespaces, !disableXmlValidations);
        }));
  }

  private void processRaiseError(ComponentAst componentModel, Set<String> syntheticErrorNamespaces) {
    if (componentModel.getIdentifier().equals(RAISE_ERROR_IDENTIFIER)) {
      String representation = componentModel.getRawParameterValue("type").orElse(null);
      if (isEmpty(representation) && disableXmlValidations) {
        // We can just ignore this as we should allow an empty value here
        return;
      }
      resolveErrorType(representation, syntheticErrorNamespaces, !disableXmlValidations);
    }
  }

  private ErrorType resolveErrorType(String representation, Set<String> syntheticErrorNamespaces, boolean checkErrorTypes) {
    ComponentIdentifier errorIdentifier = parserErrorType(representation);
    String namespace = errorIdentifier.getNamespace();
    String identifier = errorIdentifier.getName();
    ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
    Optional<ErrorType> optionalErrorType = errorTypeRepository.lookupErrorType(errorIdentifier);
    if (CORE_ERROR_NS.equals(namespace)) {
      if (checkErrorTypes) {
        return optionalErrorType
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("There's no MULE error named '%s'.",
                                                                                   identifier))));
      }
      return optionalErrorType.orElse(null);

    } else if (errorTypeRepository.getErrorNamespaces().contains(namespace) && !syntheticErrorNamespaces.contains(namespace)) {
      throw new MuleRuntimeException(createStaticMessage(format("Cannot use error type '%s:%s': namespace already exists.",
                                                                namespace, identifier)));
    } else if (syntheticErrorNamespaces.contains(namespace)) {
      if (optionalErrorType.isPresent()) {
        return optionalErrorType.get();
      }
    } else {
      syntheticErrorNamespaces.add(namespace);
    }
    if (logger.isDebugEnabled()) {
      logger.debug(format("Registering errorType '%s'", errorIdentifier));
    }
    return errorTypeRepository.addErrorType(errorIdentifier, errorTypeRepository.getAnyErrorType());
  }

  private ComponentIdentifier parserErrorType(String representation) {
    int separator = representation.indexOf(':');
    String namespace;
    String identifier;
    if (separator > 0) {
      namespace = representation.substring(0, separator).toUpperCase();
      identifier = representation.substring(separator + 1).toUpperCase();
    } else {
      namespace = CORE_ERROR_NS;
      identifier = representation.toUpperCase();
    }

    return ComponentIdentifier.builder().namespace(namespace).name(identifier).build();
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

  /**
   * Creates the definition for all the objects to be created form the enabled components in the {@code applicationModel}.
   *
   * @param beanFactory      the bean factory in which definition must be created.
   * @param applicationModel the artifact application model.
   * @param mustBeRoot       if the component must be root to be created.
   * @return an order list of the created bean names. The order must be respected for the creation of the objects.
   */
  protected List<Pair<String, ComponentAst>> createApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                                         ArtifactAst applicationModel,
                                                                         boolean mustBeRoot) {
    Map<ComponentAst, SpringComponentModel> springComponentModels = new LinkedHashMap<>();

    return doCreateApplicationComponents(beanFactory, applicationModel, mustBeRoot, springComponentModels);
  }

  protected List<Pair<String, ComponentAst>> doCreateApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                                           ArtifactAst applicationModel, boolean mustBeRoot,
                                                                           Map<ComponentAst, SpringComponentModel> springComponentModels) {
    Set<String> alwaysEnabledTopLevelComponents = new HashSet<>();
    Set<ComponentIdentifier> alwaysEnabledUnnamedTopLevelComponents = applicationModel.topLevelComponentsStream()
        .filter(cm -> cm.getModel(HasStereotypeModel.class)
            .map(stm -> stm.getStereotype().isAssignableTo(APP_CONFIG))
            .orElse(false))
        .peek(cm -> cm.getComponentId().ifPresent(alwaysEnabledTopLevelComponents::add))
        .filter(cm -> !cm.getComponentId().isPresent())
        .map(ComponentAst::getIdentifier)
        .collect(toSet());
    Set<String> alwaysEnabledGeneratedTopLevelComponentsName = new HashSet<>();

    List<Pair<String, ComponentAst>> createdComponentModels = new ArrayList<>();

    final Set<ComponentAst> rootComponents = resolveRootComponents(applicationModel);

    recursiveStreamWithHierarchy(applicationModel, BOTTOM_UP)
        // Create component if must not be root is mandatory or component is a root component or component is child of a root component
        .filter(cm -> !mustBeRoot || rootComponents.contains(cm.getFirst())
            || cm.getSecond().stream().anyMatch(rootComponents::contains))
        .filter(cm -> !isIgnored(cm.getFirst()))
        .forEach(cm -> {
          if (rootComponents.contains(cm.getFirst())) {
            cm.getFirst().getComponentId()
                .ifPresent(componentName -> createdComponentModels.add(new Pair<>(componentName, cm.getFirst())));
          }

          beanDefinitionFactory.resolveComponent(springComponentModels,
                                                 cm.getSecond().isEmpty() ? null : cm.getSecond().get(cm.getSecond().size() - 1),
                                                 cm.getFirst(), beanFactory, componentLocator);

          if (rootComponents.contains(cm.getFirst())) {
            componentLocator.addComponentLocation(cm.getFirst().getLocation());
          }
        });


    springComponentModels.values().stream()
        .filter(resolvedComponentModel -> rootComponents.contains(resolvedComponentModel.getComponent()))
        .forEach(resolvedComponentModel -> registerRootSpringBean(beanFactory, alwaysEnabledUnnamedTopLevelComponents,
                                                                  alwaysEnabledGeneratedTopLevelComponentsName,
                                                                  createdComponentModels, resolvedComponentModel));

    // This should only be done once at the initial application model creation, called from Spring
    List<Pair<ComponentAst, Optional<String>>> objectProvidersByName =
        lookObjectProvidersComponentModels(applicationModel, springComponentModels);

    objectProvidersByName.stream()
        .map(pair -> springComponentModels.get(pair.getFirst()).getObjectInstance())
        .forEach(this.objectProviders::add);
    registerObjectFromObjectProviders(beanFactory);

    Set<String> objectProviderNames = objectProvidersByName.stream()
        .map(Pair::getSecond)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toSet());

    // Put object providers first, then always enabled components, then the rest
    createdComponentModels.sort(comparing(beanNameAndComponent -> {
      final String beanName = beanNameAndComponent.getFirst();
      if (objectProviderNames.contains(beanName)) {
        return 1;
      } else if (alwaysEnabledGeneratedTopLevelComponentsName.contains(beanName)) {
        return 2;
      } else if (alwaysEnabledTopLevelComponents.contains(beanName)) {
        return 3;
      } else {
        return 4;
      }
    }));

    return createdComponentModels;
  }

  protected boolean isIgnored(ComponentAst componentAst) {
    return beanDefinitionFactory.isComponentIgnored(componentAst.getIdentifier());
  }

  private Set<ComponentAst> resolveRootComponents(ArtifactAst applicationModel) {
    final Set<ConfigurationModel> xmlSdk1ConfigModels = newSetFromMap(new IdentityHashMap<>());
    extensionManager.getExtensions()
        .stream()
        .flatMap(extension -> extension.getModelProperty(XmlExtensionModelProperty.class)
            .map(mp -> extension.getConfigurationModels().stream())
            .orElse(Stream.empty()))
        .forEach(xmlSdk1ConfigModels::add);

    // Handle specific case for nested configs/topLevelElements generated by XmlSdk1 macroexpansion
    return concat(applicationModel.topLevelComponentsStream(),
                  applicationModel.topLevelComponentsStream()
                      .flatMap(root -> root.recursiveStream()
                          .filter(comp -> comp.getModel(ConfigurationModel.class)
                              .map(xmlSdk1ConfigModels::contains)
                              .orElse(comp.getIdentifier().getName().equals(DEFAULT_GLOBAL_ELEMENTS)))
                          .flatMap(ComponentAst::directChildrenStream)))
                              .filter(comp -> !comp.getIdentifier().getName().equals(DEFAULT_GLOBAL_ELEMENTS))
                              .collect(toSet());
  }

  private void registerRootSpringBean(DefaultListableBeanFactory beanFactory,
                                      Set<ComponentIdentifier> alwaysEnabledUnnamedTopLevelComponents,
                                      Set<String> alwaysEnabledGeneratedTopLevelComponentsName,
                                      List<Pair<String, ComponentAst>> createdComponentModels,
                                      SpringComponentModel resolvedComponentModel) {
    String nameAttribute = resolvedComponentModel.getComponent().getComponentId().orElse(null);
    if (resolvedComponentModel.getComponent().getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
      nameAttribute = OBJECT_MULE_CONFIGURATION;
    } else if (nameAttribute == null) {
      // This may be a configuration that does not requires a name.
      nameAttribute = uniqueValue(resolvedComponentModel.getBeanDefinition().getBeanClassName());

      if (alwaysEnabledUnnamedTopLevelComponents.contains(resolvedComponentModel.getComponent().getIdentifier())) {
        alwaysEnabledGeneratedTopLevelComponentsName.add(nameAttribute);
        createdComponentModels.add(new Pair<>(nameAttribute, resolvedComponentModel.getComponent()));
      } else if (resolvedComponentModel.getType() != null
          && TransactionManagerFactory.class.isAssignableFrom(resolvedComponentModel.getType())) {
        createdComponentModels.add(new Pair<>(nameAttribute, resolvedComponentModel.getComponent()));
      }
    }

    beanFactory.registerBeanDefinition(nameAttribute,
                                       requireNonNull(resolvedComponentModel.getBeanDefinition(),
                                                      "BeanDefinition null for "
                                                          + resolvedComponentModel.getComponent().toString()));
    postProcessBeanDefinition(resolvedComponentModel, beanFactory, nameAttribute);
  }

  @Override
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    super.customizeBeanFactory(beanFactory);
    new SpringMuleContextServiceConfigurator(muleContext,
                                             applicationModel.getConfigurationProperties(),
                                             artifactType,
                                             optionalObjectsController,
                                             beanFactory,
                                             componentLocator,
                                             serviceDiscoverer,
                                             originalRegistry,
                                             resourceLocator).createArtifactServices();

    originalRegistry = null;
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    Optional<ComponentAst> configurationOptional =
        applicationModel.findComponentDefinitionModel(CONFIGURATION_IDENTIFIER);
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
    if (artifactType.equals(APP) || artifactType.equals(POLICY) || artifactType.equals(DOMAIN)) {
      muleInjectorProcessor = new MuleInjectorProcessor();
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

  public MuleContextWithRegistry getMuleContext() {
    return muleContext;
  }

  public OptionalObjectsController getOptionalObjectsController() {
    return optionalObjectsController;
  }

  public Registry getRegistry() {
    return getMuleContext().getRegistry().get(OBJECT_REGISTRY);
  }

  @Override
  public String toString() {
    return format("%s: %s (%s)", this.getClass().getName(), muleContext.getConfiguration().getId(), artifactType.name());
  }

  // *****************************
  // ArtifactConfigResolverContext
  // *****************************


  @Override
  public String getArtifactName() {
    return muleContext.getConfiguration().getId();
  }

  @Override
  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }

  @Override
  public ConfigResource[] getArtifactConfigResources() {
    return artifactConfigResources;
  }

  @Override
  public Set<ExtensionModel> getExtensions() {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  @Override
  public ClassLoader getExecutionClassLoader() {
    return muleContext.getExecutionClassLoader();
  }

  public ApplicationModel getApplicationModel() {
    return applicationModel;
  }

}
