/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.SHARE_ERROR_TYPE_REPOSITORY_PROPERTY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.internal.artifact.SpringArtifactContext;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.StaticConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.config.ParentMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.core.internal.exception.FilteredErrorTypeRepository;
import org.mule.runtime.core.internal.registry.CompositeMuleRegistryHelper;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.dsl.api.ConfigResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a Spring XML Configuration file used with Mule
 * name-spaces. Multiple configuration files can be loaded from this builder (specified as a comma-separated list).
 */
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder
    implements ParentMuleContextAwareConfigurationBuilder, ArtifactContextFactory {

  private ArtifactDeclaration artifactDeclaration;
  private boolean enableLazyInit = false;
  private boolean disableXmlValidations = false;

  private SpringRegistry registry;

  private ArtifactAst parentArtifactAst;
  private ApplicationContext parentContext;
  private MuleArtifactContext muleArtifactContext;
  private final ArtifactType artifactType;
  private final LockFactory runtimeLockFactory;
  private Optional<ComponentBuildingDefinitionRegistryFactory> componentBuildingDefinitionRegistryFactory = empty();

  private SpringXmlConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                        ArtifactType artifactType, boolean enableLazyInit, boolean disableXmlValidations,
                                        LockFactory runtimeLockFactory)
      throws ConfigurationException {
    super(configResources, artifactProperties);
    this.artifactType = artifactType;
    this.enableLazyInit = enableLazyInit;
    this.disableXmlValidations = disableXmlValidations;
    this.runtimeLockFactory = runtimeLockFactory;
  }

  public SpringXmlConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                       ArtifactType artifactType, boolean enableLazyInit, boolean disableXmlValidations)
      throws ConfigurationException {
    this(configResources, artifactProperties, artifactType, enableLazyInit, disableXmlValidations,
         getRuntimeLockFactory());
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String configResources, Map<String, String> artifactProperties, ArtifactType artifactType)
      throws ConfigurationException {
    this(new String[] {configResources}, artifactProperties, artifactType, false, false);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String configResource) throws ConfigurationException {
    this(configResource, emptyMap(), APP);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String[] configFiles, Map<String, String> artifactProperties)
      throws ConfigurationException {
    this(configFiles, artifactProperties, APP, false, false);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String[] configFiles, boolean enableLazyInit, boolean disableXmlValidations)
      throws ConfigurationException {
    this(configFiles, emptyMap(), APP, enableLazyInit, disableXmlValidations, getRuntimeLockFactory());
  }

  public SpringXmlConfigurationBuilder(String[] configurationFiles, ArtifactDeclaration artifactDeclaration,
                                       Map<String, String> artifactProperties, ArtifactType artifactType,
                                       boolean enableLazyInitialisation, boolean disableXmlValidations,
                                       LockFactory runtimeLockFactory)
      throws ConfigurationException {
    this(configurationFiles, artifactProperties, artifactType, enableLazyInitialisation, disableXmlValidations,
         runtimeLockFactory);
    this.artifactDeclaration = artifactDeclaration;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    if (isEmpty(artifactConfigResources) && artifactType == DOMAIN) {
      ((DefaultMuleContext) muleContext).setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return;
    }

    muleArtifactContext = createApplicationContext(muleContext);
    createSpringRegistry(muleContext, muleArtifactContext);
  }

  /**
   * Template method for modifying the list of resources to be loaded. This operation is a no-op by default.
   *
   * @param allResources the list of {@link ConfigResource} to be loaded
   */
  protected void addResources(List<ConfigResource> allResources) {}

  private MuleArtifactContext createApplicationContext(MuleContext muleContext) throws Exception {
    OptionalObjectsController applicationObjectcontroller = new DefaultOptionalObjectsController();
    OptionalObjectsController parentObjectController = null;

    if (parentContext instanceof MuleArtifactContext) {
      parentObjectController = ((MuleArtifactContext) parentContext).getOptionalObjectsController();
    }

    if (parentObjectController != null) {
      applicationObjectcontroller = new CompositeOptionalObjectsController(applicationObjectcontroller, parentObjectController);
    }

    // TODO MULE-10084 : Refactor to only accept artifactConfiguration and not artifactConfigResources
    final MuleArtifactContext muleArtifactContext =
        doCreateApplicationContext(muleContext, artifactDeclaration, applicationObjectcontroller);
    serviceConfigurators.forEach(serviceConfigurator -> serviceConfigurator.configure(muleContext.getCustomizationService()));
    return muleArtifactContext;
  }

  private MuleArtifactContext doCreateApplicationContext(MuleContext muleContext,
                                                         ArtifactDeclaration artifactDeclaration,
                                                         OptionalObjectsController optionalObjectsController) {
    ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory =
        this.componentBuildingDefinitionRegistryFactory
            .orElse(new DefaultComponentBuildingDefinitionRegistryFactory());

    final ArtifactAst artifactAst =
        createApplicationModel(getExtensions(muleContext.getExtensionManager()),
                               artifactDeclaration, resolveArtifactConfigResources(), getArtifactProperties(),
                               disableXmlValidations);

    MuleArtifactContext muleArtifactContext;
    if (enableLazyInit) {
      muleArtifactContext = new LazyMuleArtifactContext(muleContext, artifactAst,
                                                        optionalObjectsController,
                                                        resolveParentConfigurationProperties(),
                                                        getArtifactProperties(), artifactType,
                                                        resolveComponentModelInitializer(),
                                                        runtimeLockFactory,
                                                        componentBuildingDefinitionRegistryFactory);
    } else {
      muleArtifactContext = new MuleArtifactContext(muleContext, artifactAst,
                                                    optionalObjectsController,
                                                    resolveParentConfigurationProperties(),
                                                    getArtifactProperties(), artifactType,
                                                    componentBuildingDefinitionRegistryFactory);
      muleArtifactContext.initialize();
    }

    return muleArtifactContext;
  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  private ArtifactAst createApplicationModel(Set<ExtensionModel> extensions,
                                             ArtifactDeclaration artifactDeclaration,
                                             ConfigResource[] artifactConfigResources,
                                             Map<String, String> artifactProperties,
                                             boolean disableXmlValidations) {
    try {
      final ArtifactAst artifactAst;

      if (artifactDeclaration == null) {
        if (artifactConfigResources.length == 0) {
          artifactAst = emptyArtifact();
        } else {
          final AstXmlParser parser =
              createMuleXmlParser(extensions, artifactProperties, disableXmlValidations);

          artifactAst = parser.parse(stream(artifactConfigResources)
              .map((CheckedFunction<ConfigResource, Pair<String, InputStream>>) (configFile -> new Pair<>(configFile
                  .getResourceName(), configFile.getInputStream())))
              .collect(toList()));
        }
      } else {
        artifactAst = toArtifactast(artifactDeclaration, extensions);
      }

      return artifactAst;
    } catch (MuleRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private AstXmlParser createMuleXmlParser(Set<ExtensionModel> extensions,
                                           Map<String, String> artifactProperties, boolean disableXmlValidations) {
    ConfigurationPropertiesResolver propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(), new StaticConfigurationPropertiesProvider(artifactProperties));

    Builder builder = AstXmlParser.builder()
        .withPropertyResolver(propertyKey -> (String) propertyResolver.resolveValue(propertyKey))
        // TODO MULE-19203 for policies this includes all extensions from the app as well. It should be just the ones
        // declared in the policy, with a feature flag for getting the ones from the app as well (ref:
        // MuleSystemProperties#SHARE_ERROR_TYPE_REPOSITORY_PROPERTY).
        .withExtensionModels(extensions)
        .withParentArtifact(resolveParentArtifact());
    if (disableXmlValidations) {
      builder = builder.withSchemaValidationsDisabled();
    }
    return builder.build();
  }

  protected ArtifactAst resolveParentArtifact() {
    if (POLICY.equals(artifactType)) {
      if (shareErrorTypeRepository()) {
        // Because MULE-18196 breaks backwards, we need this feature flag to allow legacy behavior
        return parentArtifactAst;
      } else {
        return new ArtifactAst() {

          @Override
          public Set<ExtensionModel> dependencies() {
            return parentArtifactAst.dependencies();
          }

          @Override
          public Optional<ArtifactAst> getParent() {
            return parentArtifactAst.getParent();
          }

          @Override
          public Stream<ComponentAst> recursiveStream(AstTraversalDirection direction) {
            return parentArtifactAst.recursiveStream(direction);
          }

          @Override
          public Spliterator<ComponentAst> recursiveSpliterator(AstTraversalDirection direction) {
            return parentArtifactAst.recursiveSpliterator(direction);
          }

          @Override
          public Stream<ComponentAst> topLevelComponentsStream() {
            return parentArtifactAst.topLevelComponentsStream();
          }

          @Override
          public Spliterator<ComponentAst> topLevelComponentsSpliterator() {
            return parentArtifactAst.topLevelComponentsSpliterator();
          }

          @Override
          public void updatePropertiesResolver(UnaryOperator<String> newPropertiesResolver) {
            parentArtifactAst.updatePropertiesResolver(newPropertiesResolver);
          }

          @Override
          public ErrorTypeRepository getErrorTypeRepository() {
            // Since there is already a workaround to allow polices to use http connector without declaring the dependency
            // and relying on it provided by the app, this case has to be accounted for here when handling error codes as
            // well.
            return new FilteredErrorTypeRepository(parentArtifactAst.getErrorTypeRepository(), singleton("HTTP"));
          }
        };
      }
    }
    return parentArtifactAst;
  }

  private boolean shareErrorTypeRepository() {
    return getBoolean(SHARE_ERROR_TYPE_REPOSITORY_PROPERTY);
  }

  private ConfigResource[] resolveArtifactConfigResources() {
    return artifactConfigResources;
  }

  private Optional<ConfigurationProperties> resolveParentConfigurationProperties() {
    Optional<ConfigurationProperties> parentConfigurationProperties = empty();
    if (parentContext != null) {
      parentConfigurationProperties = of(parentContext.getBean(ConfigurationProperties.class));
    }

    return parentConfigurationProperties;
  }

  private Optional<ComponentModelInitializer> resolveComponentModelInitializer() {
    Optional<ComponentModelInitializer> parentLazyComponentInitializer = empty();
    if (parentContext != null && parentContext instanceof ComponentModelInitializer) {
      parentLazyComponentInitializer = of((ComponentModelInitializer) parentContext);
    }

    return parentLazyComponentInitializer;
  }

  private void createSpringRegistry(MuleContext muleContext, ApplicationContext applicationContext) throws Exception {
    // Note: The SpringRegistry must be created before
    // muleArtifactContext.refresh() gets called because
    // some beans may try to look up other beans via the Registry during
    // preInstantiateSingletons().
    if (parentContext != null) {
      createRegistryWithParentContext(muleContext, applicationContext, parentContext);

      if ((parentContext instanceof MuleArtifactContext &&
          ((MuleArtifactContext) parentContext).getMuleContext().getRegistry() instanceof MuleRegistryHelper)) {
        MuleRegistryHelper parentMuleRegistryHelper =
            (MuleRegistryHelper) ((MuleArtifactContext) parentContext).getMuleContext().getRegistry();

        CompositeMuleRegistryHelper compositeMuleRegistryHelper =
            new CompositeMuleRegistryHelper(registry, muleContext, parentMuleRegistryHelper);
        ((MuleContextWithRegistry) muleContext).setRegistry(compositeMuleRegistryHelper);
      } else {
        ((MuleContextWithRegistry) muleContext).setRegistry(registry);
      }

    } else {
      registry = new SpringRegistry(applicationContext, muleContext,
                                    new ConfigurationDependencyResolver(muleArtifactContext.getApplicationModel()),
                                    ((DefaultMuleContext) muleContext).getLifecycleInterceptor());

      ((MuleContextWithRegistry) muleContext).setRegistry(registry);
    }

    ((DefaultMuleContext) muleContext).setInjector(registry);
  }

  private void createRegistryWithParentContext(MuleContext muleContext, ApplicationContext applicationContext,
                                               ApplicationContext parentContext)
      throws ConfigurationException {
    if (applicationContext instanceof ConfigurableApplicationContext) {
      ((ConfigurableApplicationContext) applicationContext).setParent(parentContext);
      registry = new SpringRegistry(applicationContext, muleContext,
                                    new ConfigurationDependencyResolver(muleArtifactContext.getApplicationModel()),
                                    ((DefaultMuleContext) muleContext).getLifecycleInterceptor());
    } else {
      throw new ConfigurationException(createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
    }
  }

  @Override
  protected synchronized void applyLifecycle(LifecycleManager lifecycleManager) throws Exception {
    // If the MuleContext is started, start all objects in the new Registry.
    if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME)) {
      lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
    }
  }

  @Override
  public ArtifactContext createArtifactContext() {
    return new SpringArtifactContext(muleArtifactContext);
  }

  @Override
  public void setParentContext(MuleContext domainContext, ArtifactAst parentAst) {
    this.parentContext = ((MuleContextWithRegistry) domainContext).getRegistry().get("springApplicationContext");
    this.parentArtifactAst = parentAst;
  }

  public void setComponentBuildingDefinitionRegistryFactory(ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory) {
    this.componentBuildingDefinitionRegistryFactory = ofNullable(componentBuildingDefinitionRegistryFactory);
  }
}
