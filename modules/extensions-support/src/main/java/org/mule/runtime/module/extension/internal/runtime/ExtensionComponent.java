/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.TemplateParser.createMuleStyleParser;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.requiresConfig;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataProvider;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.exception.TooManyConfigsException;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that groups all the common behaviour between different extension's components, like {@link OperationMessageProcessor} and
 * {@link ExtensionMessageSource}.
 * <p>
 * Provides capabilities of Metadata resolution and configuration validation.
 *
 * @since 4.0
 */
public abstract class ExtensionComponent
    implements MuleContextAware, MetadataKeyProvider, MetadataProvider, FlowConstructAware, Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(ExtensionComponent.class);
  protected final ExtensionManagerAdapter extensionManager;
  private final TemplateParser expressionParser = createMuleStyleParser();
  private final RuntimeExtensionModel extensionModel;
  private final ComponentModel componentModel;
  private final String configurationProviderName;
  private final MetadataMediator metadataMediator;
  private final ClassTypeLoader typeLoader;

  protected FlowConstruct flowConstruct;
  protected MuleContext muleContext;

  @Inject
  protected ConnectionManagerAdapter connectionManager;

  @Inject
  private MuleMetadataManager metadataManager;

  protected ExtensionComponent(RuntimeExtensionModel extensionModel,
                               RuntimeComponentModel componentModel,
                               String configurationProviderName,
                               ExtensionManagerAdapter extensionManager) {
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
    this.configurationProviderName = configurationProviderName;
    this.extensionManager = extensionManager;
    this.metadataMediator = new MetadataMediator(extensionModel, componentModel);
    this.typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(getExtensionClassLoader());
  }

  /**
   * Makes sure that the operation is valid by invoking {@link #validateOperationConfiguration(ConfigurationProvider)} and then
   * delegates on {@link #doInitialise()} for custom initialisation
   *
   * @throws InitialisationException if a fatal error occurs causing the Mule instance to shutdown
   */
  @Override
  public final void initialise() throws InitialisationException {
    withContextClassLoader(getExtensionClassLoader(), () -> {
      validateConfigurationProviderIsNotExpression();
      Optional<ConfigurationProvider> provider = findConfigurationProvider();

      if (provider.isPresent()) {
        validateOperationConfiguration(provider.get());
      }

      doInitialise();
      return null;
    }, InitialisationException.class, e -> {
      throw new InitialisationException(e, this);
    });
  }

  /**
   * Delegates into {@link #doStart()} making sure that it executes using the extension's class loader
   *
   * @throws MuleException if the phase couldn't be applied
   */
  @Override
  public final void start() throws MuleException {
    withContextClassLoader(getExtensionClassLoader(), () -> {
      doStart();
      return null;
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    });
  }

  /**
   * Delegates into {@link #doStop()} making sure that it executes using the extension's class loader
   *
   * @throws MuleException if the phase couldn't be applied
   */
  @Override
  public final void stop() throws MuleException {
    withContextClassLoader(getExtensionClassLoader(), () -> {
      doStop();
      return null;
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    });
  }

  /**
   * Delegates into {@link #doDispose()} making sure that it executes using the extension's class loader
   */
  @Override
  public final void dispose() {
    try {
      withContextClassLoader(getExtensionClassLoader(), () -> {
        doDispose();
        return null;
      });
    } catch (Exception e) {
      LOGGER.warn("Exception found trying to dispose component", e);
    }
  }

  /**
   * Implementors will use this method to perform their own initialisation logic
   *
   * @throws InitialisationException if a fatal error occurs causing the Mule instance to shutdown
   */
  protected abstract void doInitialise() throws InitialisationException;

  /**
   * Implementors will use this method to perform their own starting logic
   *
   * @throws MuleException if the component could not start
   */
  protected abstract void doStart() throws MuleException;

  /**
   * Implementors will use this method to perform their own stopping logic
   *
   * @throws MuleException if the component could not stop
   */
  protected abstract void doStop() throws MuleException;

  /**
   * Implementors will use this method to perform their own disposing logic
   */
  protected abstract void doDispose();

  /**
   * Validates that the configuration returned by the {@code configurationProvider} is compatible with the associated
   * {@link RuntimeComponentModel}
   *
   * @param configurationProvider
   */
  protected abstract void validateOperationConfiguration(ConfigurationProvider configurationProvider);

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys() throws MetadataResolvingException {
    final MetadataContext metadataContext = getMetadataContext();
    return withContextClassLoader(getClassLoader(this.extensionModel), () -> metadataMediator.getMetadataKeys(metadataContext));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor> getMetadata() throws MetadataResolvingException {
    return withContextClassLoader(getClassLoader(this.extensionModel), () -> metadataMediator.getMetadata());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataKey key) throws MetadataResolvingException {
    final MetadataContext metadataContext = getMetadataContext();
    return withContextClassLoader(getClassLoader(this.extensionModel), () -> metadataMediator.getMetadata(metadataContext, key));
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  protected MetadataContext getMetadataContext() throws MetadataResolvingException {
    MuleEvent fakeEvent = getInitialiserEvent(muleContext);

    Optional<ConfigurationInstance> configuration = getConfiguration(fakeEvent);

    if (configuration.isPresent()) {
      ConfigurationProvider configurationProvider = findConfigurationProvider()
          .orElseThrow(() -> new MetadataResolvingException("Failed to create the required configuration for Metadata retrieval",
                                                            FailureCode.INVALID_CONFIGURATION));

      if (configurationProvider instanceof DynamicConfigurationProvider) {
        throw new MetadataResolvingException("Configuration used for Metadata fetch cannot be dynamic",
                                             FailureCode.INVALID_CONFIGURATION);
      }
    }

    String cacheId = configuration.map(ConfigurationInstance::getName)
        .orElseGet(() -> extensionModel.getName() + "|" + componentModel.getName());

    return new DefaultMetadataContext(configuration, connectionManager, metadataManager.getMetadataCache(cacheId), typeLoader);
  }

  /**
   * @param event a {@link MuleEvent}
   * @return a configuration instance for the current component with a given {@link MuleEvent}
   */
  protected Optional<ConfigurationInstance> getConfiguration(MuleEvent event) {
    if (!requiresConfig(componentModel)) {
      return empty();
    }

    if (isConfigurationSpecified()) {
      return getConfigurationProviderByName()
          .map(provider -> Optional.of(provider.get(event)))
          .orElseThrow(() -> new IllegalModelDefinitionException(format(
                                                                        "Flow '%s' contains a reference to config '%s' but it doesn't exists",
                                                                        flowConstruct.getName(), configurationProviderName)));
    }

    return Optional.of(getConfigurationProviderByModel()
        .map(provider -> provider.get(event))
        .orElseGet(() -> extensionManager.getConfiguration(extensionModel, event)));
  }

  protected ClassLoader getExtensionClassLoader() {
    return getClassLoader(extensionModel);
  }

  private Optional<ConfigurationProvider> findConfigurationProvider() {
    if (requiresConfig(componentModel)) {
      return isConfigurationSpecified()
          ? getConfigurationProviderByName()
          : getConfigurationProviderByModel();
    }

    return empty();
  }

  private Optional<ConfigurationProvider> getConfigurationProviderByName() {
    return extensionManager.getConfigurationProvider(configurationProviderName);
  }

  private Optional<ConfigurationProvider> getConfigurationProviderByModel() {
    try {
      return extensionManager.getConfigurationProvider(extensionModel);
    } catch (TooManyConfigsException e) {
      throw new IllegalStateException(String.format(
                                                    "No config-ref was specified for component '%s' of extension '%s', but %d are registered. Please specify which to use",
                                                    componentModel.getName(), extensionModel.getName(), e.getConfigsCount()),
                                      e);
    }
  }

  private boolean isConfigurationSpecified() {
    return !isBlank(configurationProviderName);
  }

  private void validateConfigurationProviderIsNotExpression() throws InitialisationException {
    if (isConfigurationSpecified() && expressionParser.isContainsTemplate(configurationProviderName)) {
      throw new InitialisationException(createStaticMessage(format("Flow '%s' defines component '%s' which specifies the expression '%s' as a config-ref. "
          + "Expressions are not allowed as config references", flowConstruct.getName(), hyphenize(componentModel.getName()),
                                                                   configurationProviderName)),
                                        this);
    }
  }

  protected String getConfigurationProviderName() {
    return configurationProviderName;
  }
}
