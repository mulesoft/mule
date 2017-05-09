/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.client;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

/**
 * Placeholder class which makes the default exception handler available.
 */
public class MuleClientFlowConstruct implements FlowConstruct {

  MuleContext muleContext;

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
  public MessagingExceptionHandler getExceptionListener() {
    return muleContext.getDefaultErrorHandler();
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
}
