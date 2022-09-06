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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.runtime.client.NullComponent.NULL_COMPONENT;
import static org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient.from;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;
import static org.slf4j.LoggerFactory.getLogger;

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
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.internal.client.ComplexParameter;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.client.operation.DefaultOperationParameterizer;
import org.mule.runtime.module.extension.internal.runtime.client.operation.EventedOperationsParameterDecorator;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationClient;
import org.mule.runtime.module.extension.internal.runtime.client.operation.OperationKey;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
  private LoadingCache<OperationKey, OperationClient> clientCache;

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extensionName,
                                                             String operationName,
                                                             Consumer<OperationParameterizer> parameters) {

    DefaultOperationParameterizer parameterizer = new DefaultOperationParameterizer();
    parameters.accept(parameterizer);

    OperationKey key = toKey(extensionName, operationName, parameterizer);

    ComponentParameterization.Builder<OperationModel> paramsBuilder = ComponentParameterization.builder(key.getOperationModel());
    parameterizer.setValuesOn(paramsBuilder);

    OperationClient client = clientCache.get(key);

    boolean shouldCompleteEvent = false;
    CoreEvent contextEvent = parameterizer.getContextEvent().orElse(null);
    if (contextEvent == null) {
      contextEvent = getNullEvent(muleContext);
      shouldCompleteEvent = true;
    }

    final Map<String, Object> resolvedParams = resolveLegacyParameters(paramsBuilder.build(), contextEvent);
    OperationModel operationModel = key.getOperationModel();
    CursorProviderFactory<Object> cursorProviderFactory = parameterizer.getCursorProviderFactory(streamingManager);

    ExecutionContextAdapter<OperationModel> context = new DefaultExecutionContext<>(
                                                                                    key.getExtensionModel(),
                                                                                    getConfigurationInstance(
                                                                                                             key.getConfigurationProvider(),
                                                                                                             contextEvent),
                                                                                    resolvedParams,
                                                                                    operationModel,
                                                                                    contextEvent,
                                                                                    cursorProviderFactory,
                                                                                    streamingManager,
                                                                                    NULL_COMPONENT,
                                                                                    parameterizer.getRetryPolicyTemplate(),
                                                                                    IMMEDIATE_SCHEDULER,
                                                                                    empty(),
                                                                                    muleContext);

    return client.execute(context, shouldCompleteEvent);
  }

  private Map<String, Object> resolveLegacyParameters(ComponentParameterization<OperationModel> parameters, CoreEvent event) {
    try {
      ResolverSet resolverSet = getResolverSetFromComponentParameterization(
                                                                            parameters,
                                                                            muleContext,
                                                                            true,
                                                                            reflectionCache,
                                                                            expressionManager,
                                                                            "");

      try (ValueResolvingContext ctx = ValueResolvingContext.builder(event).build()) {
        return resolverSet.resolve(ctx).asMap();
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(e.getMessage()), e);
    }
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(Optional<ConfigurationProvider> configurationProvider,
                                                                   CoreEvent contextEvent) {
    return configurationProvider.map(config -> config.get(contextEvent));
  }

  private OperationKey toKey(String extensionName,
                             String operationName,
                             DefaultOperationParameterizer parameterizer) {

    final ExtensionModel extensionModel = findExtension(extensionName);
    final Optional<ConfigurationProvider> configurationProvider = findConfiguration(extensionModel, parameterizer);
    final OperationModel operationModel = findOperationModel(extensionModel, configurationProvider, operationName);

    return new OperationKey(extensionModel, configurationProvider, operationModel);
  }

  private LoadingCache<OperationKey, OperationClient> createClientCache() {
    return Caffeine.newBuilder()
        // Since the removal listener runs asynchronously, force waiting for all cleanup tasks to be complete before proceeding
        // (and finalizing) the context disposal.
        // Ref: https://github.com/ben-manes/caffeine/issues/104#issuecomment-238068997
        .executor(cacheShutdownExecutor)
        .expireAfterAccess(5, MINUTES)
        .removalListener((key, mediator, cause) -> disposeClient((OperationKey) key, (OperationClient) mediator))
        .build(this::createOperationClient);
  }

  private OperationClient createOperationClient(OperationKey key) {
    OperationClient client = from(
                                  key,
                                  extensionManager,
                                  expressionManager,
                                  extensionConnectionSupplier,
                                  errorTypeRepository,
                                  reflectionCache,
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
                          setContextEvent(parameterizer, parameters);
                          parameters.getConfigName().ifPresent(parameterizer::withConfigRef);
                          resolveLegacyParameters(parameterizer, parameters);
                          configureLegacyRepeatableStreaming(parameterizer, operationModel);
                        });
  }

  protected void resolveLegacyParameters(OperationParameterizer parameterizer, OperationParameters legacyParameters) {
    resolveLegacyParameters(legacyParameters.get(), parameterizer::withParameter);
  }

  private void resolveLegacyParameters(Map<String, Object> parameters,
                                       BiConsumer<String, Object> resolvedValueConsumer) {
    parameters.forEach((paramName, value) -> {
      if (CONFIG_ATTRIBUTE_NAME.equals(paramName)) {
        return;
      }

      if (value instanceof ComplexParameter) {
        ComplexParameter complex = (ComplexParameter) value;
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

  private void setContextEvent(OperationParameterizer parameterizer, OperationParameters parameters) {
    if (parameters instanceof EventedOperationsParameterDecorator) {
      parameterizer.inTheContextOf(((EventedOperationsParameterDecorator) parameters).getContextEvent());
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    cacheShutdownExecutor = new ShutdownExecutor();
    clientCache = createClientCache();
  }

  @Override
  public void dispose() {
    if (clientCache != null) {
      clientCache.invalidateAll();
    }

    if (cacheShutdownExecutor != null) {
      cacheShutdownExecutor.shutdown();
      shutdownAndAwaitTermination(cacheShutdownExecutor, 5, SECONDS);
    }
  }

}
