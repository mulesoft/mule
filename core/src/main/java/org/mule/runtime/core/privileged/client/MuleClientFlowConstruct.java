/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.client;

import static java.util.Optional.empty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.slf4j.Logger;

/**
 * Placeholder class which makes the default exception handler available.
 */
public final class MuleClientFlowConstruct extends AbstractComponent implements FlowConstruct, Disposable {

  private static final Logger LOGGER = getLogger(MuleClientFlowConstruct.class);

  MuleContext muleContext;
  FlowExceptionHandler exceptionHandler;

  public MuleClientFlowConstruct(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public String getName() {
    return "MuleClient";
  }

  @Override
  public String getUniqueIdString() {
    return muleContext.getUniqueIdString();
  }

  @Override
  public String getServerId() {
    return muleContext.getId();
  }

  @Override
  public FlowExceptionHandler getExceptionListener() {
    if (exceptionHandler == null) {
      exceptionHandler = muleContext.getDefaultErrorHandler(empty());
      try {
        initialiseIfNeeded(exceptionHandler, true, muleContext);
      } catch (InitialisationException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return exceptionHandler;
  }

  @Override
  public LifecycleState getLifecycleState() {
    return null;
  }

  @Override
  public FlowConstructStatistics getStatistics() {
    return null;
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  public MessageProcessorChain getMessageProcessorChain() {
    return null;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(exceptionHandler, LOGGER);
  }
}
