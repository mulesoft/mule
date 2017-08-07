/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toActionCode;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.exception.ErrorTypeLocator;
import org.mule.runtime.core.api.execution.MessageProcessContext;
import org.mule.runtime.core.api.execution.MessageProcessingManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.source.ParameterizedSource;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionHandlerManager;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalSourceException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBasedParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionFactory;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * A {@link MessageSource} which connects the Extensions API with the Mule runtime by connecting a {@link Source} with a flow
 * represented by a {@link #messageProcessor}
 *
 * @since 4.0
 */
public class ExtensionMessageSource extends ExtensionComponent<SourceModel> implements MessageSource, ExceptionCallback,
    ParameterizedSource {

  private static final Logger LOGGER = getLogger(ExtensionMessageSource.class);

  @Inject
  private MessageProcessingManager messageProcessingManager;

  @Inject
  private SchedulerService schedulerService;

  private final SourceModel sourceModel;
  private final SourceAdapterFactory sourceAdapterFactory;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private final ExceptionHandlerManager exceptionEnricherManager;

  private SourceConnectionManager sourceConnectionManager;
  private Processor messageProcessor;
  private LazyValue<TransactionConfig> transactionConfig = new LazyValue<>(this::buildTransactionConfig);

  private SourceAdapter sourceAdapter;
  private Scheduler retryScheduler;
  private Scheduler flowTriggerScheduler;

  // TODO - MULE-12066 : Support XA transactions in SDK extensions at source level
  private final ExtensionTransactionFactory transactionFactory = new ExtensionTransactionFactory();
  private AtomicBoolean started = new AtomicBoolean(false);

  public ExtensionMessageSource(ExtensionModel extensionModel,
                                SourceModel sourceModel,
                                SourceAdapterFactory sourceAdapterFactory,
                                ConfigurationProvider configurationProvider,
                                RetryPolicyTemplate retryPolicyTemplate,
                                CursorProviderFactory cursorProviderFactory,
                                ExtensionManager managerAdapter) {
    super(extensionModel, sourceModel, configurationProvider, cursorProviderFactory, managerAdapter);
    this.sourceModel = sourceModel;
    this.sourceAdapterFactory = sourceAdapterFactory;
    this.retryPolicyTemplate = retryPolicyTemplate;
    this.exceptionEnricherManager = new ExceptionHandlerManager(extensionModel, sourceModel);
  }

  private synchronized void createSource() throws Exception {
    if (sourceAdapter == null) {
      sourceAdapter =
          sourceAdapterFactory.createAdapter(getConfiguration(getInitialiserEvent(muleContext)),
                                             createSourceCallbackFactory(),
                                             getLocation(),
                                             sourceConnectionManager);
      muleContext.getInjector().inject(sourceAdapter);
    }
  }

  private void startSource() throws MuleException {
    try {
      retryPolicyTemplate.execute(new SourceRetryCallback(), retryScheduler);
    } catch (Throwable e) {
      if (e instanceof MuleException) {
        throw (MuleException) e;
      } else {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private void stopSource() throws MuleException {
    if (sourceAdapter != null) {
      try {
        sourceAdapter.stop();
      } catch (Exception e) {
        throw new DefaultMuleException(format("Found exception stopping source '%s' of root component '%s'",
                                              sourceAdapter.getName(),
                                              getLocation().getRootContainerName()),
                                       e);
      }
    }
  }

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
        .setListener(messageProcessor)
        .setProcessingManager(messageProcessingManager)
        .setMuleContext(muleContext)
        .setProcessContextSupplier(this::createProcessingContext)
        .setCursorStreamProviderFactory(getCursorProviderFactory())
        .setCompletionHandlerFactory(completionHandlerFactory)
        .build();
  }

  @Override
  public void onException(Throwable exception) {
    exception = exceptionEnricherManager.process(exception);
    Optional<ConnectionException> connectionException = extractConnectionException(exception);
    if (connectionException.isPresent()) {
      try {
        LOGGER.warn(format("Message source '%s' on root component '%s' threw exception. Restarting...", sourceAdapter.getName(),
                           getLocation().getRootContainerName()),
                    exception);
        restart();
      } catch (Throwable e) {
        notifyExceptionAndShutDown(e);
      }
    } else {
      notifyExceptionAndShutDown(exception);
    }
  }

  private void notifyExceptionAndShutDown(Throwable exception) {
    LOGGER.error(format("Message source on root component '%s' threw exception. Shutting down it forever...",
                        getLocation().getRootContainerName()),
                 exception);
    shutdown();
  }

  private void restart() throws MuleException {
    synchronized (started) {
      if (started.get()) {
        stopSource();
        disposeSource();
        startSource();
      } else {
        LOGGER.warn(format("Message source '%s' on root component '%s' is stopped. Not doing restart", sourceAdapter.getName(),
                           getLocation().getRootContainerName()));
      }
    }
  }

  @Override
  public void doStart() throws MuleException {
    if (retryScheduler == null) {
      retryScheduler = schedulerService.ioScheduler();
    }
    if (flowTriggerScheduler == null) {
      flowTriggerScheduler = schedulerService.cpuLightScheduler();
    }

    synchronized (started) {
      startSource();
      started.set(true);
    }
  }

  @Override
  public void doStop() throws MuleException {
    synchronized (started) {
      started.set(false);
      try {
        stopSource();
      } finally {
        stopSchedulers();
      }
    }
  }

  @Override
  public void doDispose() {
    disposeSource();
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
    if (flowTriggerScheduler != null) {
      try {
        flowTriggerScheduler.stop();
      } finally {
        flowTriggerScheduler = null;
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
    transactionConfig.setFactory(transactionFactory);

    return transactionConfig;
  }

  SourceConnectionManager getSourceConnectionManager() {
    return sourceConnectionManager;
  }

  private MessageProcessContext createProcessingContext() {

    return new MessageProcessContext() {

      @Override
      public boolean supportsAsynchronousProcessing() {
        return true;
      }

      @Override
      public MessageSource getMessageSource() {
        return ExtensionMessageSource.this;
      }

      @Override
      public Scheduler getFlowExecutionExecutor() {
        return flowTriggerScheduler;
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
        return muleContext.getErrorTypeLocator();
      }
    };
  }

  private class SourceRetryCallback implements RetryCallback {

    @Override
    public void doWork(RetryContext context) throws Exception {
      try {
        createSource();
        sourceAdapter.initialise();
        sourceAdapter.start();
      } catch (Exception e) {
        stopSource();
        disposeSource();
        Throwable throwable = exceptionEnricherManager.process(e);
        Optional<ConnectionException> connectionException = extractConnectionException(throwable);
        if (connectionException.isPresent()) {
          throwable = connectionException.get();
        }
        throw throwable instanceof Exception ? ((Exception) throwable) : new MuleRuntimeException(throwable);
      }
    }

    @Override
    public String getWorkDescription() {
      return "Message Source Reconnection";
    }

    @Override
    public Object getWorkOwner() {
      return ExtensionMessageSource.this;
    }
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
    return new ObjectBasedParameterValueResolver(sourceAdapter.getDelegate(), sourceModel);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    sourceConnectionManager = new SourceConnectionManager(connectionManager);
    try {
      createSource();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public Map<String, Object> getInitialisationParameters() {
    final ValueResolvingContext ctx = from(MuleExtensionUtils.getInitialiserEvent());
    Map<String, ValueResolver<?>> resolvers = sourceAdapterFactory.getSourceParameters().getResolvers();
    return resolvers.entrySet().stream()
        .collect(toImmutableMap(entry -> entry.getKey(), entry -> {
          try {
            return entry.getValue().resolve(ctx);
          } catch (MuleException e) {
            throw new MuleRuntimeException(createStaticMessage(format(
                                                                      "Could not resolve parameter '%s' for message source at location '%s'",
                                                                      entry.getKey()),
                                                               getLocation().toString()),
                                           e);
          }
        }));
  }
}
