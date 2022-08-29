/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getPagingResultTransformer;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.createReconnectionInterceptorsChain;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.component.ComponentParameterization;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.result.ValueReturnDelegate;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;


/**
 * This is the default implementation for a {@link ExtensionsClient}, it uses the {@link ExtensionManager} in the
 * {@link MuleContext} to search for the extension that wants to execute the operation from.
 * <p>
 * The concrete execution of the operation is handled by an {@link OperationMessageProcessor} instance.
 * <p>
 * This implementation can only execute extensions that were built using the SDK, Smart Connectors operations can't be executed.
 *
 * @since 4.0
 */
public final class DefaultExtensionsClient implements ExtensionsClient, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultExtensionsClient.class);
  private static final NullComponent NULL_COMPONENT = new NullComponent();
  private static final NullProfilingDataProducer NULL_PROFILING_DATA_PRODUCER = new NullProfilingDataProducer();

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ExtensionConnectionSupplier extensionConnectionSupplier;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private StreamingManager streamingManager;

  @Inject
  private MuleContext muleContext;

  private ExecutorService cacheShutdownExecutor;
  private LoadingCache<OperationKey, ExecutionMediator<OperationModel>> mediatorCache;

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extensionName,
                                                             String operationName,
                                                             Consumer<OperationParameterizer> parameters) {

    DefaultOperationParameterizer parameterizer = new DefaultOperationParameterizer();
    parameters.accept(parameterizer);

    OperationKey key = toKey(extensionName, operationName, parameterizer);

    ComponentParameterization.Builder<OperationModel> paramsBuilder = ComponentParameterization.builder(key.getOperationModel());
    parameterizer.setValuesOn(paramsBuilder);

    ExecutionMediator<OperationModel> mediator = mediatorCache.get(key);

    return withNullEvent(event -> {
      final Map<String, Object> resolvedParams = resolveParameters(paramsBuilder.build());
      OperationModel operationModel = key.getOperationModel();
      CompletableComponentExecutor<OperationModel> executor = getComponentExecutor(operationModel, resolvedParams);
      CursorProviderFactory<Object> cursorProviderFactory = parameterizer.getCursorProviderFactory(streamingManager);

      try {
        ExecutionContextAdapter<OperationModel> context = new DefaultExecutionContext<>(
            key.getExtensionModel(),
            getConfigurationInstance(key.getConfigurationProvider()),
            resolvedParams,
            operationModel,
            event,
            cursorProviderFactory,
            streamingManager,
            NULL_COMPONENT,
            parameterizer.getRetryPolicyTemplate(),
            IMMEDIATE_SCHEDULER,
            empty(),
            muleContext
        );

        CompletableFuture<Result<T, A>> future = new CompletableFuture<>();
        mediator.execute(executor, context, new ExecutorCallback() {

          @Override
          public void complete(Object value) {
            future.complete(asResult(value, operationModel, context, cursorProviderFactory));
          }

          @Override
          public void error(Throwable e) {
            future.completeExceptionally(e);
          }
        });

        return future;
      } finally {
        if (executor != null) {
          stopIfNeeded(executor);
          disposeIfNeeded(executor, LOGGER);
        }
      }
    });
  }

  private <T, A> Result<T, A> asResult(Object value,
                                       OperationModel operationModel,
                                       ExecutionContextAdapter<OperationModel> context,
                                       CursorProviderFactory cursorProviderFactory) {

    ValueReturnDelegate delegate = new ValueReturnDelegate(operationModel, cursorProviderFactory, muleContext);
    CoreEvent resultEvent = delegate.asReturnValue(value, context);
    return (Result<T, A>) Result.builder(resultEvent.getMessage()).build();
  }

  private CompletableComponentExecutor<OperationModel> getComponentExecutor(OperationModel operationModel,
                                                                            Map<String, Object> params) {
    Map<String, Object> initParams = new HashMap<>();
    operationModel.getAllParameterModels().stream()
        .filter(p -> p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent())
        .forEach(p -> {
          String paramName = p.getName();
          if (params.containsKey(paramName)) {
            initParams.put(paramName, params.get(paramName));
          }
        });
    
    CompletableComponentExecutor<OperationModel> executor = getOperationExecutorFactory(operationModel).createExecutor(operationModel, initParams);
    try {
      initialiseIfNeeded(executor, true, muleContext);
      startIfNeeded(executor);

      return executor;
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Map<String, Object> resolveParameters(ComponentParameterization<OperationModel> parameters) {
    try {
      ResolverSet resolverSet = getResolverSetFromComponentParameterization(
          parameters,
          muleContext,
          true,
          reflectionCache,
          expressionManager,
          "");

      withNullEvent(event -> {
        try (ValueResolvingContext.builder(event).build()) {

        }
      })

      resolverSet.resolve(ValueResolvingContext.).

      return (Map) resolverSet.getResolvers();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(e.getMessage()), e);
    }
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(Optional<ConfigurationProvider> configurationProvider) {
    return configurationProvider.map(config -> withNullEvent(config::get));
  }

  private OperationKey toKey(String extensionName,
                             String operationName,
                             DefaultOperationParameterizer parameterizer) {

    final ExtensionModel extensionModel = findExtension(extensionName);
    final Optional<ConfigurationProvider> configurationProvider = findConfiguration(extensionModel, parameterizer);
    final OperationModel operationModel = findOperationModel(extensionModel, configurationProvider, operationName);

    return new OperationKey(extensionModel, configurationProvider, operationModel);
  }

  private LoadingCache<OperationKey, ExecutionMediator<OperationModel>> createMediatorCache() {
    return Caffeine.newBuilder()
        // Since the removal listener runs asynchronously, force waiting for all cleanup tasks to be complete before proceeding
        // (and finalizing) the context disposal.
        // Ref: https://github.com/ben-manes/caffeine/issues/104#issuecomment-238068997
        .executor(cacheShutdownExecutor)
        .expireAfterAccess(5, MINUTES)
        .removalListener((key, mediator, cause) -> disposeMediator((OperationKey) key, (ExecutionMediator<OperationModel>) mediator))
        .build(this::createExecutionMediator);
  }

  private ExecutionMediator<OperationModel> createExecutionMediator(OperationKey key) {
    final ExtensionModel extensionModel = key.getExtensionModel();
    final OperationModel operationModel = key.getOperationModel();
    ExecutionMediator<OperationModel> mediator = new DefaultExecutionMediator<>(
        extensionModel,
        operationModel,
        createReconnectionInterceptorsChain(extensionModel,
            operationModel,
            extensionConnectionSupplier,
            reflectionCache),
        errorTypeRepository,
        muleContext.getExecutionClassLoader(),
        getPagingResultTransformer(operationModel, extensionConnectionSupplier, supportsOAuth(extensionModel)).orElse(null),
        NULL_PROFILING_DATA_PRODUCER);

    try {
      initialiseIfNeeded(mediator, true, muleContext);
      startIfNeeded(mediator);

      return mediator;
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create mediator for operation " + key), e);
    }
  }

  private void disposeMediator(OperationKey identifier, ExecutionMediator<OperationModel> mediator) {
    try {
      stopIfNeeded(mediator);
    } catch (Exception e) {
      LOGGER.error("Exception found trying to stop ExtensionClient mediator for operation " + identifier);
    } finally {
      disposeIfNeeded(mediator, LOGGER);
    }
  }

  private Optional<ConfigurationProvider> findConfiguration(ExtensionModel extensionModel,
                                                            DefaultOperationParameterizer parameterizer) {
    final String configRef = parameterizer.getConfigRef();
    if (configRef != null) {
      return of(extensionManager.getConfigurationProvider(configRef)
          .map(configurationProvider -> {
            if (configurationProvider.getExtensionModel() != extensionModel) {
              throw new IllegalArgumentException();
            }
            return configurationProvider;
          })
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No configuration [" + configRef + "] found"))));
    }

    return empty();
  }

  private OperationModel findOperationModel(ExtensionModel extensionModel,
                                            Optional<ConfigurationProvider> configurationProvider,
                                            String operationName) {
    Optional<OperationModel> operation = extensionModel.getOperationModel(operationName);
    if (operation.isPresent()) {
      return operation.get();
    }

    if (configurationProvider.isPresent()) {
      ConfigurationModel configurationModel = configurationProvider.get().getConfigurationModel();
      return configurationModel.getOperationModel(operationName).orElseThrow(
          () -> noSuchOperationException(operationName));
    } else {
      throw new IllegalArgumentException("Operation '" + operationName + "' not found at the extension level");
    }
  }

  private MuleRuntimeException noSuchOperationException(String operationName) {
    throw new MuleRuntimeException(createStaticMessage(format("No Operation [%s] Found", operationName)));
  }
  private OperationModel findOperationModel(ExtensionModel extensionModel, String operationName) {
    for (ConfigurationModel configurationModel : extensionModel.getConfigurationModels()) {
      Optional<OperationModel> operation = configurationModel.getOperationModel(operationName);
      if (operation.isPresent()) {
        return operation.get();
      }
    }

    return extensionModel.getOperationModel(operationName).orElseThrow(() -> noSuchOperationException(operationName));
  }

  private ExtensionModel findExtension(String extensionName) {
    return extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Extension [" + extensionName + "] Found")));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extensionName,
                                                             String operationName,
                                                             OperationParameters parameters) {

    final ExtensionModel extensionModel = findExtension(extensionName);
    final OperationModel operationModel = findOperationModel(extensionModel, operationName);

    return executeAsync(
        extensionName,
        operationName,
        parameterizer -> {
          parameters.get().forEach((key, value) -> {
            if (!CONFIG_ATTRIBUTE_NAME.equals(key)) {
              parameterizer.withParameter(key, value);
            }
          });
          parameters.getConfigName().ifPresent(parameterizer::withConfigRef);
          configureLegacyRepeatableStreaming(parameterizer, operationModel);
        }
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters params)
      throws MuleException {

    try {
      return (Result<T, A>) executeAsync(extension, operation, params).get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof MuleException) {
        throw (MuleException) cause;
      } else {
        throw new DefaultMuleException(cause);
      }
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new DefaultMuleException(e);
    }
  }

  private void configureLegacyRepeatableStreaming(OperationParameterizer parameterizer, OperationModel operationModel) {
    if (operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
      setDefaultRepeatableIterables(parameterizer);
    } else if (operationModel.supportsStreaming()) {
      setDefaultRepeatableStreaming(parameterizer);
    }
  }

  private void setDefaultRepeatableStreaming(OperationParameterizer parameterizer) {
    parameterizer.withDefaultRepeatableStreaming();
  }

  private void setDefaultRepeatableIterables(OperationParameterizer parameterizer) {
    parameterizer.withDefaultRepeatableIterables();
  }

  @Override
  public void initialise() throws InitialisationException {
    cacheShutdownExecutor = new ShutdownExecutor();
    mediatorCache = createMediatorCache();
  }

  @Override
  public void dispose() {
    if (mediatorCache != null) {
      mediatorCache.invalidateAll();
    }

    if (cacheShutdownExecutor != null) {
      cacheShutdownExecutor.shutdown();
      shutdownAndAwaitTermination(cacheShutdownExecutor, 5, SECONDS);
    }
  }

  private static class OperationKey {

    private final ExtensionModel extensionModel;
    private final Optional<ConfigurationProvider> configurationProvider;
    private final OperationModel operationModel;
    private final String configName;

    public OperationKey(ExtensionModel extensionModel,
                        Optional<ConfigurationProvider> configurationProvider,
                        OperationModel operationModel) {
      this.extensionModel = extensionModel;
      this.configurationProvider = configurationProvider;
      this.operationModel = operationModel;
      configName = configurationProvider.map(ConfigurationProvider::getName).orElse(null);
    }

    public ExtensionModel getExtensionModel() {
      return extensionModel;
    }

    public Optional<ConfigurationProvider> getConfigurationProvider() {
      return configurationProvider;
    }

    public OperationModel getOperationModel() {
      return operationModel;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof OperationKey) {
        OperationKey that = (OperationKey) o;
        return Objects.equals(extensionModel.getName(), that.extensionModel.getName()) &&
            Objects.equals(operationModel.getName(), that.operationModel.getName()) &&
            Objects.equals(configName, that.configName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hash(extensionModel.getName(), operationModel.getName(), configName);
    }

    @Override
    public String toString() {
      return format("[Extension: %s; Operation: %s, ConfigName: %s",
          extensionModel.getName(), operationModel.getName(), configName);
    }
  }

  private static class NullProfilingDataProducer implements ProfilingDataProducer<ComponentThreadingProfilingEventContext, CoreEvent> {

    private NullProfilingDataProducer() {
    }

    @Override
    public void triggerProfilingEvent(ComponentThreadingProfilingEventContext profilerEventContext) {

    }

    @Override
    public void triggerProfilingEvent(CoreEvent sourceData, Function<CoreEvent, ComponentThreadingProfilingEventContext> transformation) {

    }
  }

  private static class NullComponent extends AbstractComponent {
  }
}
