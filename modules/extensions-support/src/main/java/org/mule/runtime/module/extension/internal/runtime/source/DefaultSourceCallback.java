/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.function.Supplier;

/**
 * Default implementation of {@link SourceCallback}. Instances are to be created through the {@link #builder()} method.
 *
 * @param <T> the generic type of the output values of the generated results
 * @param <A> the generic type of the attributes of the generated results
 * @since 4.0
 */
class DefaultSourceCallback<T, A extends Attributes> implements SourceCallback<T, A>, Startable, Stoppable {

  /**
   * A Builder to create instance of {@link DefaultSourceCallback}
   *
   * @param <T> the generic type of the output values of the generated results
   * @param <A> the generic type of the attributes of the generated results
   */
  static class Builder<T, A extends Attributes> {

    private Builder() {}

    private DefaultSourceCallback<T, A> product = new DefaultSourceCallback();

    public Builder<T, A> setListener(Processor listener) {
      product.listener = listener;
      return this;
    }

    public Builder<T, A> setConfigName(String configName) {
      product.configName = configName;
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

    public SourceCallback<T, A> build() {
      checkArgument(product.listener, "listener");
      checkArgument(product.flowConstruct, "flowConstruct");
      checkArgument(product.exceptionCallback, "exceptionCallback");
      checkArgument(product.messageProcessingManager, "messageProcessingManager");
      checkArgument(product.processContextSupplier, "processContextSupplier");
      checkArgument(product.completionHandlerFactory, "completionHandlerSupplier");
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

  private Processor listener;
  private String configName;
  private FlowConstruct flowConstruct;
  private ExceptionCallback exceptionCallback;
  private MessageProcessingManager messageProcessingManager;
  private Supplier<MessageProcessContext> processContextSupplier;
  private SourceCompletionHandlerFactory completionHandlerFactory;
  private Sink sink;
  private Disposable streamCancellation;

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
    MessageProcessContext messageProcessContext = processContextSupplier.get();
    messageProcessingManager.processMessage(
                                            new ModuleFlowProcessingTemplate(toMessage(result),
                                                                             listener,
                                                                             completionHandlerFactory
                                                                                 .createCompletionHandler(context),
                                                                             messageProcessContext,
                                                                             sink),
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

  @Override
  public void start() throws MuleException {
    if (flowConstruct instanceof Pipeline) {
      sink = ((Pipeline) flowConstruct).getProcessingStrategy().createSink(flowConstruct, listener);
    } else {
      sink = SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE.createSink(flowConstruct, listener);
    }
  }

  @Override
  public void stop() throws MuleException {
    sink.complete();
  }
}
