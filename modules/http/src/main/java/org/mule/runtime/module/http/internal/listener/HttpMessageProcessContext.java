/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.concurrent.Executor;

public class HttpMessageProcessContext implements MessageProcessContext {

  private static final ComponentIdentifier COMPONENT_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace("http").withName("listener").build();

  private final DefaultHttpListener listener;
  private final FlowConstruct flowConstruct;
  private final Executor workManager;
  private final ClassLoader executionClassLoader;

  HttpMessageProcessContext(final DefaultHttpListener listener, final FlowConstruct flowConstruct, final Executor workManager,
                            final ClassLoader executionClassLoader) {
    this.listener = listener;
    this.flowConstruct = flowConstruct;
    this.workManager = workManager;
    this.executionClassLoader = executionClassLoader;
  }

  @Override
  public boolean supportsAsynchronousProcessing() {
    return false;
  }

  @Override
  public MessageSource getMessageSource() {
    return listener;
  }

  @Override
  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  @Override
  public Executor getFlowExecutionExecutor() {
    return workManager;
  }

  @Override
  public TransactionConfig getTransactionConfig() {
    return null;
  }

  @Override
  public ClassLoader getExecutionClassLoader() {
    return executionClassLoader;
  }

  @Override
  public ComponentIdentifier getSourceIdentifier() {
    return COMPONENT_IDENTIFIER;
  }
}
