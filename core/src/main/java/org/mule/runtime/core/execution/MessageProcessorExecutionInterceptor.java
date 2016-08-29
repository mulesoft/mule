/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;

/**
 * Intercepts a MessageProcessor execution.
 */
public interface MessageProcessorExecutionInterceptor extends MuleContextAware, FlowConstructAware {

  public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException;
}
