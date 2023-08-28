/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findOperation;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findSource;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.client.source.SourceHandler;
import org.mule.runtime.extension.api.client.source.SourceParameterizer;
import org.mule.runtime.extension.api.client.source.SourceResultHandler;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.client.operation.DefaultOperationParameterizer;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationKey;
import org.mule.runtime.module.extension.internal.runtime.client.source.DefaultSourceHandler;
import org.mule.runtime.module.extension.internal.runtime.client.source.SourceClient;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;


/**
 * This is the default implementation for a {@link ExtensionsClient}, it uses the {@link ExtensionManager} in the
 * {@link MuleContext} to search for the extension that wants to execute the operation from.
 * <p>
 * The concrete execution of the operation is handled by an {@link OperationClient} instance.
 * <p>
 * This implementation can only execute extensions that were built using the SDK, Smart Connectors operations can't be executed.
 *
 * @since 4.0
 */
public final class DefaultExtensionsClient implements ExtensionsClient, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultExtensionsClient.class);

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private ExtensionConnectionSupplier extensionConnectionSupplier;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private StreamingManager streamingManager;

  @Inject
  private NotificationDispatcher notificationDispatcher;

  @Inject
  private ComponentTracerFactory<CoreEvent> componentTracerFactory;

  @Inject
  private MuleContext muleContext;

  private ExecutorService cacheShutdownExecutor;
  private LoadingCache<OperationKey, OperationClient> operationClientCache;
  private Set<SourceClient> sourceClients = new HashSet<>();

  // TODO: remove once performance degradation is fixed.
  @Inject
  private Registry registry;
  @Inject
  private PolicyManager policyManager;
  private Cache<String, OperationMessageProcessor> operationMessageProcessorCache;
  private ExtensionsClientProcessorsHelper extensionsClientProcessorsHelper;

  @Override
  public <T, A> CompletableFuture<Result<T, A>> execute(String extensionName,
                                                        String operationName,
                                                        Consumer<OperationParameterizer> parameters) {

    DefaultOperationParameterizer parameterizer = new DefaultOperationParameterizer();
    parameters.accept(parameterizer);

    OperationKey key = toOperationKey(extensionName, operationName, parameterizer);

    return operationClientCache.get(key).execute(key, parameterizer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> SourceHandler createSource(String extensionName,
                                           String sourceName,
                                           Consumer<SourceResultHandler<T, A>> handler,
                                           Consumer<SourceParameterizer> parameters) {
    final ExtensionModel extensionModel = findExtension(extensionName);
    final SourceModel sourceModel = findSourceModel(extensionModel, sourceName);

    SourceClient<T, A> sourceClient = new SourceClient<>(extensionModel,
                                                         sourceModel,
                                                         parameters,
                                                         handler,
                                                         extensionManager,
                                                         streamingManager,
                                                         errorTypeLocator,
                                                         reflectionCache,
                                                         expressionManager,
                                                         notificationDispatcher,
                                                         muleContext.getTransactionFactoryManager(),
                                                         muleContext);

    try {
      initialiseIfNeeded(sourceClient, true, muleContext);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception initializing source:" + e.getMessage()), e);
    }

    return new DefaultSourceHandler(sourceClient, () -> discard(sourceClient));
  }

  private void discard(SourceClient sourceClient) {
    synchronized (sourceClients) {
      try {
        stopAndDispose(sourceClient);
      } finally {
        sourceClients.remove(sourceClient);
      }
    }
  }

  private void stopAndDispose(SourceClient sourceClient) {
    try {
      sourceClient.stop();
    } catch (Exception e) {
      LOGGER.error("Exception found stopping source client: " + e.getMessage(), e);
    } finally {
      sourceClient.dispose();
    }
  }

  private OperationKey toOperationKey(String extensionName,
                                      String operationName,
                                      DefaultOperationParameterizer parameterizer) {
    return new OperationKey(extensionName,
                            parameterizer.getConfigRef(),
                            operationName,
                            this::findExtension,
                            this::findOperationModel,
                            extensionManager);
  }

  private LoadingCache<OperationKey, OperationClient> createOperationClientCache() {
    return Caffeine.newBuilder()
        // Since the removal listener runs asynchronously, force waiting for all cleanup tasks to be complete before proceeding
        // (and finalizing) the context disposal.
        // Ref: https://github.com/ben-manes/caffeine/issues/104#issuecomment-238068997
        .executor(cacheShutdownExecutor)
        .expireAfterAccess(5, MINUTES)
        .removalListener((key, client, cause) -> disposeClient((OperationKey) key, (OperationClient) client))
        .build(this::createOperationClient);
  }

  private Cache<String, OperationMessageProcessor> createOperationMessageProcessorCache() {
    return Caffeine.newBuilder()
        .maximumSize(100)
        // Since the removal listener runs asynchronously, force waiting for all cleanup tasks to be complete before proceeding
        // (and finalizing) the context disposal.
        // Ref: https://github.com/ben-manes/caffeine/issues/104#issuecomment-238068997
        .executor(cacheShutdownExecutor)
        .expireAfterAccess(10, MINUTES)
        .<String, OperationMessageProcessor>removalListener((key, operationMessageProcessor,
                                                             removalCause) -> disposeProcessor(operationMessageProcessor))
        .build();
  }

  private static void disposeProcessor(OperationMessageProcessor processor) {
    if (processor == null) {
      return;
    }
    try {
      processor.stop();
      processor.dispose();
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Error while disposing the executing operation"), e);
    }
  }

  private OperationClient createOperationClient(OperationKey key) {
    OperationClient client = from(
                                  key,
                                  extensionManager,
                                  expressionManager,
                                  extensionConnectionSupplier,
                                  errorTypeRepository,
                                  streamingManager,
                                  reflectionCache,
                                  componentTracerFactory,
                                  muleContext);

    try {
      initialiseIfNeeded(client);
      startIfNeeded(client);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception found creating operation client: " + e.getMessage()), e);
    }

    return client;
  }

  private void disposeClient(OperationKey identifier, OperationClient client) {
    try {
      stopIfNeeded(client);
    } catch (Exception e) {
      LOGGER.error("Exception found trying to stop operation client for operation " + identifier);
    } finally {
      disposeIfNeeded(client, LOGGER);
    }
  }

  private OperationModel findOperationModel(ExtensionModel extensionModel, String operationName) {
    return findOperation(extensionModel, operationName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("No Operation [%s] Found", operationName))));
  }

  private SourceModel findSourceModel(ExtensionModel extensionModel, String sourceName) {
    return findSource(extensionModel, sourceName)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("No Source [%s] Found", sourceName))));
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
    // TODO: delegate to the new API once the performance degradation is fixed
    OperationMessageProcessor processor =
        extensionsClientProcessorsHelper.getOperationMessageProcessor(extensionName, operationName, parameters);
    final CoreEvent eventFromParams = extensionsClientProcessorsHelper.getEvent(parameters);
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
        .toFuture();
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

  @Override
  public void initialise() throws InitialisationException {
    cacheShutdownExecutor = new ShutdownExecutor();
    operationClientCache = createOperationClientCache();
    operationMessageProcessorCache = createOperationMessageProcessorCache();
    extensionsClientProcessorsHelper =
        new ExtensionsClientProcessorsHelper(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                             operationMessageProcessorCache);
  }

  @Override
  public void dispose() {
    synchronized (sourceClients) {
      sourceClients.forEach(this::stopAndDispose);
      sourceClients.clear();
    }

    if (operationClientCache != null) {
      operationClientCache.invalidateAll();
    }

    if (operationMessageProcessorCache != null) {
      operationMessageProcessorCache.invalidateAll();
    }

    if (cacheShutdownExecutor != null) {
      cacheShutdownExecutor.shutdown();
      shutdownAndAwaitTermination(cacheShutdownExecutor, 5, SECONDS);
    }
  }

}
