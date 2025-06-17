/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static org.mule.runtime.api.config.MuleRuntimeFeature.UNSUPPORTED_EXTENSIONS_CLIENT_RUN_ASYNC;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.isPagedOperation;
import static org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findOperation;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findSource;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.FeatureFlaggingService;
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
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.client.source.SourceHandler;
import org.mule.runtime.extension.api.client.source.SourceParameterizer;
import org.mule.runtime.extension.api.client.source.SourceResultHandler;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.internal.client.ComplexParameter;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.client.operation.DefaultOperationParameterizer;
import org.mule.runtime.module.extension.internal.runtime.client.operation.EventedOperationsParameterDecorator;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationKey;
import org.mule.runtime.module.extension.internal.runtime.client.source.DefaultSourceHandler;
import org.mule.runtime.module.extension.internal.runtime.client.source.SourceClient;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.inject.Inject;

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
  private FeatureFlaggingService featureFlaggingService;

  @Inject
  private MuleContext muleContext;

  @Inject
  private MuleConfiguration muleConfiguration;

  @Inject
  private ArtifactEncoding artifactEncoding;

  private ExecutorService cacheShutdownExecutor;
  private LoadingCache<OperationKey, OperationClient> operationClientCache;
  private final Set<SourceClient> sourceClients = new HashSet<>();

  @Override
  public <T, A> CompletableFuture<Result<T, A>> execute(String extensionName,
                                                        String operationName,
                                                        Consumer<OperationParameterizer> parameters) {

    ExtensionModel extensionModel = findExtension(extensionName);
    OperationModel operationModel = findOperationModel(extensionModel, operationName);

    return doExecute(extensionModel, operationModel, parameters);
  }

  private <T, A> CompletableFuture<Result<T, A>> doExecute(ExtensionModel extensionModel,
                                                           OperationModel operationModel,
                                                           Consumer<OperationParameterizer> parameters) {
    DefaultOperationParameterizer parameterizer = new DefaultOperationParameterizer();
    parameters.accept(parameterizer);

    OperationKey key = new OperationKey(extensionModel, operationModel, parameterizer.getConfigRef());

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
      LOGGER.atError().setCause(e).log("Exception found stopping source client: " + e.getMessage());
    } finally {
      sourceClient.dispose();
    }
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
                                  muleContext,
                                  muleConfiguration,
                                  artifactEncoding,
                                  notificationDispatcher);

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
      LOGGER.atError().setCause(e).log("Exception found trying to stop operation client for operation {}", identifier);
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
    // Prevent using code that relies on org.mule.runtime.extension.internal.client.ComplexParameter
    if (featureFlaggingService.isEnabled(UNSUPPORTED_EXTENSIONS_CLIENT_RUN_ASYNC)) {
      throw new UnsupportedOperationException("executeAsync not supported. Ref MuleRuntimeFeature#UNSUPPORTED_EXTENSIONS_CLIENT_RUN_ASYNC");
    }

    final ExtensionModel extensionModel = findExtension(extensionName);
    final OperationModel operationModel = findOperationModel(extensionModel, operationName);

    return doExecute(
                     extensionModel,
                     operationModel,
                     parameterizer -> {
                       setContextEvent(parameterizer, parameters);
                       parameters.getConfigName().ifPresent(parameterizer::withConfigRef);
                       resolveLegacyParameters(parameterizer, parameters);
                       configureLegacyRepeatableStreaming(parameterizer, operationModel);
                     });
  }

  private void resolveLegacyParameters(OperationParameterizer parameterizer, OperationParameters legacyParameters) {
    resolveLegacyParameters(legacyParameters.get(), parameterizer::withParameter);
  }

  private void resolveLegacyParameters(Map<String, Object> parameters,
                                       BiConsumer<String, Object> resolvedValueConsumer) {
    parameters.forEach((paramName, value) -> {
      if (CONFIG_ATTRIBUTE_NAME.equals(paramName)) {
        return;
      }

      if (value instanceof ComplexParameter complex) {
        DefaultObjectBuilder<?> builder = new DefaultObjectBuilder<>(complex.getType(), reflectionCache);
        resolveLegacyParameters(complex.getParameters(), (propertyName, propertyValue) -> builder
            .addPropertyResolver(propertyName, new StaticValueResolver<>(propertyValue)));

        value = withNullEvent(event -> {
          try (ValueResolvingContext ctx = ValueResolvingContext.builder(event).build()) {
            return builder.build(ctx);
          } catch (MuleException e) {
            throw new MuleRuntimeException(createStaticMessage(format("Could not construct parameter [%s]", paramName)), e);
          }
        });
      }

      resolvedValueConsumer.accept(paramName, value);
    });
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
      if (cause instanceof MuleException muleException) {
        throw muleException;
      } else {
        throw new DefaultMuleException(cause);
      }
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new DefaultMuleException(e);
    }
  }

  private void configureLegacyRepeatableStreaming(OperationParameterizer parameterizer, OperationModel operationModel) {
    if (isPagedOperation(operationModel)) {
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

  private void setContextEvent(OperationParameterizer parameterizer, OperationParameters parameters) {
    if (parameters instanceof EventedOperationsParameterDecorator eop) {
      parameterizer.inTheContextOf(eop.getContextEvent());
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    cacheShutdownExecutor = new ShutdownExecutor();
    operationClientCache = createOperationClientCache();
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

    if (cacheShutdownExecutor != null) {
      cacheShutdownExecutor.shutdown();
      shutdownAndAwaitTermination(cacheShutdownExecutor, 5, SECONDS);
    }
  }

}
