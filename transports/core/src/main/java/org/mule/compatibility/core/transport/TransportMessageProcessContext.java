/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.execution.MessageProcessContext;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic {@link org.mule.execution.MessageProcessContext} implementations for transports.
 */
public class TransportMessageProcessContext implements MessageProcessContext {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private final MessageReceiver messageReceiver;
  private Executor flowExecutor;

  /**
   * Creates an instance that executes the flow in the current thread. Calling #supportsAsynchronousProcessing method will always
   * return false since there's not work manager specified for the flow execution.
   *
   * @param messageReceiver receiver of the message
   */
  public TransportMessageProcessContext(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * Creates an instance that executes the flow using the supplied WorkManager. Calling #supportsAsynchronousProcessing method
   * will always return true since there's a WorkManager available to execute the flow.
   *
   * @param messageReceiver receiver of the message
   * @param flowExecutor the {@link Executor} to use for the flow execution
   */
  public TransportMessageProcessContext(MessageReceiver messageReceiver, Executor flowExecutor) {
    this.messageReceiver = messageReceiver;
    this.flowExecutor = flowExecutor;
  }

  @Override
  public MessageSource getMessageSource() {
    return this.messageReceiver.getEndpoint();
  }

  protected MessageSource getMessageReceiver() {
    return this.messageReceiver;
  }

  @Override
  public FlowConstruct getFlowConstruct() {
    return this.messageReceiver.getFlowConstruct();
  }

  @Override
  public boolean supportsAsynchronousProcessing() {
    if (flowExecutor != null) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Executor getFlowExecutionExecutor() {
    return flowExecutor;
  }

  @Override
  public TransactionConfig getTransactionConfig() {
    return messageReceiver.getEndpoint().getTransactionConfig();
  }

  @Override
  public ClassLoader getExecutionClassLoader() {
    return messageReceiver.getEndpoint().getMuleContext().getExecutionClassLoader();
  }

}

