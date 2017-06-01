/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.TemplateParser.createMuleStyleParser;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.requiresConfig;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataProvider;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
public abstract class ExtensionComponent<T extends ComponentModel> extends AbstractAnnotatedObject
    implements MuleContextAware, MetadataKeyProvider, MetadataProvider<T>, FlowConstructAware, Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(ExtensionComponent.class);

  protected final ExtensionManager extensionManager;
  private final TemplateParser expressionParser = createMuleStyleParser();
  private final ExtensionModel extensionModel;
  private final T componentModel;
  private final AtomicReference<ConfigurationProvider> configurationProvider = new AtomicReference<>();
  private final MetadataMediator<T> metadataMediator;
  private final ClassTypeLoader typeLoader;
  private final LazyValue<Boolean> requiresConfig = new LazyValue<>(this::computeRequiresConfig);
  protected final ClassLoader classLoader;

  private CursorProviderFactory cursorProviderFactory;
  protected FlowConstruct flowConstruct;
  protected MuleContext muleContext;

  @Inject
  protected ConnectionManagerAdapter connectionManager;

  @Inject
  protected StreamingManager streamingManager;

  @Inject
  private MuleMetadataService metadataService;

  protected ExtensionComponent(ExtensionModel extensionModel,
                               T componentModel,
                               ConfigurationProvider configurationProvider,
                               CursorProviderFactory cursorProviderFactory,
                               ExtensionManager extensionManager) {
    this.extensionModel = extensionModel;
    this.classLoader = getClassLoader(extensionModel);
    this.componentModel = componentModel;
    this.configurationProvider.set(configurationProvider);
    this.extensionManager = extensionManager;
    this.cursorProviderFactory = cursorProviderFactory;
    this.metadataMediator = new MetadataMediator<>(componentModel);
    this.typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(classLoader);
  }

  /**
   * Makes sure that the operation is valid by invoking {@link #validateOperationConfiguration(ConfigurationProvider)} and then
   * delegates on {@link #doInitialise()} for custom initialisation
   *
   * @throws InitialisationException if a fatal error occurs causing the Mule instance to shutdown
   */
  @Override
  public final void initialise() throws InitialisationException {
    if (cursorProviderFactory == null) {
      cursorProviderFactory = componentModel.getModelProperty(PagedOperationModelProperty.class)
          .map(p -> (CursorProviderFactory) streamingManager.forObjects().getDefaultCursorProviderFactory())
          .orElseGet(() -> streamingManager.forBytes().getDefaultCursorProviderFactory());
    }
    withContextClassLoader(classLoader, () -> {
      validateConfigurationProviderIsNotExpression();
      findConfigurationProvider().ifPresent(this::validateOperationConfiguration);
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
    withContextClassLoader(classLoader, () -> {
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
    withContextClassLoader(classLoader, () -> {
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
      withContextClassLoader(classLoader, () -> {
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
   * {@link ComponentModel}
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
    try {
      return runWithMetadataContext(
                                    context -> withContextClassLoader(getClassLoader(this.extensionModel),
                                                                      () -> metadataMediator.getMetadataKeys(context)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onKeys());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<T>> getMetadata() throws MetadataResolvingException {
    try {
      return runWithMetadataContext(context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator
                                                                          .getMetadata(context, getParameterValueResolver())));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<T>> getMetadata(MetadataKey key) throws MetadataResolvingException {
    try {
      return runWithMetadataContext(context -> withContextClassLoader(getClassLoader(this.extensionModel),
                                                                      () -> metadataMediator.getMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }


  protected <R> R runWithMetadataContext(Function<MetadataContext, R> metadataContextFunction)
      throws MetadataResolvingException, ConnectionException {
    MetadataContext context = getMetadataContext();
    R result = metadataContextFunction.apply(context);
    context.dispose();
    return result;
  }

  private MetadataContext getMetadataContext() throws MetadataResolvingException, ConnectionException {
    Event fakeEvent = getInitialiserEvent(muleContext);

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

    return new DefaultMetadataContext(configuration, connectionManager, metadataService.getMetadataCache(cacheId), typeLoader);
  }

  /**
   * @param event a {@link Event}
   * @return a configuration instance for the current component with a given {@link Event}
   */
  protected Optional<ConfigurationInstance> getConfiguration(Event event) {
    if (!requiresConfig.get()) {
      return empty();
    }

    if (isConfigurationSpecified()) {
      return of(configurationProvider.get())
          .map(provider -> ofNullable(provider.get(event)))
          .orElseThrow(() -> new IllegalModelDefinitionException(format(
                                                                        "Flow '%s' contains a reference to config '%s' but it doesn't exists",
                                                                        flowConstruct.getName(), configurationProvider)));
    }

    return extensionManager.getConfigurationProvider(extensionModel, componentModel)
        .map(provider -> {
          configurationProvider.set(provider);
          return ofNullable(provider.get(event));
        })
        .orElseGet(() -> extensionManager.getConfiguration(extensionModel, componentModel, event));
  }

  protected CursorProviderFactory getCursorProviderFactory() {
    return cursorProviderFactory;
  }

  private Optional<ConfigurationProvider> findConfigurationProvider() {
    if (isConfigurationSpecified()) {
      return of(configurationProvider.get());
    }

    return extensionManager.getConfigurationProvider(extensionModel, componentModel);
  }

  private boolean isConfigurationSpecified() {
    return configurationProvider.get() != null;
  }

  private boolean computeRequiresConfig() {
    return requiresConfig(extensionModel, componentModel);
  }

  private void validateConfigurationProviderIsNotExpression() throws InitialisationException {
    if (isConfigurationSpecified() && expressionParser.isContainsTemplate(configurationProvider.get().getName())) {
      throw new InitialisationException(
                                        createStaticMessage(format("Flow '%s' defines component '%s' which specifies the expression '%s' as a config-ref. "
                                            + "Expressions are not allowed as config references", flowConstruct.getName(),
                                                                   hyphenize(componentModel.getName()),
                                                                   configurationProvider)),
                                        this);
    }
  }

  protected abstract ParameterValueResolver getParameterValueResolver();

  /**
   * @return the extension model where the component has been defined.
   */
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }
}
