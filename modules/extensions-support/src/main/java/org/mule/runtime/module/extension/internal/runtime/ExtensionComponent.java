/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.api.config.MuleRuntimeFeature.START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.isPagedOperation;
import static org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory.METADATA_CACHE_ID_GENERATOR_KEY;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getValueProviderModels;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.Message;
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
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.RouterInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ScopeInputMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.data.sample.ComponentSampleDataProvider;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.runtime.extension.api.values.ComponentValueProvider;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.internal.MuleMetadataService;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.extension.api.metadata.PropagatedParameterTypeResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.data.sample.DefaultSampleDataProviderMediator;
import org.mule.runtime.module.extension.internal.loader.java.property.TypeLoaderModelProperty;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.DefaultValueProviderMediator;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

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
    ComponentSampleDataProvider, Lifecycle {

  private final static Logger LOGGER = getLogger(ExtensionComponent.class);

  private final ExtensionModel extensionModel;
  private final AtomicReference<ValueResolver<ConfigurationProvider>> configurationProviderResolver = new AtomicReference<>();
  private final ClassTypeLoader typeLoader;
  private final LazyValue<Boolean> requiresConfig = new LazyValue<>(this::computeRequiresConfig);
  private final LazyValue<Boolean> usesDynamicConfiguration = new LazyValue<>(this::computeUsesDynamicConfiguration);
  private final LazyValue<Optional<ConfigurationProvider>> staticallyResolvedConfigurationProvider =
      new LazyValue<>(this::doResolveConfigurationProviderStatically);
  private final LazyValue<Optional<ConfigurationInstance>> staticConfigurationInstance =
      new LazyValue<>(this::doGetStaticConfiguration);

  protected final ExtensionManager extensionManager;
  private DefaultMetadataMediator<T> metadataMediator;
  protected ClassLoader classLoader;
  protected final T componentModel;

  protected CursorProviderFactory cursorProviderFactory;

  protected MuleContext muleContext;

  @Inject
  protected ExpressionManager expressionManager;

  @Inject
  protected ConnectionManagerAdapter connectionManager;

  @Inject
  protected StreamingManager streamingManager;

  @Inject
  protected Optional<MuleMetadataService> metadataService;

  protected ConfigurationComponentLocator componentLocator;

  @Inject
  private ArtifactEncoding artifactEncoding;

  @Inject
  protected ReflectionCache reflectionCache;

  @Inject
  protected ErrorTypeRepository errorTypeRepository;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  private Optional<MetadataCacheIdGeneratorFactory<ComponentAst>> cacheIdGeneratorFactory;

  protected MetadataCacheIdGenerator<ComponentAst> cacheIdGenerator;

  private Function<CoreEvent, Optional<ConfigurationInstance>> configurationResolver;

  protected ExtensionComponent(ExtensionModel extensionModel,
                               T componentModel,
                               ConfigurationProvider configurationProvider,
                               CursorProviderFactory cursorProviderFactory,
                               ExtensionManager extensionManager) {
    this(extensionModel, componentModel,
         configurationProvider != null ? new StaticValueResolver<>(configurationProvider) : null,
         cursorProviderFactory,
         extensionManager);
  }

  protected ExtensionComponent(ExtensionModel extensionModel,
                               T componentModel,
                               ValueResolver<ConfigurationProvider> configurationProviderResolver,
                               CursorProviderFactory cursorProviderFactory,
                               ExtensionManager extensionManager) {
    this.extensionModel = extensionModel;
    this.classLoader = getClassLoader(extensionModel);
    this.componentModel = componentModel;
    this.configurationProviderResolver.set(configurationProviderResolver);
    this.extensionManager = extensionManager;
    this.cursorProviderFactory = cursorProviderFactory;
    this.typeLoader = extensionModel.getModelProperty(TypeLoaderModelProperty.class)
        .map(TypeLoaderModelProperty::getTypeLoader)
        .orElseGet(() -> ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(classLoader));
  }

  /**
   * Makes sure that the operation is valid by invoking {@link #validateOperationConfiguration(ConfigurationProvider)} and then
   * delegates on {@link #doInitialise()} for custom initialisation
   *
   * @throws InitialisationException if a fatal error occurs causing the Mule instance to shutdown
   */
  @Override
  public final void initialise() throws InitialisationException {
    if (metadataService.isPresent()) {
      initializeForFatTooling();
    }

    if (cursorProviderFactory == null) {
      if (isPagedOperation(componentModel)) {
        cursorProviderFactory = (CursorProviderFactory) streamingManager.forObjects().getDefaultCursorProviderFactory();
      } else {
        cursorProviderFactory = streamingManager.forBytes().getDefaultCursorProviderFactory();
      }
    }

    if (!featureFlaggingService.isEnabled(START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER) &&
        classLoader != null && classLoader.getParent() != null &&
        classLoader.getParent() instanceof RegionClassLoader) {
      classLoader = from(classLoader, ((RegionClassLoader) classLoader.getParent()).getOwnerClassLoader().getClassLoader());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Starting extensions with %s", classLoader));
    }

    withContextClassLoader(classLoader, () -> {
      Optional<ValueResolver<ConfigurationProvider>> configProviderResolver = findConfigurationProviderResolver();
      if (configProviderResolver.isPresent()) {
        initialiseIfNeeded(configProviderResolver.get(), muleContext);
      }
      initConfigurationResolver();
      configProviderResolver.flatMap(this::resolveConfigurationProviderStatically)
          .ifPresent(this::validateOperationConfiguration);
      doInitialise();
      return null;
    }, InitialisationException.class, e -> {
      throw new InitialisationException(e, this);
    });
  }

  private void initConfigurationResolver() {
    if (!requiresConfig.get()) {
      configurationResolver = event -> empty();
      return;
    }

    // check for implicit provider
    findConfigurationProviderResolver().ifPresent(configurationProviderResolver::set);

    if (!isConfigurationSpecified()) {
      // obtain implicit instance
      configurationResolver = event -> extensionManager.getConfiguration(extensionModel, componentModel, event);
      return;
    }

    if (doesConfigurationDependOnEvent()) {
      // the config reference is dynamic, the config can be either dynamic or static
      configurationResolver = this::resolveConfigFromDynamicResolver;
      return;
    }

    // the optional should be always present at this point because we already checked dependsOnEvent
    // we can cache the resolution of the provider at this point because the reference is static
    final ConfigurationProvider configurationProvider = getConfigurationProvider().get();

    // the config provider can either be static or dynamic
    configurationResolver = event -> resolveConfigFromProvider(configurationProvider, event);
  }

  /**
   * First resolves the configuration provider (which can be either static or dynamic) and then gets the configuration instance
   * from it.
   * <p>
   * Even though this method would work if the provider resolver is static, it only makes sense to use it when it is dynamic,
   * otherwise it is best to cache the resolution of the provider.
   */
  private Optional<ConfigurationInstance> resolveConfigFromDynamicResolver(CoreEvent event) {
    ConfigurationProvider configurationProvider;
    try {
      configurationProvider = resolveConfigurationProvider(configurationProviderResolver.get(), event);
      validateDynamicOperationConfiguration(configurationProvider);
    } catch (MuleException e) {
      throw new IllegalArgumentException(format("Error resolving configuration for component '%s'",
                                                getLocation().getRootContainerName()),
                                         e);
    }

    // the config provider at this point can be either static or dynamic, it doesn't matter, we need to resolve the
    // config anyway
    return resolveConfigFromProvider(configurationProvider, event);
  }

  /**
   * Resolves a configuration instance from a given provider (which can be either static or dynamic).
   */
  private Optional<ConfigurationInstance> resolveConfigFromProvider(ConfigurationProvider configurationProvider,
                                                                    CoreEvent event) {
    ConfigurationInstance instance = configurationProvider.get(event);
    if (instance == null) {
      throw new IllegalModelDefinitionException(format(
                                                       "Root component '%s' contains a reference to config '%s' but it doesn't exists",
                                                       getLocation().getRootContainerName(),
                                                       configurationProvider));
    }

    return of(instance);
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

  /**
   * Validates that the configuration returned by the {@code configurationProvider} is compatible with the associated
   * {@link ComponentModel}.
   * <p>
   * The difference with {@link #validateOperationConfiguration(ConfigurationProvider)} is that this version will wrap the thrown
   * exception with {@link IllegalArgumentException} which is more suitable for runtime validations.
   *
   * @param configurationProvider
   */
  private void validateDynamicOperationConfiguration(ConfigurationProvider configurationProvider) {
    try {
      validateOperationConfiguration(configurationProvider);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  @Override
  @Inject
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  /**
   * @param event a {@link CoreEvent}
   * @return a configuration instance for the current component with a given {@link CoreEvent}
   */
  protected Optional<ConfigurationInstance> getConfiguration(CoreEvent event) {
    return configurationResolver.apply(event);
  }

  protected boolean requiresConfig() {
    return requiresConfig.get();
  }

  private ConfigurationProvider resolveConfigurationProvider(ValueResolver<ConfigurationProvider> configurationProviderResolver,
                                                             CoreEvent event)
      throws MuleException {
    ValueResolvingContext valueResolvingContext = ValueResolvingContext.builder(event)
        .withExpressionManager(expressionManager)
        .build();

    return configurationProviderResolver.resolve(valueResolvingContext);
  }

  private Optional<ConfigurationProvider> resolveConfigurationProviderStatically() {
    // Since the resolver does not change after initialization, we can cache the result of the resolution.
    // Also note that we can only do so because this is a static resolution (i.e.: if the resolver is dynamic, it will always
    // return the same value: empty).
    return staticallyResolvedConfigurationProvider.get();
  }

  private Optional<ConfigurationProvider> doResolveConfigurationProviderStatically() {
    return resolveConfigurationProviderStatically(configurationProviderResolver.get());
  }

  private Optional<ConfigurationProvider> resolveConfigurationProviderStatically(ValueResolver<ConfigurationProvider> configurationProviderResolver) {
    // If the resolver is dynamic, then it cannot be resolved statically
    if (configurationProviderResolver.isDynamic()) {
      return empty();
    }

    // Since the resolver is not dynamic, we can resolve it using a null Event
    CoreEvent nullEvent = getNullEvent();

    try {
      return of(resolveConfigurationProvider(configurationProviderResolver, nullEvent));
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    } finally {
      if (nullEvent != null) {
        ((BaseEventContext) nullEvent.getContext()).success();
      }
    }
  }

  protected Optional<ConfigurationProvider> getConfigurationProvider() {
    return resolveConfigurationProviderStatically();
  }

  protected boolean usesDynamicConfiguration() {
    return usesDynamicConfiguration.get();
  }

  private boolean computeUsesDynamicConfiguration() {
    // TODO W-11267571: if not being able to resolve the ValueResolver<ConfigurationProvider> at this point, hence
    // treating it as dynamic, ends up causing performance issues.
    return isConfigurationSpecified()
        && (doesConfigurationDependOnEvent() || getConfigurationProvider().map(ConfigurationProvider::isDynamic).orElse(true));
  }

  /**
   * Similar to {@link #getConfiguration(CoreEvent)} but only works if the {@link #configurationProviderResolver} is static.
   * Otherwise, returns an empty value.
   */
  protected Optional<ConfigurationInstance> getStaticConfiguration() {
    return staticConfigurationInstance.get();
  }

  private Optional<ConfigurationInstance> doGetStaticConfiguration() {
    if (!requiresConfig()) {
      return empty();
    }

    if (configurationResolver == null || usesDynamicConfiguration()) {
      return empty();
    }

    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getNullEvent();
      return configurationResolver.apply(initialiserEvent);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  protected CursorProviderFactory getCursorProviderFactory() {
    return cursorProviderFactory;
  }

  private Optional<ValueResolver<ConfigurationProvider>> findConfigurationProviderResolver() {
    if (isConfigurationSpecified()) {
      return of(configurationProviderResolver.get());
    }

    return extensionManager.getConfigurationProvider(extensionModel, componentModel)
        .map(StaticValueResolver::new);
  }

  private boolean isConfigurationSpecified() {
    return configurationProviderResolver.get() != null;
  }

  private boolean doesConfigurationDependOnEvent() {
    return isConfigurationSpecified() && configurationProviderResolver.get().isDynamic();
  }

  private boolean computeRequiresConfig() {
    return ExtensionModelUtils.requiresConfig(extensionModel, componentModel);
  }

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

  @Inject
  public void setComponentLocator(ConfigurationComponentLocator componentLocator) {
    this.componentLocator = componentLocator;
  }

  /////////////////////////////////////////////////////////////////////////////
  // "Fat" Tooling support
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Only to be accessed through {@link #getValueProviderMediator()} as this is a lazy value only used in design time.
   *
   * Purposely not modeled as a {@link LazyValue} to prevent the creation of unnecessary instances when not running in design time
   * or when the underlying component doesn't support the capability in the first place
   */
  private DefaultValueProviderMediator<T> valueProviderMediator;

  /**
   * Only to be accessed through {@link #getSampleDataProviderMediator()} as this is a lazy value only used in design time.
   *
   * Purposely not modeled as a {@link LazyValue} to prevent the creation of unnecessary instances when not running in design time
   * or when the underlying component doesn't support the capability in the first place
   */
  private DefaultSampleDataProviderMediator sampleDataProviderMediator;

  protected void initializeForFatTooling() {
    this.metadataMediator = new DefaultMetadataMediator<>(componentModel, reflectionCache);

    initCacheIdGenerator();
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
                                                                                                             getParameterValueResolver())));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onKeys());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(MetadataKey partialKey) throws MetadataResolvingException {
    try {
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getMetadataKeys(context,
                                                                                                             partialKey)));
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
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public MetadataResult<InputMetadataDescriptor> getInputMetadata(MetadataKey key) throws MetadataResolvingException {
    try {
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader, () -> metadataMediator
                                        .getInputMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public MetadataResult<ScopeInputMetadataDescriptor> getScopeInputMetadata(MetadataKey key,
                                                                            Supplier<MessageMetadataType> scopeInputMessageType)
      throws MetadataResolvingException {
    try {
      // We use a no-op implementation for the PropagatedParameterTypeResolver because this is used only for type resolution
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader, () -> metadataMediator
                                        .getScopeInputMetadata(context, key, scopeInputMessageType,
                                                               PropagatedParameterTypeResolver.NO_OP)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public MetadataResult<RouterInputMetadataDescriptor> getRouterInputMetadata(MetadataKey key,
                                                                              Supplier<MessageMetadataType> routerInputMessageType)
      throws MetadataResolvingException {
    try {
      // We use a no-op implementation for the PropagatedParameterTypeResolver because this is used only for type resolution
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader, () -> metadataMediator
                                        .getRouterInputMetadata(context, key, routerInputMessageType,
                                                                PropagatedParameterTypeResolver.NO_OP)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public MetadataResult<OutputMetadataDescriptor> getOutputMetadata(MetadataKey key) throws MetadataResolvingException {
    try {
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getOutputMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Value> getValues(String parameterName) throws org.mule.runtime.extension.api.values.ValueResolvingException {
    // TODO: MULE-19298 - throws org.mule.sdk.api.values.ValueResolvingException
    try {
      return runWithResolvingContext(context -> withContextClassLoader(classLoader, () -> getValueProviderMediator().getValues(
                                                                                                                               parameterName,
                                                                                                                               getParameterValueResolver(),
                                                                                                                               (CheckedSupplier<Object>) () -> context
                                                                                                                                   .getConnection()
                                                                                                                                   .orElse(null),
                                                                                                                               (CheckedSupplier<Object>) () -> context
                                                                                                                                   .getConfig()
                                                                                                                                   .orElse(null),
                                                                                                                               context
                                                                                                                                   .getConnectionProvider()
                                                                                                                                   .orElse(null))));
    } catch (MuleRuntimeException e) {
      Throwable rootException = getRootException(e);
      if (rootException instanceof org.mule.runtime.extension.api.values.ValueResolvingException) {
        throw (org.mule.runtime.extension.api.values.ValueResolvingException) rootException;
      } else {
        throw new org.mule.runtime.extension.api.values.ValueResolvingException("An unknown error occurred trying to resolve values. "
            + e.getCause().getMessage(),
                                                                                UNKNOWN, e);
      }
    } catch (Exception e) {
      throw new org.mule.runtime.extension.api.values.ValueResolvingException("An unknown error occurred trying to resolve values. "
          + e.getCause().getMessage(),
                                                                              UNKNOWN, e);
    }
  }

  @Override
  public Set<Value> getValues(String parameterName, String targetSelector)
      throws org.mule.runtime.extension.api.values.ValueResolvingException {
    try {
      return runWithResolvingContext(context -> withContextClassLoader(classLoader,
                                                                       () -> getValueProviderMediator()
                                                                           .getValues(parameterName,
                                                                                      getParameterValueResolver(), targetSelector,
                                                                                      (CheckedSupplier<Object>) () -> context
                                                                                          .getConnection().orElse(null),
                                                                                      (CheckedSupplier<Object>) () -> context
                                                                                          .getConfig().orElse(null),
                                                                                      context.getConnectionProvider()
                                                                                          .orElse(null))));
    } catch (MuleRuntimeException e) {
      Throwable rootException = getRootException(e);
      if (rootException instanceof org.mule.runtime.extension.api.values.ValueResolvingException) {
        throw (org.mule.runtime.extension.api.values.ValueResolvingException) rootException;
      } else {
        throw new org.mule.runtime.extension.api.values.ValueResolvingException("An unknown error occurred trying to resolve values. "
            + e.getCause().getMessage(),
                                                                                UNKNOWN, e);
      }
    } catch (Exception e) {
      throw new org.mule.runtime.extension.api.values.ValueResolvingException("An unknown error occurred trying to resolve values. "
          + e.getCause().getMessage(),
                                                                              UNKNOWN, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Message getSampleData() throws SampleDataException {
    try {
      return runWithResolvingContext(context -> withContextClassLoader(classLoader, () -> getSampleDataProviderMediator()
          .getSampleData(getParameterValueResolver(),
                         (CheckedSupplier<Object>) () -> context.getConnection().orElse(null),
                         (CheckedSupplier<Object>) () -> context.getConfig().orElse(null),
                         (CheckedSupplier<ConnectionProvider>) () -> context.getConnectionProvider().orElse(null))));
    } catch (MuleRuntimeException e) {
      throw extractOfType(e, SampleDataException.class).orElseGet(
                                                                  () -> new SampleDataException("An unknown error occurred trying to obtain Sample Data. "
                                                                      + e.getCause().getMessage(),
                                                                                                SampleDataException.UNKNOWN, e));
    } catch (Exception e) {
      throw new SampleDataException("An unknown error occurred trying to obtain Sample Data. " + e.getCause().getMessage(),
                                    SampleDataException.UNKNOWN, e);
    }
  }

  private void initCacheIdGenerator() {
    this.cacheIdGenerator = cacheIdGeneratorFactory
        .map(f -> {
          DslResolvingContext context = DslResolvingContext.getDefault(extensionManager.getExtensions());
          ComponentLocator<ComponentAst> configLocator = location -> componentLocator
              .find(location)
              .map(component -> (ComponentAst) component.getAnnotation(ANNOTATION_COMPONENT_CONFIG));

          return f.create(context, configLocator);
        })
        .orElse(null);
  }

  private ValueProviderMediator getValueProviderMediator() {
    if (valueProviderMediator == null) {
      synchronized (this) {
        if (valueProviderMediator == null) {
          valueProviderMediator =
              new DefaultValueProviderMediator<>(componentModel, () -> muleContext, () -> reflectionCache);
        }
      }
    }

    return valueProviderMediator;
  }

  private DefaultSampleDataProviderMediator getSampleDataProviderMediator() {
    if (sampleDataProviderMediator == null) {
      synchronized (this) {
        if (sampleDataProviderMediator == null) {
          sampleDataProviderMediator = new DefaultSampleDataProviderMediator(
                                                                             extensionModel,
                                                                             componentModel,
                                                                             this,
                                                                             muleContext,
                                                                             artifactEncoding,
                                                                             reflectionCache,
                                                                             streamingManager);
        }
      }
    }

    return sampleDataProviderMediator;
  }

  protected abstract ParameterValueResolver getParameterValueResolver();

  protected <R> MetadataResult<R> runWithMetadataContext(Function<MetadataContext, MetadataResult<R>> contextConsumer)
      throws MetadataResolvingException, ConnectionException {
    return runWithMetadataContext(contextConsumer, empty(), empty());
  }

  protected <R> MetadataResult<R> runWithMetadataContext(Function<MetadataContext, MetadataResult<R>> contextConsumer,
                                                         Optional<ScopeOutputMetadataContext> scopePropagationContext,
                                                         Optional<RouterOutputMetadataContext> routerPropagationContext)
      throws MetadataResolvingException, ConnectionException {
    MetadataContext context = null;
    try {
      MetadataCacheId cacheId = getMetadataCacheId();
      MetadataCache metadataCache = metadataService.get().getMetadataCache(cacheId.getValue());
      context =
          withContextClassLoader(classLoader,
                                 () -> getMetadataContext(metadataCache, scopePropagationContext, routerPropagationContext));
      MetadataResult<R> result = contextConsumer.apply(context);
      if (result.isSuccess()) {
        metadataService.get().saveCache(cacheId.getValue(), metadataCache);
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
    return cacheIdGenerator.getIdForGlobalMetadata((ComponentAst) this.getAnnotation(ANNOTATION_COMPONENT_CONFIG))
        .map(id -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(id.getParts().toString());
          }
          return id;
        })
        .orElseThrow(() -> new IllegalStateException(
                                                     format("Missing information to obtain the MetadataCache for the component '%s'. "
                                                         +
                                                         "Expected to have the ComponentAst information in the '%s' annotation but none was found.",
                                                            this.getLocation().toString(), ANNOTATION_COMPONENT_CONFIG)));
  }

  private <R> R runWithResolvingContext(Function<ExtensionResolvingContext, R> function) {
    ExtensionResolvingContext context = getResolvingContext();
    R result;
    try {
      result = function.apply(context);
    } finally {
      context.dispose();
    }
    return result;
  }

  private MetadataContext getMetadataContext(MetadataCache cache,
                                             Optional<ScopeOutputMetadataContext> scopePropagationContext,
                                             Optional<RouterOutputMetadataContext> routerPropagationContext)
      throws MetadataResolvingException {
    CoreEvent fakeEvent = null;
    try {
      fakeEvent = getNullEvent();

      Optional<ConfigurationInstance> configuration = getConfiguration(fakeEvent);

      if (configuration.isPresent()) {
        ValueResolver<ConfigurationProvider> configurationProviderResolver = findConfigurationProviderResolver()
            .orElseThrow(
                         () -> new MetadataResolvingException("Failed to create the required configuration for Metadata retrieval",
                                                              INVALID_CONFIGURATION));

        Optional<ConfigurationProvider> configurationProvider =
            resolveConfigurationProviderStatically(configurationProviderResolver);
        if (!configurationProvider.isPresent() || configurationProvider.get() instanceof DynamicConfigurationProvider) {
          throw new MetadataResolvingException("Configuration used for Metadata fetch cannot be dynamic", INVALID_CONFIGURATION);
        }
      }
      return new DefaultMetadataContext(() -> configuration, connectionManager, cache, typeLoader, scopePropagationContext,
                                        routerPropagationContext);
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
        fakeEvent = getNullEvent();
        return getConfiguration(fakeEvent);
      } finally {
        if (fakeEvent != null) {
          ((BaseEventContext) fakeEvent.getContext()).success();
        }
      }
    }, connectionManager);
  }

  @Inject
  @Named(METADATA_CACHE_ID_GENERATOR_KEY)
  public void setCacheIdGeneratorFactory(Optional<MetadataCacheIdGeneratorFactory<ComponentAst>> cacheIdGeneratorFactory) {
    this.cacheIdGeneratorFactory = cacheIdGeneratorFactory;
  }

  @Override
  public MetadataResult<OutputMetadataDescriptor> getScopeOutputMetadata(MetadataKey key,
                                                                         ScopeOutputMetadataContext scopeOutputMetadataContext)
      throws MetadataResolvingException {
    try {
      return runWithMetadataContext(context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getOutputMetadata(context, key)),
                                    of(scopeOutputMetadataContext), empty());
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public MetadataResult<OutputMetadataDescriptor> getRouterOutputMetadata(MetadataKey key,
                                                                          RouterOutputMetadataContext routerOutputMetadataContext)
      throws MetadataResolvingException {
    try {
      return runWithMetadataContext(context -> withContextClassLoader(classLoader,
                                                                      () -> metadataMediator.getOutputMetadata(context, key)),
                                    empty(), of(routerOutputMetadataContext));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onComponent());
    }
  }
}
