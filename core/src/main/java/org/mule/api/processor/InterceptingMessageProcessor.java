/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.api.MuleEvent;
import org.mule.api.source.MessageSource;

/**
 * <p>
 * Processes {@link MuleEvent}'s intercepting another listener
 * {@link MessageProcessor}. It is the InterceptingMessageProcessor's responsibility
 * to invoke the next {@link MessageProcessor}.
 * </p>
 * Although not normal, it is valid for the <i>listener</i> MessageProcessor to be
 * <i>null</i> and implementations should handle this case.
 * 
 * @since 3.0
 */
public interface InterceptingMessageProcessor extends MessageProcessor, MessageSource
{
}
