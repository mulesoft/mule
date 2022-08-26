/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.privileged.util.EventUtils.withNullEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getPagingResultTransformer;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.createReconnectionInterceptorsChain;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.util.EventUtils;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
  private static final NullProfilingDataProducer NULL_PROFILING_PRODUCER = new NullProfilingDataProducer();

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ExtensionConnectionSupplier extensionConnectionSupplier;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private MuleContext muleContext;

  private ExecutorService cacheShutdownExecutor;
  private LoadingCache<OperationKey, ExecutionMediator<OperationModel>> mediatorCache;

//  @Inject
//  private ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory;

//  private ExtensionsClientProcessorsStrategy extensionsClientProcessorsStrategy;

//  /**
//   * This constructor enables the {@link DefaultExtensionsClient} to be aware of the current execution {@link CoreEvent} and
//   * enables to perform the dynamic operation execution with the same event that the SDK operation using the
//   * {@link ExtensionsClient} receives.
//   *
//   * @param event                                     the current execution event.
//   * @param extensionsClientProcessorsStrategyFactory the factory used to get the appropriate operation message processor strategy
//   */
//  public DefaultExtensionsClient(CoreEvent event,
//                                 ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory) {
//    this.event = event;
//    this.extensionsClientProcessorsStrategyFactory = extensionsClientProcessorsStrategyFactory;
//  }

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extensionName,
                                                             String operationName,
                                                             Consumer<OperationParameterizer> parameters) {

    DefaultOperationParameterizer parameterizer = new DefaultOperationParameterizer();
    parameters.accept(parameterizer);

    OperationKey key = toKey(extensionName, operationName, parameterizer);
    ExecutionMediator<OperationModel> mediator = mediatorCache.get(key);

    new DefaultExecutionContext<>(key.getExtensionModel(),
        getConfigurationInstance(key.getConfigurationProvider()),
        
        )

    mediator.execute();
    return null;
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(Optional<ConfigurationProvider> configurationProvider) {
    return configurationProvider.map(p -> withNullEvent(event -> p.get(event)));
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
    return new DefaultExecutionMediator<>(extensionModel,
        operationModel,
        createReconnectionInterceptorsChain(extensionModel,
            operationModel,
            extensionConnectionSupplier,
            reflectionCache),
        errorTypeRepository,
        muleContext.getExecutionClassLoader(),
        getPagingResultTransformer(extensionModel, operationModel, extensionConnectionSupplier, supportsOAuth(extensionModel)).orElse(null),
        NULL_PROFILING_PRODUCER);
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
          .orElseThrow(() -> new IllegalArgumentException("Configuration '" + configRef + "' doesn't exist")));
    }

    return empty();
  }

  private OperationModel findOperationModel(ExtensionModel extensionModel,
                                            Optional<ConfigurationProvider> configurationProvider,
                                            String operationName) {

    return configurationProvider.flatMap(cp -> cp.getConfigurationModel().getOperationModel(operationName))
        .orElseGet(() -> extensionModel.getOperationModel(operationName)
            .orElseThrow(() -> new IllegalArgumentException("Operation '" + "' not found")));
  }

  private ExtensionModel findExtension(String extensionName) {
    return extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No Extension [" + extensionName + "] Found")));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension, String operation, OperationParameters parameters) {
    OperationMessageProcessor processor =
        extensionsClientProcessorsStrategy.getOperationMessageProcessor(extension, operation, parameters);
    final CoreEvent eventFromParams = extensionsClientProcessorsStrategy.getEvent(parameters);
    return just(eventFromParams)
        .transform(processor)
        .map(event -> Result.<T, A>builder(event.getMessage()).build())
        .onErrorMap(t -> {
          Throwable unwrapped = unwrap(t);
          if (unwrapped instanceof MessagingException) {
            return unwrapped;
          } else {
            return new MessagingException(eventFromParams, unwrapped, processor);
          }
        })
        .doAfterTerminate(() -> extensionsClientProcessorsStrategy.disposeProcessor(processor))
        .toFuture();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters params)
      throws MuleException {
    OperationMessageProcessor processor =
        extensionsClientProcessorsStrategy.getOperationMessageProcessor(extension, operation, params);
    final CoreEvent eventFromParams = extensionsClientProcessorsStrategy.getEvent(params);
    try {
      CoreEvent process = processor.process(eventFromParams);
      return Result.<T, A>builder(process.getMessage()).build();
    } catch (Exception e) {
      Throwable unwrapped = unwrap(e);
      if (unwrapped instanceof MessagingException) {
        throw (MessagingException) unwrapped;
      } else {
        throw new MessagingException(eventFromParams, unwrapped, processor);
      }
    } finally {
      extensionsClientProcessorsStrategy.disposeProcessor(processor);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    cacheShutdownExecutor = new ShutdownExecutor();
    mediatorCache = createMediatorCache();
  }

  @Override
  public void dispose() {
    if (cacheShutdownExecutor != null) {
      cacheShutdownExecutor.shutdown();
      shutdownAndAwaitTermination(cacheShutdownExecutor, 5, SECONDS);
    }

    if (mediatorCache != null) {
      mediatorCache.invalidateAll();
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

    @Override
    public void triggerProfilingEvent(ComponentThreadingProfilingEventContext profilerEventContext) {

    }

    @Override
    public void triggerProfilingEvent(CoreEvent sourceData, Function<CoreEvent, ComponentThreadingProfilingEventContext> transformation) {

    }
  }
}
