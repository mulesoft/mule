/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.config.internal.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.internal.processor.LifecycleStatePostProcessor;
import org.mule.runtime.config.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.config.internal.processor.PostRegistrationActionsPostProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.dsl.api.ConfigResource;

import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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
public class BaseMuleArtifactContext extends AbstractRefreshableConfigApplicationContext {

  private static final Logger LOGGER = getLogger(BaseMuleArtifactContext.class);

  public static final String INNER_BEAN_PREFIX = "(inner bean)";

  // private final OptionalObjectsController optionalObjectsController;
  private final DefaultRegistry serviceDiscoverer;
  // private final DefaultResourceLocator resourceLocator;
  // private final PropertiesResolverConfigurationProperties configurationProperties;
  // private ArtifactAst applicationModel;
  private final MuleContextWithRegistry muleContext;
  // private final BeanDefinitionFactory beanDefinitionFactory;
  private final ArtifactType artifactType;
  // protected SpringConfigurationComponentLocator componentLocator = new SpringConfigurationComponentLocator(componentName -> {
  // try {
  // BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(componentName);
  // return beanDefinition.isPrototype();
  // } catch (NoSuchBeanDefinitionException e) {
  // return false;
  // }
  // });
  // protected List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();
  private org.mule.runtime.core.internal.registry.Registry originalRegistry;
  // private final ExtensionManager extensionManager;

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext                                the {@link MuleContext} that own this context
   * @param artifactAst                                the definition of the artifact to create a context for
   * @param optionalObjectsController                  the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *                                                   org.mule.runtime.config.internal.SpringRegistry
   * @param parentConfigurationProperties              the resolver for properties from the parent artifact to be used as fallback
   *                                                   in this artifact.
   * @param artifactProperties                         map of properties that can be referenced from the
   *                                                   {@code artifactConfigResources} as external configuration values
   * @param artifactType                               the type of artifact to determine the base objects of the created context.
   * @param componentBuildingDefinitionRegistryFactory
   * @since 3.7.0
   */
  public BaseMuleArtifactContext(MuleContext muleContext,
                                 // OptionalObjectsController optionalObjectsController,
                                 // Optional<ConfigurationProperties> parentConfigurationProperties,
                                 // Map<String, String> artifactProperties,
                                 ArtifactType artifactType
  // ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory
  ) {
    // checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = (MuleContextWithRegistry) muleContext;
    // this.optionalObjectsController = optionalObjectsController;
    this.artifactType = artifactType;
    this.serviceDiscoverer = new DefaultRegistry(muleContext);
    // this.resourceLocator = new DefaultResourceLocator();
    originalRegistry = ((MuleRegistryHelper) getMuleRegistry()).getDelegate();

    // extensionManager = muleContext.getExtensionManager();

    // TODO (MULE-19608) remove this and make it into a component building definition
    // this.beanDefinitionFactory =
    // new BeanDefinitionFactory(muleContext.getConfiguration().getId(),
    // componentBuildingDefinitionRegistryFactory.create(getExtensions()));
    //
    // this.applicationModel = artifactAst;
    //
    // this.configurationProperties = createConfigurationAttributeResolver(applicationModel, parentConfigurationProperties,
    // artifactProperties,
    // new ClassLoaderResourceProvider(muleContext
    // .getExecutionClassLoader()),
    // ofNullable(getMuleRegistry()
    // .lookupObject(FEATURE_FLAGGING_SERVICE_KEY)));
    //
    // try {
    // initialiseIfNeeded(configurationProperties.getConfigurationPropertiesResolver());
    // applicationModel.updatePropertiesResolver(configurationProperties.getConfigurationPropertiesResolver());
    //
    // validateArtifact(applicationModel);
    // } catch (ConfigurationException | InitialisationException e) {
    // throw new MuleRuntimeException(e);
    // }
    // registerErrors(applicationModel);
  }

  protected MuleRegistry getMuleRegistry() {
    return this.muleContext.getRegistry();
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    beanFactory.setBeanExpressionResolver(null);

    // TODO base or not base?
    registerEditors(beanFactory);

    registerAnnotationConfigProcessors((BeanDefinitionRegistry) beanFactory, beanFactory);

    addBeanPostProcessors(beanFactory,
                          new MuleContextPostProcessor(muleContext),
                          new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext
                              .getRegistry(), beanFactory),
                          // new DiscardedOptionalBeanPostProcessor(optionalObjectsController,
                          // (DefaultListableBeanFactory) beanFactory),
                          new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState())// ,
    // new ComponentLocatorCreatePostProcessor(componentLocator)
    );

    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);

    // prepareObjectProviders();
  }

  // protected void prepareObjectProviders() {
  // MuleArtifactObjectProvider muleArtifactObjectProvider = new MuleArtifactObjectProvider(this);
  // ImmutableObjectProviderConfiguration providerConfiguration =
  // new ImmutableObjectProviderConfiguration(configurationProperties, muleArtifactObjectProvider);
  // for (ConfigurableObjectProvider objectProvider : objectProviders) {
  // objectProvider.configure(providerConfiguration);
  // }
  // }
  //
  // /**
  // * Process all the {@link ObjectProvider}s from the {@link ApplicationModel} to get their beans and register them inside the
  // * spring bean factory so they can be used for dependency injection.
  // *
  // * @param beanFactory the spring bean factory where the objects will be registered.
  // */
  // protected void registerObjectFromObjectProviders(ConfigurableListableBeanFactory beanFactory) {
  // ((ObjectProviderAwareBeanFactory) beanFactory).setObjectProviders(objectProviders);
  // }
  //
  // private List<Pair<ComponentAst, Optional<String>>> lookObjectProvidersComponentModels(ArtifactAst applicationModel,
  // Map<ComponentAst, SpringComponentModel> springComponentModels) {
  // return applicationModel.topLevelComponentsStream()
  // .filter(componentModel -> {
  // final SpringComponentModel springModel = springComponentModels.get(componentModel);
  // return springModel != null && springModel.getType() != null
  // && ConfigurableObjectProvider.class.isAssignableFrom(springModel.getType());
  // })
  // .map(componentModel -> new Pair<>(componentModel, componentModel.getComponentId()))
  // .collect(toList());
  // }

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
      // disposeIfNeeded(configurationProperties.getConfigurationPropertiesResolver(), LOGGER);
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
    // createApplicationComponents(beanFactory, applicationModel, true);
  }

  @Override
  public void destroy() {
    // try {
    super.destroy();
    // } catch (Exception e) {
    // for (ObjectProvider objectProvider : objectProviders) {
    // disposeIfNeeded(objectProvider, LOGGER);
    // }
    // throw new MuleRuntimeException(e);
    // }
  }

  @Override
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    super.customizeBeanFactory(beanFactory);
    new BaseSpringMuleContextServiceConfigurator(muleContext,
                                                 // configurationProperties,
                                                 artifactType,
                                                 // optionalObjectsController,
                                                 beanFactory,
                                                 // componentLocator,
                                                 serviceDiscoverer,
                                                 originalRegistry// ,
    // resourceLocator
    ).createArtifactServices();

    originalRegistry = null;
  }

  // @Override
  // protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  // Optional<ComponentAst> configurationOptional = findComponentDefinitionModel(applicationModel, CONFIGURATION_IDENTIFIER);
  // if (configurationOptional.isPresent()) {
  // return;
  // }
  // BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
  // beanDefinitionRegistry.registerBeanDefinition(OBJECT_MULE_CONFIGURATION,
  // genericBeanDefinition(MuleConfigurationConfigurator.class).getBeanDefinition());
  // }

  private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
    registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      ConfigurationClassPostProcessor.class, null);
    registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      RequiredAnnotationBeanPostProcessor.class, null);
    registerInjectorProcessor(beanFactory);
  }

  protected void registerInjectorProcessor(ConfigurableListableBeanFactory beanFactory) {
    MuleInjectorProcessor muleInjectorProcessor = new MuleInjectorProcessor();
    muleInjectorProcessor.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(muleInjectorProcessor);
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
    // beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(),
    // optionalObjectsController));

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

  // /**
  // * Forces the registration of instances of {@link TransformerResolver} and {@link Converter} to be created, so that
  // * {@link PostRegistrationActionsPostProcessor} can work its magic and add them to the transformation graph
  // */
  // protected static void postProcessBeanDefinition(SpringComponentModel resolvedComponent, BeanDefinitionRegistry registry,
  // String beanName) {
  // if (Converter.class.isAssignableFrom(resolvedComponent.getType())) {
  // GenericBeanDefinition converterBeanDefinitionCopy = new GenericBeanDefinition(resolvedComponent.getBeanDefinition());
  // converterBeanDefinitionCopy.setScope(SPRING_SINGLETON_OBJECT);
  // registry.registerBeanDefinition(beanName + "-" + "converter", converterBeanDefinitionCopy);
  // }
  // }

  public MuleContextWithRegistry getMuleContext() {
    return muleContext;
  }

  // public OptionalObjectsController getOptionalObjectsController() {
  // return optionalObjectsController;
  // }

  public Registry getRegistry() {
    return getMuleContext().getRegistry().get(OBJECT_REGISTRY);
  }

  @Override
  public String toString() {
    return format("%s: %s (base)", this.getClass().getName(), muleContext.getConfiguration().getId());
  }

}
