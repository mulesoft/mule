/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.execution.MessageProcessContext;
import org.mule.runtime.core.api.execution.MessageProcessingManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionSourceBinder;

import java.util.function.Supplier;

/**
 * Default implementation of {@link SourceCallback}.
 * Instances are to be created through the {@link #builder()} method.
 *
 * @param <T> the generic type of the output values of the generated results
 * @param <A> the generic type of the attributes of the generated results
 * @since 4.0
 */
class DefaultSourceCallback<T, A> implements SourceCallbackAdapter<T, A> {

  /**
   * A Builder to create instance of {@link DefaultSourceCallback}
   *
   * @param <T> the generic type of the output values of the generated results
   * @param <A> the generic type of the attributes of the generated results
   */
  static class Builder<T, A> {

    private Builder() {}

    private DefaultSourceCallback<T, A> product = new DefaultSourceCallback();

    public Builder<T, A> setSourceModel(SourceModel sourceModel) {
      product.sourceModel = sourceModel;
      product.returnsListOfMessages = returnsListOfMessages(sourceModel);
      return this;
    }

    public Builder<T, A> setConfigurationInstance(ConfigurationInstance configurationInstance) {
      product.configurationInstance = configurationInstance;
      return this;
    }

    public Builder<T, A> setTransactionConfig(TransactionConfig transactionConfig) {
      product.transactionConfig = transactionConfig;
      return this;
    }

    public Builder<T, A> setListener(Processor listener) {
      product.listener = listener;
      return this;
    }

    public Builder<T, A> setMuleContext(MuleContext muleContext) {
      product.muleContext = muleContext;
      return this;
    }

    public Builder<T, A> setSource(ExtensionMessageSource messageSource) {
      product.messageSource = messageSource;
      return this;
    }

    public Builder<T, A> setFlowConstruct(FlowConstruct flowConstruct) {
      product.flowConstruct = flowConstruct;
      return this;
    }

    public Builder<T, A> setExceptionCallback(ExceptionCallback exceptionCallback) {
      product.exceptionCallback = exceptionCallback;
      return this;
    }

    public Builder<T, A> setProcessingManager(MessageProcessingManager processingManager) {
      product.messageProcessingManager = processingManager;
      return this;
    }

    public Builder<T, A> setProcessContextSupplier(Supplier<MessageProcessContext> processContextSupplier) {
      product.processContextSupplier = processContextSupplier;
      return this;
    }

    public Builder<T, A> setCompletionHandlerFactory(SourceCompletionHandlerFactory completionHandlerFactory) {
      product.completionHandlerFactory = completionHandlerFactory;
      return this;
    }

    public Builder<T, A> setCursorStreamProviderFactory(CursorProviderFactory cursorProviderFactory) {
      product.cursorProviderFactory = cursorProviderFactory;
      return this;
    }

    public SourceCallback<T, A> build() {
      checkArgument(product.listener, "listener");
      checkArgument(product.flowConstruct, "flowConstruct");
      checkArgument(product.exceptionCallback, "exceptionCallback");
      checkArgument(product.messageProcessingManager, "messageProcessingManager");
      checkArgument(product.processContextSupplier, "processContextSupplier");
      checkArgument(product.completionHandlerFactory, "completionHandlerSupplier");
      checkArgument(product.sourceModel, "sourceModel");
      checkArgument(product.cursorProviderFactory, "cursorStreamProviderFactory");
      checkArgument(product.messageSource, "messageSource");
      checkArgument(product.muleContext, "muleContext");

      product.transactionSourceBinder =
          new TransactionSourceBinder(product.messageSource.getExtensionModel(), product.sourceModel, product.muleContext);

      return product;
    }

    private void checkArgument(Object value, String name) {
      Preconditions.checkArgument(value != null, name + " was not set");
    }

  }

  /**
   * @return a new {@link Builder}
   */
  static Builder builder() {
    return new Builder();
  }

  private SourceModel sourceModel;
  private ConfigurationInstance configurationInstance;
  private Processor listener;
  private MuleContext muleContext;
  private ExtensionMessageSource messageSource;
  private FlowConstruct flowConstruct;
  private ExceptionCallback exceptionCallback;
  private MessageProcessingManager messageProcessingManager;
  private Supplier<MessageProcessContext> processContextSupplier;
  private SourceCompletionHandlerFactory completionHandlerFactory;
  private CursorProviderFactory cursorProviderFactory;
  private TransactionConfig transactionConfig;
  private boolean returnsListOfMessages = false;
  private TransactionSourceBinder transactionSourceBinder;

  private DefaultSourceCallback() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(Result<T, A> result) {
    handle(result, createContext());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(Result<T, A> result, SourceCallbackContext context) {
    checkArgument(context instanceof SourceCallbackContextAdapter, "The supplied context was not created through this callback, "
        + "you naughty developer");
    MessageProcessContext messageProcessContext = processContextSupplier.get();

    SourceResultAdapter resultAdapter = new SourceResultAdapter(result, cursorProviderFactory, returnsListOfMessages);
    Message message = of(resultAdapter);

    executeFlow(context, messageProcessContext, message);
  }

  private void executeFlow(SourceCallbackContext context, MessageProcessContext messageProcessContext, Message message) {
    messageProcessingManager.processMessage(
                                            new ModuleFlowProcessingTemplate(message, listener, completionHandlerFactory
                                                .createCompletionHandler((SourceCallbackContextAdapter) context)),
                                            messageProcessContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSourceException(Throwable exception) {
    exceptionCallback.onException(exception);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SourceCallbackContext createContext() {
    return new DefaultSourceCallbackContext(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionSourceBinder getTransactionSourceBinder() {
    return transactionSourceBinder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationInstance getConfigurationInstance() {
    return configurationInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionConfig getTransactionConfig() {
    return transactionConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SourceConnectionManager getSourceConnectionManager() {
    return messageSource.getSourceConnectionManager();
  }
}
