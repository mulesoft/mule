/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Context for processing one message from a {@link MessageSource}.
 *
 * Mule {@link MessageSource} implementations should create one instance of MessageProcessContext per message that generates.
 *
 * MessageProcessContext is responsible for - Define if the incoming message can be processed in a separate thread - Provide
 * access to the {@link MessageSource} of the message - Provide access to the {@link FlowConstruct} were the message is going to
 * be executed - Provide access, if available, to the {@link Executor} to use for processing the message - Provide the
 * {@link MessageSource} transaction configuration
 */
public interface MessageProcessContext {

  /**
   * @return the {@link MessageSource} that retrieve the message. Can not be null
   */
  MessageSource getMessageSource();

  /**
   * @return the {@link TransactionConfig} associated to the {@link MessageSource} that received the message. If
   *         {@link Optional#empty()} then no transaction config will be used.
   */
  Optional<TransactionConfig> getTransactionConfig();

  /**
   * @return the class loader to be used for processing the message.
   */
  ClassLoader getExecutionClassLoader();

  /**
   * @return the {@link ErrorTypeLocator} for this context
   */
  ErrorTypeLocator getErrorTypeLocator();

  /**
   * @return the exception resolver for this context's source
   */
  MessagingExceptionResolver getMessagingExceptionResolver();

  FlowConstruct getFlowConstruct();
}
