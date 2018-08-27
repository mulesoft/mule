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
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;
import static org.mule.runtime.core.privileged.util.TemplateParser.createMuleStyleParser;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.requiresConfig;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getValueProviderModels;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataProvider;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.util.TemplateParser;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.values.ComponentValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.ValueProviderMediator;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public abstract class ExtensionComponent<T extends ComponentModel> extends AbstractComponent
    implements MuleContextAware, MetadataKeyProvider, MetadataProvider<T>, ComponentValueProvider,
    Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(ExtensionComponent.class);

  private final TemplateParser expressionParser = createMuleStyleParser();
  private final ExtensionModel extensionModel;
  private final AtomicReference<ConfigurationProvider> configurationProvider = new AtomicReference<>();
  private final MetadataMediator<T> metadataMediator;
  private final ValueProviderMediator<T> valueProviderMediator;
  private final ClassTypeLoader typeLoader;
  private final LazyValue<Boolean> requiresConfig = new LazyValue<>(this::computeRequiresConfig);

  protected final ExtensionManager extensionManager;
  protected final ClassLoader classLoader;
  protected final T componentModel;

  protected CursorProviderFactory cursorProviderFactory;

  protected MuleContext muleContext;

  @Inject
  protected ConnectionManagerAdapter connectionManager;

  @Inject
  protected StreamingManager streamingManager;

  @Inject
  protected TransactionFactoryLocator transactionFactoryLocator;

  @Inject
  protected MuleMetadataService metadataService;

  @Inject
  private ConfigurationComponentLocator componentLocator;

  @Inject
  protected ReflectionCache reflectionCache;

  @Inject
  private MetadataCacheIdGeneratorFactory<ComponentConfiguration> cacheIdGeneratorFactory;

  protected MetadataCacheIdGenerator<ComponentConfiguration> cacheIdGenerator;


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
    this.valueProviderMediator = new ValueProviderMediator<>(componentModel, () -> muleContext, () -> reflectionCache);
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

    setCacheIdGenerator();
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
                                    context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getMetadataKeys(context,
                                                                                                             getParameterValueResolver(),
                                                                                                             reflectionCache)));
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
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader, () -> metadataMediator
                                        .getMetadata(context, getParameterValueResolver(),
                                                     reflectionCache)));
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
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Value> getValues(String parameterName) throws ValueResolvingException {
    try {
      return runWithValueProvidersContext(context -> withContextClassLoader(classLoader,
                                                                            () -> valueProviderMediator
                                                                                .getValues(parameterName,
                                                                                           getParameterValueResolver(),
                                                                                           (CheckedSupplier<Object>) () -> context
                                                                                               .getConnection().orElse(null),
                                                                                           (CheckedSupplier<Object>) () -> context
                                                                                               .getConfig().orElse(null))));
    } catch (MuleRuntimeException e) {
      Throwable rootException = getRootException(e);
      if (rootException instanceof ValueResolvingException) {
        throw (ValueResolvingException) rootException;
      } else {
        throw new ValueResolvingException("An unknown error occurred trying to resolve values. " + e.getCause().getMessage(),
                                          UNKNOWN, e);
      }
    } catch (Exception e) {
      throw new ValueResolvingException("An unknown error occurred trying to resolve values. " + e.getCause().getMessage(),
                                        UNKNOWN, e);
    }
  }

  protected <R> MetadataResult<R> runWithMetadataContext(Function<MetadataContext, MetadataResult<R>> contextConsumer)
      throws MetadataResolvingException, ConnectionException {
    MetadataContext context = null;
    try {
      MetadataCacheId cacheId = getMetadataCacheId();
      MetadataCache metadataCache = metadataService.getMetadataCache(cacheId.getValue());
      context = withContextClassLoader(classLoader, () -> getMetadataContext(metadataCache));
      MetadataResult<R> result = contextConsumer.apply(context);
      if (result.isSuccess()) {
        metadataService.saveCache(cacheId.getValue(), metadataCache);
      }

      return result;
    } catch (MuleRuntimeException e) {
      // TODO(MULE-13621) this should be deleted once the configuration is created lazily in the getMetadataContext method.
      try {
        throw e.getCause();
      } catch (MetadataResolvingException | ConnectionException cause) {
        throw cause;
      } catch (Throwable t) {
        throw e;
      }
    } finally {
      if (context != null) {
        context.dispose();
      }
    }
  }

  private MetadataCacheId getMetadataCacheId() {
    return cacheIdGenerator.getIdForGlobalMetadata((ComponentConfiguration) this.getAnnotation(ANNOTATION_COMPONENT_CONFIG))
        .map(id -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(id.getParts().toString());
          }
          return id;
        })
        .orElseThrow(() -> new IllegalStateException(
                                                     format("Missing information to obtain the MetadataCache for the component '%s'. "
                                                         +
                                                         "Expected to have the ComponentConfiguration information in the '%s' annotation but none was found.",
                                                            this.getLocation().toString(), ANNOTATION_COMPONENT_CONFIG)));
  }

  private <R> R runWithValueProvidersContext(Function<ExtensionResolvingContext, R> valueProviderFunction) {
    ExtensionResolvingContext context = getResolvingContext();
    R result;
    try {
      result = valueProviderFunction.apply(context);
    } finally {
      context.dispose();
    }
    return result;
  }

  private MetadataContext getMetadataContext(MetadataCache cache)
      throws MetadataResolvingException {
    CoreEvent fakeEvent = null;
    try {
      fakeEvent = getInitialiserEvent(muleContext);

      Optional<ConfigurationInstance> configuration = getConfiguration(fakeEvent);

      if (configuration.isPresent()) {
        ConfigurationProvider configurationProvider = findConfigurationProvider()
            .orElseThrow(() -> new MetadataResolvingException("Failed to create the required configuration for Metadata retrieval",
                                                              INVALID_CONFIGURATION));

        if (configurationProvider instanceof DynamicConfigurationProvider) {
          throw new MetadataResolvingException("Configuration used for Metadata fetch cannot be dynamic", INVALID_CONFIGURATION);
        }
      }

      return new DefaultMetadataContext(() -> configuration, connectionManager,
                                        cache,
                                        typeLoader);
    } finally {
      if (fakeEvent != null) {
        ((BaseEventContext) fakeEvent.getContext()).success();
      }
    }
  }

  private ExtensionResolvingContext getResolvingContext() {
    return new ExtensionResolvingContext(() -> {
      CoreEvent fakeEvent = null;
      try {
        fakeEvent = getInitialiserEvent(muleContext);
        return getConfiguration(fakeEvent);
      } finally {
        if (fakeEvent != null) {
          ((BaseEventContext) fakeEvent.getContext()).success();
        }
      }
    }, connectionManager);
  }

  /**
   * @param event a {@link CoreEvent}
   * @return a configuration instance for the current component with a given {@link CoreEvent}
   */
  protected Optional<ConfigurationInstance> getConfiguration(CoreEvent event) {
    if (!requiresConfig.get()) {
      return empty();
    }

    if (isConfigurationSpecified()) {
      return of(configurationProvider.get())
          .map(provider -> ofNullable(provider.get(event)))
          .orElseThrow(() -> new IllegalModelDefinitionException(format(
                                                                        "Root component '%s' contains a reference to config '%s' but it doesn't exists",
                                                                        getLocation().getRootContainerName(),
                                                                        configurationProvider)));
    }

    return getDefaultConfiguraiton(event);
  }

  private Optional<ConfigurationInstance> getDefaultConfiguraiton(CoreEvent event) {
    return extensionManager.getConfigurationProvider(extensionModel, componentModel)
        .map(provider -> {
          configurationProvider.set(provider);
          return ofNullable(provider.get(event));
        }).orElseGet(() -> extensionManager.getConfiguration(extensionModel, componentModel, event));
  }

  /**
   * Similar to {@link #getConfiguration(CoreEvent)} but only works if the {@link #configurationProvider} is static. Otherwise,
   * returns an empty value.
   */
  protected Optional<ConfigurationInstance> getStaticConfiguration() {
    if (!requiresConfig.get() || (isConfigurationSpecified() && configurationProvider.get().isDynamic())) {
      return empty();
    }

    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent(muleContext);
      return getConfiguration(initialiserEvent);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
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
                                        createStaticMessage(
                                                            format("Root component '%s' defines component '%s' which specifies the expression '%s' as a config-ref. "
                                                                + "Expressions are not allowed as config references",
                                                                   getLocation().getRootContainerName(),
                                                                   hyphenize(componentModel.getName()),
                                                                   configurationProvider)),
                                        this);
    }
  }

  private void setCacheIdGenerator() {
    DslResolvingContext context = DslResolvingContext.getDefault(extensionManager.getExtensions());
    MetadataCacheIdGeneratorFactory.ComponentLocator<ComponentConfiguration> configLocator = location -> componentLocator
        .find(location)
        .map(component -> (ComponentConfiguration) component.getAnnotation(ANNOTATION_COMPONENT_CONFIG));

    this.cacheIdGenerator = cacheIdGeneratorFactory.create(context, configLocator);
  }

  protected abstract ParameterValueResolver getParameterValueResolver();

  /**
   * @return the extension model where the component has been defined.
   */
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  @Override
  public List<ValueProviderModel> getModels(String providerName) {
    return getValueProviderModels(componentModel.getAllParameterModels());
  }
}
