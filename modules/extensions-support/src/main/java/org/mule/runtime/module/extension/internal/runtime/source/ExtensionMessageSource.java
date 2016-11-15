/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldValue;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionEnricherManager;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalOperationException;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalSourceException;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * A {@link MessageSource} which connects the Extensions API with the Mule runtime by connecting a {@link Source} with a flow
 * represented by a {@link #messageProcessor}
 *
 * @since 4.0
 */
public class ExtensionMessageSource extends ExtensionComponent implements MessageSource, ExceptionCallback {

  private static final Logger LOGGER = getLogger(ExtensionMessageSource.class);

  @Inject
  private MessageProcessingManager messageProcessingManager;

  @Inject
  private SchedulerService schdulerService;

  private final SourceModel sourceModel;
  private final SourceAdapterFactory sourceAdapterFactory;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private final ExceptionEnricherManager exceptionEnricherManager;
  private Processor messageProcessor;

  private SourceAdapter sourceAdapter;
  private Scheduler retryScheduler;
  private Scheduler flowTriggerScheduler;

  public ExtensionMessageSource(ExtensionModel extensionModel, SourceModel sourceModel, SourceAdapterFactory sourceAdapterFactory,
                                ConfigurationProvider configurationProvider, RetryPolicyTemplate retryPolicyTemplate,
                                ExtensionManagerAdapter managerAdapter) {
    super(extensionModel, sourceModel, configurationProvider, managerAdapter);
    this.sourceModel = sourceModel;
    this.sourceAdapterFactory = sourceAdapterFactory;
    this.retryPolicyTemplate = retryPolicyTemplate;
    this.exceptionEnricherManager = new ExceptionEnricherManager(extensionModel, sourceModel);

  }

  private synchronized void createSource() throws Exception {
    if (sourceAdapter == null) {
      sourceAdapter =
          sourceAdapterFactory.createAdapter(getConfiguration(getInitialiserEvent(muleContext)), createSourceCallbackFactory());
      muleContext.getInjector().inject(sourceAdapter);
      sourceAdapter.setFlowConstruct(flowConstruct);
    }
  }

  private void startSource() {
    try {
      retryPolicyTemplate.execute(new SourceRetryCallback(), retryScheduler);
    } catch (Throwable e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void stopSource() throws MuleException {
    if (sourceAdapter != null) {
      try {
        sourceAdapter.stop();
      } catch (Exception e) {
        throw new DefaultMuleException(format("Found exception stopping source '%s' of flow '%s'", sourceAdapter.getName(),
                                              flowConstruct.getName()),
                                       e);
      }
    }
  }

  private SourceCallbackFactory createSourceCallbackFactory() {
    return completionHandlerFactory -> DefaultSourceCallback.builder()
        .setConfigName(getConfigName())
        .setExceptionCallback(this)
        .setFlowConstruct(flowConstruct)
        .setListener(messageProcessor)
        .setProcessingManager(messageProcessingManager)
        .setProcessContextSupplier(this::createProcessingContext)
        .setCompletionHandlerFactory(completionHandlerFactory)
        .build();
  }

  @Override
  public void onException(Throwable exception) {
    exception = exceptionEnricherManager.processException(exception);
    Optional<ConnectionException> connectionException = extractConnectionException(exception);
    if (connectionException.isPresent()) {
      try {
        LOGGER.warn(format("Message source '%s' on flow '%s' threw exception. Restarting...", sourceAdapter.getName(),
                           flowConstruct.getName()),
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
    LOGGER.error(format("Message source '%s' on flow '%s' threw exception. Shutting down it forever...", sourceAdapter.getName(),
                        flowConstruct.getName()),
                 exception);
    shutdown();
  }

  private void restart() throws MuleException {
    stopSource();
    disposeSource();
    startSource();
  }

  @Override
  public void doStart() throws MuleException {
    if (retryScheduler == null) {
      retryScheduler = schdulerService.ioScheduler();
    }
    if (flowTriggerScheduler == null) {
      flowTriggerScheduler = schdulerService.cpuLightScheduler();
    }

    startSource();
  }

  @Override
  public void doStop() throws MuleException {
    try {
      stopSource();
    } finally {
      stopSchedulers();
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
      LOGGER.error(format("Failed to stop source '%s' on flow '%s'", sourceAdapter.getName(), flowConstruct.getName()), e);
    }
    disposeIfNeeded(this, LOGGER);
  }

  private void stopSchedulers() {
    if (retryScheduler != null) {
      try {
        retryScheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
      } finally {
        retryScheduler = null;
      }
    }
    if (flowTriggerScheduler != null) {
      try {
        flowTriggerScheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
      } finally {
        flowTriggerScheduler = null;
      }
    }
  }

  private void disposeSource() {
    disposeIfNeeded(sourceAdapter, LOGGER);
    sourceAdapter = null;
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
      public FlowConstruct getFlowConstruct() {
        return flowConstruct;
      }

      @Override
      public Scheduler getFlowExecutionExecutor() {
        return flowTriggerScheduler;
      }

      @Override
      public TransactionConfig getTransactionConfig() {
        return null;
      }

      @Override
      public ClassLoader getExecutionClassLoader() {
        return muleContext.getExecutionClassLoader();
      }
    };
  }

  private class SourceRetryCallback implements RetryCallback {

    @Override
    public void doWork(RetryContext context) throws Exception {
      try {
        createSource();
        sourceAdapter.start();
      } catch (Exception e) {
        stopSource();
        disposeSource();
        Exception exception = exceptionEnricherManager.processException(e);
        Optional<ConnectionException> connectionException = extractConnectionException(exception);
        if (connectionException.isPresent()) {
          exception = connectionException.get();
        }

        throw exception;
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
      throw new IllegalOperationException(format(
                                                 "Flow '%s' defines an usage of operation '%s' which points to configuration '%s'. "
                                                     + "The selected config does not support that operation.",
                                                 flowConstruct.getName(), sourceModel.getName(),
                                                 configurationProvider.getName()));
    }
  }

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    return fieldName -> {
      try {
        return getFieldValue(sourceAdapter.getDelegate(), fieldName);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new ValueResolvingException(e.getMessage(), e);
      }
    };
  }

  private String getConfigName() {
    ConfigurationProvider configurationProvider = getConfigurationProvider();
    return configurationProvider != null ? configurationProvider.getName() : null;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    try {
      createSource();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }
}
