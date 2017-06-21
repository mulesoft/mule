/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Intercepts a MessageProcessor execution.
 */
interface MessageProcessorExecutionInterceptor extends MuleContextAware, FlowConstructAware {

  Event execute(Processor messageProcessor, Event event) throws MessagingException;
}
