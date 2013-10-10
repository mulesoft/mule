/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
