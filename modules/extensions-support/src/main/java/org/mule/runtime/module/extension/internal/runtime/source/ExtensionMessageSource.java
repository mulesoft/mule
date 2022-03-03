/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static com.google.common.collect.ImmutableMap.copyOf;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Function.identity;
import static org.mule.runtime.api.config.MuleRuntimeFeature.COMPUTE_CONNECTION_ERRORS_IN_STATS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.refreshTokenIfNecessary;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toActionCode;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toMap;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.NULL_THROWABLE_CONSUMER;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.runtime.core.api.lifecycle.PrimaryNodeLifecycleNotificationListener;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.execution.MessageProcessContext;
import org.mule.runtime.core.internal.execution.MessageProcessingManager;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.config.ConfiguredComponent;
import org.mule.runtime.extension.api.runtime.source.ParameterizedSource;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionHandlerManager;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalSourceException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBasedParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.source.poll.RestartContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

/**
 * A {@link MessageSource} which connects the Extensions API with the Mule runtime by connecting a {@link Source} with a flow
 * represented by a {@link #messageProcessor}
 *
 * @since 4.0
 */
public class ExtensionMessageSource extends ExtensionComponent<SourceModel> implements MessageSource,
    ExceptionCallback<ConnectionException>, ParameterizedSource, ConfiguredComponent, LifecycleStateEnabled {

  private static final Logger LOGGER = getLogger(ExtensionMessageSource.class);

  @Inject
  private MessageProcessingManager messageProcessingManager;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private ClusterService clusterService;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  private final SourceModel sourceModel;
  private final SourceAdapterFactory sourceAdapterFactory;
  private final boolean primaryNodeOnly;
  private final RetryPolicyTemplate customRetryPolicyTemplate;
  private final BackPressureStrategy backPressureStrategy;
  private final ExceptionHandlerManager exceptionEnricherManager;
  private final AtomicBoolean reconnecting = new AtomicBoolean(false);
  private final DefaultLifecycleManager<ExtensionMessageSource> lifecycleManager;

  private SourceConnectionManager sourceConnectionManager;
  private Processor messageProcessor;
  private final LazyValue<TransactionConfig> transactionConfig = new LazyValue<>(this::buildTransactionConfig);

  private SourceAdapter sourceAdapter;
  private RetryPolicyTemplate retryPolicyTemplate;
  private Scheduler retryScheduler;
  // FlowConstruct is obtained when needed because during MUnit's tooling tests and Lazy Init mode this should never be evaluated.
  private LazyValue<FlowConstruct> flowConstruct;
  private MessageProcessContext messageProcessContext;

  private final NotificationDispatcher notificationDispatcher;
  private final SingleResourceTransactionFactoryManager transactionFactoryManager;
  private final String applicationName;

  private final AtomicBoolean started = new AtomicBoolean(false);

  public ExtensionMessageSource(ExtensionModel extensionModel,
                                SourceModel sourceModel,
                                SourceAdapterFactory sourceAdapterFactory,
                                ConfigurationProvider configurationProvider,
                                boolean primaryNodeOnly,
                                RetryPolicyTemplate retryPolicyTemplate,
                                CursorProviderFactory cursorProviderFactory,
                                BackPressureStrategy backPressureStrategy,
                                ExtensionManager managerAdapter, NotificationDispatcher notificationDispatcher,
                                SingleResourceTransactionFactoryManager transactionFactoryManager, String applicationName) {
    super(extensionModel, sourceModel, configurationProvider, cursorProviderFactory, managerAdapter);
    this.sourceModel = sourceModel;
    this.sourceAdapterFactory = sourceAdapterFactory;
    this.customRetryPolicyTemplate = retryPolicyTemplate;
    this.primaryNodeOnly = primaryNodeOnly;
    this.backPressureStrategy = backPressureStrategy;
    this.notificationDispatcher = notificationDispatcher;
    this.transactionFactoryManager = transactionFactoryManager;
    this.applicationName = applicationName;
    this.exceptionEnricherManager = new ExceptionHandlerManager(extensionModel, sourceModel);
    this.lifecycleManager = new DefaultLifecycleManager<>(sourceModel.getName(), this);
  }

  private synchronized void createSource(boolean restarting) throws Exception {
    if (sourceAdapter == null) {
      CoreEvent initialiserEvent = null;
      try {
        initialiserEvent = getInitialiserEvent(muleContext);
        Optional<ConfigurationInstance> configurationInstance = startUsingConfiguration(initialiserEvent);
        sourceAdapter =
            sourceAdapterFactory.createAdapter(configurationInstance,
                                               createSourceCallbackFactory(),
                                               this,
                                               sourceConnectionManager,
                                               restarting);
        muleContext.getInjector().inject(sourceAdapter);
        retryPolicyTemplate = createRetryPolicyTemplate(customRetryPolicyTemplate);
        initialiseIfNeeded(retryPolicyTemplate, true, muleContext);
      } finally {
        if (initialiserEvent != null) {
          ((BaseEventContext) initialiserEvent.getContext()).success();
        }
      }
    }
  }

  private void startSource(boolean restarting, RestartContext restartContext) throws MuleException {
    Runnable onSuccess;
    Consumer<Throwable> onFailure;
    if (retryPolicyTemplate.isAsync()) {
      onSuccess = this::onReconnectionSuccessful;
      onFailure = this::onReconnectionFailed;
    } else {
      onSuccess = () -> {
      };
      onFailure = (t) -> {
      };
    }
    Supplier<CompletableFuture<Void>> futureSupplier = () -> {
      CompletableFuture<Void> future = new CompletableFuture<>();
      retryScheduler.execute(() -> doWork(restarting, restartContext, future));
      return future;
    };
    CompletableFuture<Void> future = retryPolicyTemplate
        .applyPolicy(futureSupplier, t -> true, t -> computeStats(), NULL_THROWABLE_CONSUMER, identity(), retryScheduler)
        .whenComplete((v, e) -> {
          if (e != null) {
            onFailure.accept(e);
          } else {
            onSuccess.run();
          }
        });
    try {
      if (!retryPolicyTemplate.isAsync()) {
        future.get();
        this.onReconnectionSuccessful();
      }
    } catch (ExecutionException exception) {
      throw new RetryPolicyExhaustedException(exception.getCause(), ExtensionMessageSource.this);
    } catch (InterruptedException e) {
      throw new MuleRuntimeException(createStaticMessage(format("Found exception starting source '%s' on flow '%s'",
                                                                sourceModel.getName(), getLocation().getRootContainerName())),
                                     e);
    }
  }

  private void startSource() throws MuleException {
    startSource(false, null);
  }

  private RetryPolicyTemplate createRetryPolicyTemplate(RetryPolicyTemplate customTemplate) {
    return this.getConfigurationInstance()
        .map(config -> config.getConnectionProvider().orElse(null))
        .map(provider -> connectionManager.getReconnectionConfigFor(provider).getRetryPolicyTemplate(customTemplate))
        .orElseGet(() -> customTemplate != null ? customTemplate : ReconnectionConfig.getDefault().getRetryPolicyTemplate());
  }

  private void stopSource() throws MuleException {
    stopSource(false);
  }

  private RestartContext stopSource(boolean restarting) throws MuleException {
    if (sourceAdapter != null) {
      final String sourceName = sourceAdapter.getName();

      CoreEvent initialiserEvent = null;
      try {
        initialiserEvent = getInitialiserEvent(muleContext);
        try {
          stopUsingConfiguration(initialiserEvent);
          return restarting ? sourceAdapter.beginRestart() : null;
        } finally {
          sourceAdapter.stop();
          if (usesDynamicConfiguration()) {
            disposeSource();
          }
        }
      } catch (Exception e) {
        throw new DefaultMuleException(format("Found exception stopping source '%s' of root component '%s'",
                                              sourceName,
                                              getLocation().getRootContainerName()),
                                       e);
      } finally {
        if (initialiserEvent != null) {
          ((BaseEventContext) initialiserEvent.getContext()).success();
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationInstance> getConfigurationInstance() {
    return sourceAdapter.getConfigurationInstance();
  }

  private SourceCallbackFactory createSourceCallbackFactory() {
    return completionHandlerFactory -> DefaultSourceCallback.builder()
        .setExceptionCallback(this)
        .setSourceModel(sourceModel)
        .setConfigurationInstance(getConfigurationInstance().orElse(null))
        .setTransactionConfig(transactionConfig.get())
        .setSource(this)
        .setMuleContext(muleContext)
        .setListener(messageProcessor)
        .setProcessingManager(messageProcessingManager)
        .setProcessContext(messageProcessContext)
        .setApplicationName(applicationName)
        .setNotificationDispatcher(notificationDispatcher)
        .setTransactionFactoryManager(transactionFactoryManager)
        .setCursorStreamProviderFactory(getCursorProviderFactory())
        .setCompletionHandlerFactory(completionHandlerFactory)
        .build();
  }

  @Override
  public void onException(ConnectionException exception) {
    if (!reconnecting.compareAndSet(false, true)) {
      LOGGER.error(format(
                          "Message source '%s' on flow '%s' found connection error but reconnection is already in progress. Error was: %s",
                          sourceModel.getName(),
                          getLocation().getRootContainerName(),
                          exception.getMessage()),
                   exception);
      return;
    }

    muleContext.getExceptionListener().handleException(exception, getLocation());

    refreshTokenIfNecessary(getConfigurationInstance()
        .flatMap(configurationInstance -> configurationInstance.getConnectionProvider()).orElse(null), exception);

    LOGGER.warn(format("Message source '%s' on flow '%s' threw exception. Attempting to reconnect...",
                       sourceAdapter.getName(), getLocation().getRootContainerName()),
                exception);

    Optional<Publisher<Void>> action = sourceAdapter.getReconnectionAction(exception);
    if (!action.isPresent()) {
      invalidateConnection(exception);
      retryScheduler.execute(() -> {
        try {
          restart();
        } catch (MuleException e) {
          this.onReconnectionFailed(e);
        }
      });
    } else {
      retryScheduler.execute(() -> {
        Mono<Void> reconnectionAction = action.map(p -> from(retryPolicyTemplate.applyPolicy(p, retryScheduler))).get();
        reconnectionAction
            .doOnSuccess(v -> onReconnectionSuccessful())
            .doOnError(this::onReconnectionFailed)
            .subscribe();
      });
    }
  }

  private void onReconnectionSuccessful() {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn("Message source '{}' on flow '{}' successfully reconnected",
                  sourceModel.getName(), getLocation().getRootContainerName());
    }
    reconnecting.set(false);
  }

  private void onReconnectionFailed(Throwable exception) {
    LOGGER.error(format("Message source '%s' on flow '%s' could not be reconnected. Will be shutdown. %s",
                        sourceModel.getName(), getLocation().getRootContainerName(), exception.getMessage()),
                 exception);
    shutdown();
    reconnecting.set(false);
  }

  private void restart() throws MuleException {
    synchronized (started) {
      if (started.get()) {
        RestartContext restartContext = stopSource(true);
        disposeSource();
        startSource(true, restartContext);
      } else {
        LOGGER.warn(format("Message source '%s' on flow '%s' is stopped. Not doing restart", getLocation().getRootContainerName(),
                           getLocation().getRootContainerName()));
      }
    }
  }

  @Override
  public void doStart() throws MuleException {
    if (shouldRunOnThisNode()) {
      reallyDoStart();
    }
  }

  private void reallyDoStart() throws MuleException {
    LOGGER.debug("Message source '{}' on flow '{}' is starting", sourceModel.getName(),
                 getLocation().getRootContainerName());
    lifecycle(() -> lifecycleManager.fireStartPhase((phase, o) -> {
      startIfNeeded(retryPolicyTemplate);

      if (retryScheduler == null) {
        retryScheduler = schedulerService.ioScheduler();
      }

      synchronized (started) {
        startSource();
        started.set(true);
      }
    }));
  }

  @Override
  public void doStop() throws MuleException {
    LOGGER.debug("Message source '{}' on flow '{}' is stopping", sourceModel.getName(),
                 getLocation().getRootContainerName());
    safeLifecycle(() -> lifecycleManager.fireStopPhase((phase, o) -> {
      synchronized (started) {
        started.set(false);
        stopSource();
      }

      stopSchedulers();
    }));
  }

  @Override
  public void doDispose() {
    try {
      safeLifecycle(() -> lifecycleManager.fireDisposePhase((phase, o) -> {
        disposeSource();
        stopIfNeeded(retryPolicyTemplate);
        disposeIfNeeded(retryPolicyTemplate, LOGGER);
        stopSchedulers();
      }));
    } catch (MuleException e) {
      LOGGER.warn(format("Failed to dispose message source at root element '%s'. %s",
                         getLocation().getRootContainerName(),
                         e.getMessage()),
                  e);
    }
  }

  private void lifecycle(CheckedRunnable runnable) throws MuleException {
    try {
      runnable.run();
    } catch (Throwable e) {
      handleLifecycleException(e, false);
    }
  }

  private void safeLifecycle(CheckedRunnable runnable) throws MuleException {
    try {
      runnable.run();
    } catch (Throwable e) {
      handleLifecycleException(e, true);
    }
  }

  private void handleLifecycleException(Throwable e, boolean unwrapLifecycleException) throws MuleException {
    e = unwrap(e);
    if (unwrapLifecycleException && e instanceof LifecycleException && e.getCause() != null) {
      e = e.getCause();
    }

    if (e instanceof IllegalStateException) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping lifecycle phase: " + e.getMessage(), e);
      }
    } else if (e instanceof MuleException) {
      throw (MuleException) e;
    } else {
      throw new DefaultMuleException(e);
    }
  }

  private void shutdown() {
    try {
      stopIfNeeded(this);
    } catch (Exception e) {
      LOGGER
          .error(format("Failed to stop source '%s' on flow '%s'", sourceAdapter.getName(), getLocation().getRootContainerName()),
                 e);
    }
    disposeIfNeeded(this, LOGGER);
  }

  private void stopSchedulers() {
    if (retryScheduler != null) {
      try {
        retryScheduler.stop();
      } finally {
        retryScheduler = null;
      }
    }
  }

  private void disposeSource() {
    disposeIfNeeded(sourceAdapter, LOGGER);
    sourceAdapter = null;
  }

  private TransactionConfig buildTransactionConfig() {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setAction(toActionCode(sourceAdapter.getTransactionalAction()));
    transactionConfig.setMuleContext(muleContext);
    TransactionType transactionalType = sourceAdapter.getTransactionalType();
    transactionConfig.setFactory(transactionFactoryLocator.lookUpTransactionFactory(transactionalType)
        .orElseThrow(() -> new IllegalStateException(format(
                                                            "Unable to create Source with Transactions of Type: [%s]. No factory available for this transaction type",
                                                            transactionalType))));

    return transactionConfig;
  }

  SourceConnectionManager getSourceConnectionManager() {
    return sourceConnectionManager;
  }

  private MessageProcessContext createProcessingContext() {

    return new MessageProcessContext() {

      private final MessagingExceptionResolver messagingExceptionResolver = new MessagingExceptionResolver(getMessageSource());

      @Override
      public MessageSource getMessageSource() {
        return ExtensionMessageSource.this;
      }

      @Override
      public Optional<TransactionConfig> getTransactionConfig() {
        return sourceModel.isTransactional() ? of(transactionConfig.get()) : empty();
      }

      @Override
      public ClassLoader getExecutionClassLoader() {
        return muleContext.getExecutionClassLoader();
      }

      @Override
      public ErrorTypeLocator getErrorTypeLocator() {
        return ((PrivilegedMuleContext) muleContext).getErrorTypeLocator();
      }

      @Override
      public MessagingExceptionResolver getMessagingExceptionResolver() {
        return messagingExceptionResolver;
      }

      @Override
      public FlowConstruct getFlowConstruct() {
        return flowConstruct.get();
      }
    };
  }

  private void doWork(boolean restarting, RestartContext restartContext, CompletableFuture<Void> future) {
    try {
      createSource(restarting);
      initialiseIfNeeded(sourceAdapter);
      if (restarting) {
        sourceAdapter.finishRestart(restartContext);
      }
      sourceAdapter.start();
      future.complete(null);
    } catch (Exception e) {
      try {
        // On connection exception, if the failed connection is present, it must be invalidated before stopping the source. This
        // warranties that a possible call to connectionProvider.disconnect made on the onStop method of the source, does not
        // affect the connection's invalidation
        extractConnectionException(e).ifPresent(connectionException -> invalidateConnection(connectionException));
        stopSource();
      } catch (Exception eStop) {
        e.addSuppressed(eStop);
      }
      try {
        disposeSource();
      } catch (Exception eDispose) {
        e.addSuppressed(eDispose);
      }
      Throwable throwable = exceptionEnricherManager.process(e);
      Optional<ConnectionException> connectionException = extractConnectionException(throwable);
      if (connectionException.isPresent()) {
        throwable = connectionException.get();
      }
      throwable = throwable instanceof Exception ? ((Exception) throwable) : new MuleRuntimeException(throwable);
      future.completeExceptionally(throwable);
    }
  }

  private void computeStats() {
    AllStatistics statistics = muleContext.getStatistics();

    if (statistics != null && statistics.isEnabled() && computeConnectionErrorsInStats()) {
      statistics.getApplicationStatistics().incConnectionErrors();
    }
  }

  private boolean computeConnectionErrorsInStats() {
    return featureFlaggingService.isEnabled(COMPUTE_CONNECTION_ERRORS_IN_STATS);
  }

  @Override
  public void setListener(Processor listener) {
    messageProcessor = listener;
  }

  /**
   * Validates if the current source is valid for the set configuration. In case that the validation fails, the method will throw
   * a {@link IllegalSourceException}
   */
  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    ConfigurationModel configurationModel = configurationProvider.getConfigurationModel();
    if (!configurationModel.getSourceModel(sourceModel.getName()).isPresent()
        && !configurationProvider.getExtensionModel().getSourceModel(sourceModel.getName()).isPresent()) {
      throw new IllegalSourceException(format(
                                              "Root component '%s' defines an usage of operation '%s' which points to configuration '%s'. "
                                                  + "The selected config does not support that operation.",
                                              getLocation().getRootContainerName(), sourceModel.getName(),
                                              configurationProvider.getName()));
    }
  }

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    return new ObjectBasedParameterValueResolver(sourceAdapter.getDelegate(), sourceModel, reflectionCache);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    flowConstruct = new LazyValue<>(() -> (FlowConstruct) componentLocator.find(getRootContainerLocation()).orElse(null));
    messageProcessContext = createProcessingContext();
    if (shouldRunOnThisNode()) {
      if (LOGGER.isDebugEnabled()) {
        boolean isPrimaryPollingInstance = clusterService.isPrimaryPollingInstance();
        if (primaryNodeOnly) {
          LOGGER
              .debug("Message source '{}' on flow '{}' running on the primary node is initializing. Note that this Message source must run on the primary node only.",
                     sourceModel.getName(), getLocation().getRootContainerName());
        } else {
          LOGGER
              .debug("Message source '{}' on flow '{}' is initializing. This {} the primary node of the cluster.",
                     sourceModel.getName(), getLocation().getRootContainerName(), isPrimaryPollingInstance ? "is" : "is not");
        }
      }

      reallyDoInitialise();
    } else {
      LOGGER
          .debug("Message source '{}' on flow '{}' cannot initialize. This Message source can only run on the primary node of the cluster",
                 sourceModel.getName(), getLocation().getRootContainerName());
      new PrimaryNodeLifecycleNotificationListener(() -> {
        LOGGER.debug("Message source '{}' on flow '{}' is initializing because the node became cluster's primary.",
                     sourceModel.getName(), getLocation().getRootContainerName());
        reallyDoInitialise();
        reallyDoStart();
      }, notificationListenerRegistry).register();
    }
  }

  private void reallyDoInitialise() throws InitialisationException {
    try {
      lifecycle(() -> lifecycleManager.fireInitialisePhase((phase, o) -> {
        sourceConnectionManager = new SourceConnectionManager(connectionManager);

        try {
          createSource(false);
          initialiseIfNeeded(sourceAdapter);
        } catch (Exception e) {
          throw new InitialisationException(e, this);
        }
      }));
    } catch (MuleException e) {
      if (e instanceof InitialisationException) {
        throw (InitialisationException) e;
      } else {
        throw new InitialisationException(e, this);
      }
    }
  }

  @Override
  public Map<String, Object> getInitialisationParameters() {
    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent();
      ResolverSet sourceParameters = sourceAdapterFactory.getSourceParameters();
      try (ValueResolvingContext context = ValueResolvingContext.builder(initialiserEvent)
          .withExpressionManager(expressionManager)
          .withConfig(getConfigurationInstance())
          .build()) {
        return copyOf(toMap(sourceParameters, context));
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("Could not resolve parameters message source at location '%s'",
                                                                getLocation().toString()),
                                                         e));
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  @Override
  public BackPressureStrategy getBackPressureStrategy() {
    return backPressureStrategy;
  }

  @Override
  public LifecycleState getLifecycleState() {
    return lifecycleManager.getState();
  }

  private boolean shouldRunOnThisNode() {
    return primaryNodeOnly ? clusterService.isPrimaryPollingInstance() : true;
  }

  private Optional<ConfigurationInstance> startUsingConfiguration(CoreEvent event) {
    return getConfigurationAndTryToMutateStats(event,
                                               (mutableConfigurationStats -> mutableConfigurationStats.addRunningSource()));
  }

  private void stopUsingConfiguration(CoreEvent event) {
    getConfigurationAndTryToMutateStats(event, (mutableConfigurationStats -> mutableConfigurationStats.discountRunningSource()));
  }

  private Optional<ConfigurationInstance> getConfigurationAndTryToMutateStats(CoreEvent event,
                                                                              Consumer<MutableConfigurationStats> mutableConfigurationStatsConsumer) {
    Optional<ConfigurationInstance> configurationInstanceOptional = getConfiguration(event);
    configurationInstanceOptional.ifPresent(configurationInstance -> {
      ConfigurationStats configurationStats = configurationInstance.getStatistics();
      if (configurationStats instanceof MutableConfigurationStats) {
        mutableConfigurationStatsConsumer.accept((MutableConfigurationStats) configurationStats);
      }
    });
    return configurationInstanceOptional;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + ": " + Objects.toString(sourceAdapter);
  }

  /**
   * Indicates if a reconnection is happening
   *
   * @return {@code true} if a reconnection is happening, {@code false} otherwise.
   */
  boolean isReconnecting() {
    return reconnecting.get();
  }

  private void invalidateConnection(ConnectionException exception) {
    exception.getConnection().ifPresent(sourceConnectionManager::invalidate);
  }
}
