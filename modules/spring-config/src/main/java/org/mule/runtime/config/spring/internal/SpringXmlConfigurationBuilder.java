/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.config.spring.internal.artifact.SpringArtifactContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigResource;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ParentMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a Spring XML Configuration file used with Mule
 * name-spaces. Multiple configuration files can be loaded from this builder (specified as a comma-separated list).
 */
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder
    implements ParentMuleContextAwareConfigurationBuilder {

  private ArtifactDeclaration artifactDeclaration = new ArtifactDeclaration();
  private boolean enableLazyInit = false;
  protected boolean useDefaultConfigResource = true;
  protected boolean useMinimalConfigResource = false;

  private SpringRegistry registry;

  private ApplicationContext domainContext;
  private ApplicationContext parentContext;
  private MuleArtifactContext muleArtifactContext;
  private ArtifactType artifactType;

  public SpringXmlConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                       ArtifactType artifactType, boolean enableLazyInit)
      throws ConfigurationException {
    super(configResources, artifactProperties);
    this.artifactType = artifactType;
    this.enableLazyInit = enableLazyInit;
  }

  public SpringXmlConfigurationBuilder(String configResources, Map<String, String> artifactProperties, ArtifactType artifactType)
      throws ConfigurationException {
    this(new String[] {configResources}, artifactProperties, artifactType, false);
  }

  public SpringXmlConfigurationBuilder(String configResource) throws ConfigurationException {
    this(configResource, emptyMap(), APP);
  }

  public SpringXmlConfigurationBuilder(String[] configFiles, boolean enableLazyInit) throws ConfigurationException {
    super(configFiles, emptyMap());
    this.artifactType = APP;
    this.enableLazyInit = enableLazyInit;
  }

  public SpringXmlConfigurationBuilder(String[] configurationFiles, ArtifactDeclaration artifactDeclaration,
                                       Map<String, String> artifactProperties, ArtifactType artifactType,
                                       boolean enableLazyInitialisation)
      throws ConfigurationException {
    this(configurationFiles, artifactProperties, artifactType, enableLazyInitialisation);
    this.artifactDeclaration = artifactDeclaration;
    this.artifactType = APP;
  }

  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, MuleContext domainContext,
                                                                boolean lazyInit)
      throws ConfigurationException {
    final SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(configResources, emptyMap(), APP, lazyInit);
    if (domainContext != null) {
      springXmlConfigurationBuilder.setParentContext(domainContext);
    }
    return springXmlConfigurationBuilder;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    muleArtifactContext = createApplicationContext(muleContext);
    createSpringRegistry(muleContext, muleArtifactContext);
  }

  /**
   * Template method for modifying the list of resources to be loaded. This operation is a no-op by default.
   *
   * @param allResources the list of {@link ConfigResource} to be loaded
   */
  @SuppressWarnings("unused")
  protected void addResources(List<ConfigResource> allResources) {}

  private MuleArtifactContext createApplicationContext(MuleContext muleContext) throws Exception {
    OptionalObjectsController applicationObjectcontroller = new DefaultOptionalObjectsController();
    OptionalObjectsController parentObjectController = null;
    ApplicationContext parentApplicationContext = parentContext != null ? parentContext : domainContext;

    if (parentApplicationContext instanceof MuleArtifactContext) {
      parentObjectController = ((MuleArtifactContext) parentApplicationContext).getOptionalObjectsController();
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
    if (enableLazyInit) {
      return new LazyMuleArtifactContext(muleContext, resolveArtifactConfigResources(), artifactDeclaration,
                                         optionalObjectsController,
                                         getArtifactProperties(), artifactType, resolveContextArtifactPluginClassLoaders(),
                                         resolveParentConfigurationProperties());
    }

    return new MuleArtifactContext(muleContext, resolveArtifactConfigResources(), artifactDeclaration, optionalObjectsController,
                                   getArtifactProperties(), artifactType, resolveContextArtifactPluginClassLoaders(),
                                   resolveParentConfigurationProperties());
  }

  protected ConfigResource[] resolveArtifactConfigResources() {
    return artifactConfigResources;
  }

  protected Optional<ConfigurationProperties> resolveParentConfigurationProperties() {
    ApplicationContext parentApplicationContext = parentContext != null ? parentContext : domainContext;

    Optional<ConfigurationProperties> parentConfigurationProperties = empty();
    if (parentApplicationContext != null) {
      parentConfigurationProperties = of(parentApplicationContext.getBean(ConfigurationProperties.class));
    }

    return parentConfigurationProperties;
  }

  private void createSpringRegistry(MuleContext muleContext, ApplicationContext applicationContext) throws Exception {
    if (parentContext != null && domainContext != null) {
      throw new IllegalStateException("An application with a web xml context and domain resources is not supported");
    }
    if (parentContext != null) {
      createRegistryWithParentContext(muleContext, applicationContext, parentContext);
    } else if (domainContext != null) {
      createRegistryWithParentContext(muleContext, applicationContext, domainContext);
    } else {
      registry = new SpringRegistry(applicationContext, muleContext, muleArtifactContext.getDependencyResolver());
    }

    // Note: The SpringRegistry must be created before
    // muleArtifactContext.refresh() gets called because
    // some beans may try to look up other beans via the Registry during
    // preInstantiateSingletons().
    ((MuleContextWithRegistries) muleContext).addRegistry(registry);
  }

  private void createRegistryWithParentContext(MuleContext muleContext, ApplicationContext applicationContext,
                                               ApplicationContext parentContext)
      throws ConfigurationException {
    if (applicationContext instanceof ConfigurableApplicationContext) {
      ((ConfigurableApplicationContext) applicationContext).setParent(parentContext);
      registry = new SpringRegistry(applicationContext, muleContext, muleArtifactContext.getDependencyResolver());
    } else {
      throw new ConfigurationException(I18nMessageFactory
          .createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
    }
  }

  @Override
  protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception {
    // If the MuleContext is started, start all objects in the new Registry.
    if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME)) {
      lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
    }
  }

  public void setUseDefaultConfigResource(boolean useDefaultConfigResource) {
    this.useDefaultConfigResource = useDefaultConfigResource;
  }

  public ArtifactContext createArtifactContext() {
    return new SpringArtifactContext(muleArtifactContext);
  }

  public void setUseMinimalConfigResource(boolean useMinimalConfigResource) {
    this.useMinimalConfigResource = useMinimalConfigResource;
  }

  protected ApplicationContext getParentContext() {
    return parentContext;
  }

  public void setParentContext(ApplicationContext parentContext) {
    this.parentContext = parentContext;
  }

  @Override
  public void setParentContext(MuleContext domainContext) {
    this.domainContext = ((MuleContextWithRegistries) domainContext).getRegistry().get("springApplicationContext");
  }
}
