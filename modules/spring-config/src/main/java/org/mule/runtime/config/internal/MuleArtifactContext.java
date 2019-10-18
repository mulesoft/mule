/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.api.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.runtime.config.api.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_SINGLETON_OBJECT;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.updateAnnotationValue;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils.getExtensionModelsComponentBuildingDefinitions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.config.api.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.api.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.internal.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.processor.ComponentLocatorCreatePostProcessor;
import org.mule.runtime.config.internal.processor.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.internal.processor.LifecycleStatePostProcessor;
import org.mule.runtime.config.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.config.internal.processor.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.internal.util.ComponentBuildingDefinitionUtils;
import org.mule.runtime.config.internal.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigResource;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context that allows resources to be loaded from the
 * Classpath of file system using the MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractRefreshableConfigApplicationContext implements ArtifactConfigResolverContext {

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
  private final ConfigurationDependencyResolver configurationDependencyResolver;
  protected ApplicationModel applicationModel;
  protected MuleContextWithRegistries muleContext;
  private ConfigResource[] artifactConfigResources;
  protected BeanDefinitionFactory beanDefinitionFactory;
  private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
  protected final XmlApplicationParser xmlApplicationParser;
  private ArtifactType artifactType;
  private List<ComponentIdentifier> componentNotSupportedByNewParsers = new ArrayList<>();
  protected SpringConfigurationComponentLocator componentLocator = new SpringConfigurationComponentLocator(componentName -> {
    try {
      BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(componentName);
      return beanDefinition.isPrototype();
    } catch (NoSuchBeanDefinitionException e) {
      return false;
    }
  });
  protected List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext the {@link MuleContext} that own this context
   * @param artifactDeclaration the mule configuration defined programmatically
   * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *        org.mule.runtime.config.internal.SpringRegistry
   * @param pluginsClassLoaders the classloades of the plugins included in the artifact, on hwich contexts the parsers will
   *        process.
   * @param parentConfigurationProperties
   * @param disableXmlValidations {@code true} when loading XML configs it will not apply validations.
   * @param runtimeComponentBuildingDefinitionProvider provider for the runtime
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s
   * @since 3.7.0
   */
  public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             List<ClassLoader> pluginsClassLoaders,
                             Optional<ConfigurationProperties> parentConfigurationProperties,
                             boolean disableXmlValidations,
                             ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider)
      throws BeansException {
    this(muleContext, artifactConfigResources, artifactDeclaration, optionalObjectsController,
         parentConfigurationProperties, artifactProperties,
         artifactType, pluginsClassLoaders, disableXmlValidations,
         runtimeComponentBuildingDefinitionProvider);
  }

  public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Optional<ConfigurationProperties> parentConfigurationProperties,
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             List<ClassLoader> pluginsClassLoaders, boolean disableXmlValidations,
                             ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider) {
    checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = (MuleContextWithRegistries) muleContext;
    this.artifactConfigResources = artifactConfigResources;
    this.optionalObjectsController = optionalObjectsController;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.artifactDeclaration = artifactDeclaration;
    this.parentConfigurationProperties = parentConfigurationProperties;
    this.xmlConfigurationDocumentLoader = disableXmlValidations ? noValidationDocumentLoader() : schemaValidatingDocumentLoader();
    this.serviceDiscoverer = new DefaultRegistry(muleContext);

    runtimeComponentBuildingDefinitionProvider.getComponentBuildingDefinitions()
        .forEach(componentBuildingDefinitionRegistry::register);

    getExtensionModelsComponentBuildingDefinitions(serviceRegistry,
                                                   getExtensionModels(muleContext.getExtensionManager()).orElse(emptySet()))
                                                       .forEach(componentBuildingDefinitionRegistry::register);

    for (ClassLoader pluginArtifactClassLoader : pluginsClassLoaders) {
      ComponentBuildingDefinitionUtils.getArtifactComponentBuildingDefinitions(serviceRegistry, pluginArtifactClassLoader)
          .forEach(componentBuildingDefinitionRegistry::register);
    }

    xmlApplicationParser = createApplicationParser();
    this.beanDefinitionFactory =
        new BeanDefinitionFactory(componentBuildingDefinitionRegistry, muleContext.getErrorTypeRepository());

    createApplicationModel();
    validateAllConfigElementHaveParsers();

    this.configurationDependencyResolver =
        new ConfigurationDependencyResolver(applicationModel, componentBuildingDefinitionRegistry,
                                            componentIdentifier -> beanDefinitionFactory
                                                .isLanguageConstructComponent(componentIdentifier));
  }



  private static Optional<Set<ExtensionModel>> getExtensionModels(ExtensionManager extensionManager) {
    return ofNullable(extensionManager == null ? null
        : extensionManager.getExtensions());
  }

  private XmlApplicationParser createApplicationParser() {
    ExtensionManager extensionManager = muleContext.getExtensionManager();
    return XmlApplicationParser.createFromExtensionModels(extensionManager.getExtensions());
  }

  private void validateAllConfigElementHaveParsers() {
    applicationModel.executeOnEveryComponentTree(componentModel -> {
      Optional<ComponentIdentifier> parentIdentifierOptional = ofNullable(componentModel.getParent())
          .flatMap(parentComponentModel -> ofNullable(parentComponentModel.getIdentifier()));
      if (!beanDefinitionFactory.hasDefinition(componentModel.getIdentifier(), parentIdentifierOptional)) {
        componentNotSupportedByNewParsers.add(componentModel.getIdentifier());
        throw new RuntimeException(format("Invalid config '%s'. No definition parser found for that config",
                                          componentModel.getIdentifier()));
      }
    });
  }

  private void createApplicationModel() {
    try {
      ArtifactConfig artifactConfig = new DefaultArtifactConfigResolver().resolveArtifactConfig(this);
      Set<ExtensionModel> extensions =
          muleContext.getExtensionManager() != null ? muleContext.getExtensionManager().getExtensions() : emptySet();
      ResourceProvider externalResourceProvider = new ClassLoaderResourceProvider(muleContext.getExecutionClassLoader());
      applicationModel = new ApplicationModel(artifactConfig, artifactDeclaration, extensions,
                                              artifactProperties, parentConfigurationProperties,
                                              of(componentBuildingDefinitionRegistry),
                                              true, externalResourceProvider);
    } catch (MuleRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);

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

  private List<Pair<ComponentModel, Optional<String>>> lookObjectProvidersComponentModels(ApplicationModel applicationModel) {
    List<Pair<ComponentModel, Optional<String>>> objectProviders = new ArrayList<>();
    applicationModel.executeOnEveryRootElement(componentModel -> {
      if (componentModel.isEnabled() && componentModel.getType() != null
          && ConfigurableObjectProvider.class.isAssignableFrom(componentModel.getType())) {
        objectProviders.add(new Pair<>(componentModel, ofNullable(componentModel.getNameAttribute())));
      }
    });
    return objectProviders;
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
   * Creates te definition for all the objects to be created form the enabled components in the {@code applicationModel}.
   *
   * @param beanFactory the bean factory in which definition must be created.
   * @param applicationModel the artifact application model.
   * @param mustBeRoot if the component must be root to be created.
   * @return an order list of the created bean names. The order must be respected for the creation of the objects.
   */
  protected List<String> createApplicationComponents(DefaultListableBeanFactory beanFactory, ApplicationModel applicationModel,
                                                     boolean mustBeRoot) {

    // This should only be done once at the initial application model creation, called from Spring
    List<Pair<ComponentModel, Optional<String>>> objectProvidersByName =
        lookObjectProvidersComponentModels(applicationModel);

    Set<String> alwaysEnabledTopLevelComponents = new HashSet<>();
    Set<ComponentIdentifier> alwaysEnabledUnnamedTopLevelComponents = new HashSet<>();
    Set<String> alwaysEnabledGeneratedTopLevelComponentsName = new HashSet<>();

    configurationDependencyResolver.resolveAlwaysEnabledComponents()
        .forEach(dependencyNode -> {
          if (dependencyNode.isTopLevel()) {
            alwaysEnabledTopLevelComponents.add(dependencyNode.getComponentName());
          } else if (dependencyNode.isUnnamedTopLevel() && dependencyNode.getComponentIdentifier().isPresent()) {
            alwaysEnabledUnnamedTopLevelComponents.add(dependencyNode.getComponentIdentifier().get());
          }
        });

    List<String> createdComponentModels = new ArrayList<>();
    applicationModel.executeOnEveryMuleComponentTree(cm -> {
      SpringComponentModel componentModel = (SpringComponentModel) cm;
      if (!mustBeRoot || componentModel.isRoot()) {
        if (beanDefinitionFactory.isComponentIgnored(componentModel.getIdentifier())) {
          return;
        }

        SpringComponentModel parentComponentModel = componentModel.getParent() != null
            ? (SpringComponentModel) componentModel.getParent()
            : (SpringComponentModel) applicationModel.getRootComponentModel();

        if (componentModel.isEnabled() || alwaysEnabledUnnamedTopLevelComponents.contains(componentModel.getIdentifier())) {
          if (componentModel.getNameAttribute() != null && componentModel.isRoot()) {
            createdComponentModels.add(componentModel.getNameAttribute());
          }
          beanDefinitionFactory
              .resolveComponentRecursively(parentComponentModel, componentModel, beanFactory,
                                           (resolvedComponentModel, registry) -> {
                                             SpringComponentModel resolvedSpringComponentModel =
                                                 (SpringComponentModel) resolvedComponentModel;
                                             if (resolvedComponentModel.isRoot()) {
                                               String nameAttribute = resolvedComponentModel.getNameAttribute();
                                               if (resolvedComponentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
                                                 nameAttribute = OBJECT_MULE_CONFIGURATION;
                                               } else if (nameAttribute == null) {
                                                 // This may be a configuration that does not requires a name.
                                                 nameAttribute = uniqueValue(resolvedSpringComponentModel.getBeanDefinition()
                                                     .getBeanClassName());

                                                 if (alwaysEnabledUnnamedTopLevelComponents
                                                     .contains(resolvedSpringComponentModel.getIdentifier())) {
                                                   alwaysEnabledGeneratedTopLevelComponentsName.add(nameAttribute);
                                                   createdComponentModels.add(nameAttribute);
                                                 }
                                               }
                                               registry.registerBeanDefinition(nameAttribute,
                                                                               resolvedSpringComponentModel.getBeanDefinition());
                                               postProcessBeanDefinition(componentModel, registry, nameAttribute);
                                             }
                                           }, null, componentLocator);

        } else {
          beanDefinitionFactory.resolveComponentRecursively(parentComponentModel, componentModel, beanFactory, null, null,
                                                            componentLocator);
        }
        componentLocator.addComponentLocation(cm.getComponentLocation());
      }
    });

    this.objectProviders
        .addAll(objectProvidersByName.stream().map(pair -> (ConfigurableObjectProvider) pair.getFirst().getObjectInstance())
            .collect(toList()));
    registerObjectFromObjectProviders(beanFactory);

    Set<String> objectProviderNames = objectProvidersByName.stream().map(Pair::getSecond).filter(Optional::isPresent)
        .map(Optional::get).collect(toSet());

    // Put object providers first, then always enabled components, then the rest
    createdComponentModels.sort(Comparator.comparing(beanName -> {
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

  /**
   * @return a resolver for dependencies between configuration objects
   */
  public ConfigurationDependencyResolver getDependencyResolver() {
    return configurationDependencyResolver;
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

  public MuleContextWithRegistries getMuleContext() {
    return muleContext;
  }

  public OptionalObjectsController getOptionalObjectsController() {
    return optionalObjectsController;
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
    Class<?> beanClass = null;
    try {
      beanClass = currentThread().getContextClassLoader().loadClass(beanDefinition.getBeanClassName());
    } catch (ClassNotFoundException e) {
      // Nothing to do, spring will break because of this eventually
    }

    if (beanClass == null || Component.class.isAssignableFrom(beanClass)) {
      updateAnnotationValue(ROOT_CONTAINER_NAME_KEY, rootContainerName, beanDefinition);
    }

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
  public XmlConfigurationDocumentLoader getXmlConfigurationDocumentLoader() {
    return xmlConfigurationDocumentLoader;
  }

  @Override
  public Set<ExtensionModel> getExtensions() {
    return muleContext.getExtensionManager() == null ? emptySet() : muleContext.getExtensionManager().getExtensions();
  }

  @Override
  public XmlApplicationParser getXmlApplicationParser() {
    return xmlApplicationParser;
  }

  @Override
  public ClassLoader getExecutionClassLoader() {
    return muleContext.getExecutionClassLoader();
  }

}
