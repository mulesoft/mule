/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory.SPRING_SINGLETON_OBJECT;
import static org.mule.runtime.config.spring.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.config.spring.dsl.api.xml.StaticXmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.StaticXmlNamespaceInfoProvider;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.model.MinimalApplicationModelGenerator;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.spring.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.spring.processors.ComponentLocatorCreatePostProcessor;
import org.mule.runtime.config.spring.processors.ContextExclusiveInjectorProcessor;
import org.mule.runtime.config.spring.processors.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.spring.processors.LifecycleStatePostProcessor;
import org.mule.runtime.config.spring.processors.MuleInjectorProcessor;
import org.mule.runtime.config.spring.processors.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.spring.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.AbstractServiceRegistry;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.TransformerResolver;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
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

  private static final ThreadLocal<MuleContext> currentMuleContext = new ThreadLocal<>();
  public static final String INNER_BEAN_PREFIX = "(inner bean)";

  protected final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry =
      new ComponentBuildingDefinitionRegistry();
  private final OptionalObjectsController optionalObjectsController;
  private final Map<String, String> artifactProperties;
  private final ArtifactDeclaration artifactDeclaration;
  private final XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader;
  protected ApplicationModel applicationModel;
  protected MuleContext muleContext;
  private Resource[] artifactConfigResources;
  protected BeanDefinitionFactory beanDefinitionFactory;
  private MuleXmlBeanDefinitionReader beanDefinitionReader;
  private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
  protected boolean useNewParsingMechanism = true;
  protected final XmlApplicationParser xmlApplicationParser;
  private ArtifactType artifactType;
  private List<ComponentIdentifier> componentNotSupportedByNewParsers = new ArrayList<>();
  private SpringConfigurationComponentLocator componentLocator = new SpringConfigurationComponentLocator();

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext the {@link MuleContext} that own this context
   * @param artifactDeclaration the mule configuration defined programmatically
   * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *        org.mule.runtime.config.spring.SpringRegistry
   * @since 3.7.0
   */
  public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Map<String, String> artifactProperties, ArtifactType artifactType)
      throws BeansException {
    this(muleContext, convert(artifactConfigResources), artifactDeclaration, optionalObjectsController, artifactProperties,
         artifactType);
  }

  public MuleArtifactContext(MuleContext muleContext, Resource[] artifactConfigResources,
                             ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                             Map<String, String> artifactProperties, ArtifactType artifactType) {
    checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = muleContext;
    this.artifactConfigResources = artifactConfigResources;
    this.optionalObjectsController = optionalObjectsController;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.artifactDeclaration = artifactDeclaration;
    this.xmlConfigurationDocumentLoader = newXmlConfigurationDocumentLoader();

    serviceRegistry.lookupProviders(ComponentBuildingDefinitionProvider.class, currentThread().getContextClassLoader())
        .forEach(componentBuildingDefinitionProvider -> {
          // TODO MULE-9637 remove support for MuleContextAware injection.
          if (componentBuildingDefinitionProvider instanceof MuleContextAware) {
            ((MuleContextAware) componentBuildingDefinitionProvider).setMuleContext(muleContext);
          }
          componentBuildingDefinitionProvider.init();
          componentBuildingDefinitionProvider.getComponentBuildingDefinitions()
              .forEach(componentBuildingDefinitionRegistry::register);
        });

    xmlApplicationParser = new XmlApplicationParser(new XmlServiceRegistry(serviceRegistry, muleContext));
    this.beanDefinitionFactory =
        new BeanDefinitionFactory(componentBuildingDefinitionRegistry, muleContext.getErrorTypeRepository());

    createApplicationModel();
    determineIfOnlyNewParsingMechanismCanBeUsed();
  }

  protected XmlConfigurationDocumentLoader newXmlConfigurationDocumentLoader() {
    return new XmlConfigurationDocumentLoader();
  }

  private void determineIfOnlyNewParsingMechanismCanBeUsed() {
    if (applicationModel.hasSpringConfig()) {
      useNewParsingMechanism = false;
      return;
    }
    applicationModel.executeOnEveryComponentTree(componentModel -> {
      Optional<ComponentIdentifier> parentIdentifierOptional = ofNullable(componentModel.getParent())
          .flatMap(parentComponentModel -> ofNullable(parentComponentModel.getIdentifier()));
      if (!beanDefinitionFactory.hasDefinition(componentModel.getIdentifier(), parentIdentifierOptional)) {
        componentNotSupportedByNewParsers.add(componentModel.getIdentifier());
        useNewParsingMechanism = false;
      }
    });
  }

  private void createApplicationModel() {
    try {
      ArtifactConfig.Builder applicationConfigBuilder = new ArtifactConfig.Builder();
      applicationConfigBuilder.setApplicationProperties(this.artifactProperties);
      for (Resource springResource : artifactConfigResources) {
        Document document =
            xmlConfigurationDocumentLoader.loadDocument(ofNullable(muleContext.getExtensionManager()),
                                                        springResource.getFilename(),
                                                        springResource.getInputStream());
        ConfigLine mainConfigLine = xmlApplicationParser.parse(document.getDocumentElement()).get();
        applicationConfigBuilder.addConfigFile(new ConfigFile(getFilename(springResource), asList(mainConfigLine)));
      }
      applicationConfigBuilder.setApplicationName(muleContext.getConfiguration().getId());
      applicationModel = new ApplicationModel(applicationConfigBuilder.build(), artifactDeclaration,
                                              ofNullable(muleContext.getExtensionManager()),
                                              of(componentBuildingDefinitionRegistry));
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
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

    addBeanPostProcessors(beanFactory,
                          new MuleContextPostProcessor(muleContext),
                          new GlobalNamePostProcessor(),
                          new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext.getRegistry(), beanFactory),
                          new DiscardedOptionalBeanPostProcessor(optionalObjectsController,
                                                                 (DefaultListableBeanFactory) beanFactory),
                          new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState()),
                          new ComponentLocatorCreatePostProcessor(componentLocator));

    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);
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

  private static Resource[] convert(ConfigResource[] resources) {
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
    BeanDefinitionReader beanDefinitionReader = createBeanDefinitionReader(beanFactory);
    // Communicate mule context to parsers
    try {
      currentMuleContext.set(muleContext);
      createInitialApplicationComponents(beanFactory, beanDefinitionReader);
    } finally {
      currentMuleContext.remove();
    }
  }

  protected void createInitialApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                    BeanDefinitionReader beanDefinitionReader) {
    if (useNewParsingMechanism) {
      createApplicationComponents(beanFactory, applicationModel, true);
    } else {
      // TODO MULE-9638 - Remove log line
      logger
          .info("Using mixed mechanism to load configuration since there are some components that were not yet migrated to the new mechanism: "
              + getOldParsingMechanismComponentIdentifiers());
      beanDefinitionReader.loadBeanDefinitions(getConfigResources());
    }
  }

  protected List<String> createApplicationComponents(DefaultListableBeanFactory beanFactory, ApplicationModel applicationModel,
                                                     boolean mustBeRoot) {
    List<String> createdComponentModels = new ArrayList<>();
    applicationModel.executeOnEveryMuleComponentTree(componentModel -> {
      if (!mustBeRoot || componentModel.isRoot()) {
        if (componentModel.getIdentifier().equals(MULE_IDENTIFIER) || !componentModel.isEnabled()) {
          return;
        }
        if (componentModel.getNameAttribute() != null) {
          createdComponentModels.add(componentModel.getNameAttribute());
        }
        beanDefinitionFactory.resolveComponentRecursively(componentModel.getParent() != null ? componentModel.getParent()
            : applicationModel.getRootComponentModel(), componentModel,
                                                          beanFactory,
                                                          (resolvedComponentModel, registry) -> {
                                                            if (resolvedComponentModel.isRoot()) {
                                                              String nameAttribute =
                                                                  resolvedComponentModel.getNameAttribute();
                                                              if (resolvedComponentModel.getIdentifier()
                                                                  .equals(CONFIGURATION_IDENTIFIER)) {
                                                                nameAttribute = OBJECT_MULE_CONFIGURATION;
                                                              } else if (nameAttribute == null) {
                                                                // This may be a configuration that does not requires a name.
                                                                nameAttribute = uniqueValue(resolvedComponentModel
                                                                    .getBeanDefinition().getBeanClassName());
                                                              }
                                                              registry.registerBeanDefinition(nameAttribute,
                                                                                              resolvedComponentModel
                                                                                                  .getBeanDefinition());
                                                              postProcessBeanDefinition(componentModel, registry,
                                                                                        nameAttribute);
                                                            }
                                                          }, null);
      }
    });
    return createdComponentModels;
  }

  protected String getOldParsingMechanismComponentIdentifiers() {
    return join(componentNotSupportedByNewParsers.toArray(), ",");
  }

  @Override
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    super.customizeBeanFactory(beanFactory);
    new SpringMuleContextServiceConfigurator(muleContext, artifactType, optionalObjectsController, beanFactory, componentLocator)
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

  protected BeanDefinitionReader createBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
    beanDefinitionReader =
        new MuleXmlBeanDefinitionReader(beanFactory, createBeanDefinitionDocumentReader(beanDefinitionFactory));
    // annotate parsed elements with metadata
    beanDefinitionReader.setDocumentLoader(createLoader());
    // hook in our custom hierarchical reader
    beanDefinitionReader.setDocumentReaderClass(getBeanDefinitionDocumentReaderClass());
    // add error reporting
    beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
    registerAnnotationConfigProcessors(beanDefinitionReader.getRegistry(), null);

    return beanDefinitionReader;
  }

  protected MuleBeanDefinitionDocumentReader createBeanDefinitionDocumentReader(BeanDefinitionFactory beanDefinitionFactory) {
    if (artifactType.equals(ArtifactType.DOMAIN)) {
      return new MuleDomainBeanDefinitionDocumentReader(beanDefinitionFactory, xmlApplicationParser,
                                                        componentBuildingDefinitionRegistry);
    }
    return new MuleBeanDefinitionDocumentReader(beanDefinitionFactory, xmlApplicationParser, componentBuildingDefinitionRegistry);
  }

  protected MuleDocumentLoader createLoader() {
    return new MuleDocumentLoader();
  }

  private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, Object source) {
    registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      ConfigurationClassPostProcessor.class, source);
    registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      RequiredAnnotationBeanPostProcessor.class, source);
    registerInjectorProcessor(registry);
  }

  protected void registerInjectorProcessor(BeanDefinitionRegistry registry) {
    if (artifactType.equals(ArtifactType.APP)) {
      registerAnnotationConfigProcessor(registry, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, MuleInjectorProcessor.class, null);
    } else if (artifactType.equals(ArtifactType.DOMAIN)) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ContextExclusiveInjectorProcessor.class);
      builder.addConstructorArgValue(this);
      registerPostProcessor(registry, (RootBeanDefinition) builder.getBeanDefinition(), AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME);
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

  protected Class<? extends MuleBeanDefinitionDocumentReader> getBeanDefinitionDocumentReaderClass() {
    if (artifactType.equals(ArtifactType.DOMAIN)) {
      return MuleDomainBeanDefinitionDocumentReader.class;
    }
    return MuleBeanDefinitionDocumentReader.class;
  }

  @Override
  protected DefaultListableBeanFactory createBeanFactory() {
    // Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(getInternalParentBeanFactory());
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
  protected static void postProcessBeanDefinition(ComponentModel resolvedComponent, BeanDefinitionRegistry registry,
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

  protected OptionalObjectsController getOptionalObjectsController() {
    return optionalObjectsController;
  }

  public static ThreadLocal<MuleContext> getCurrentMuleContext() {
    return currentMuleContext;
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

  private class XmlServiceRegistry extends AbstractServiceRegistry {

    private final ServiceRegistry delegate;
    private final XmlNamespaceInfoProvider extensionsXmlInfoProvider;

    public XmlServiceRegistry(ServiceRegistry delegate, MuleContext muleContext) {
      this.delegate = delegate;
      final ExtensionManager extensionManager = muleContext.getExtensionManager();
      List<XmlNamespaceInfo> extensionNamespaces;
      if (extensionManager != null) {
        extensionNamespaces = extensionManager.getExtensions().stream()
            .map(ext -> {
              XmlDslModel xmlDslModel = ext.getXmlDslModel();
              return xmlDslModel != null
                  ? new StaticXmlNamespaceInfo(xmlDslModel.getNamespace(), xmlDslModel.getPrefix()) : null;
            })
            .filter(info -> info != null)
            .collect(new ImmutableListCollector<>());
      } else {
        extensionNamespaces = ImmutableList.of();
      }

      extensionsXmlInfoProvider = new StaticXmlNamespaceInfoProvider(extensionNamespaces);
    }

    @Override
    protected <T> Collection<T> doLookupProviders(Class<T> providerClass, ClassLoader classLoader) {
      Collection<T> providers = delegate.lookupProviders(providerClass, classLoader);
      if (XmlNamespaceInfoProvider.class.equals(providerClass)) {
        providers = ImmutableList.<T>builder().addAll(providers).add((T) extensionsXmlInfoProvider).build();
      }

      return providers;
    }
  }
}
