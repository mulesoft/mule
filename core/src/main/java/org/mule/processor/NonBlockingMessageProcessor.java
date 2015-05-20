/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.NonBlockingSupported;
import org.mule.api.processor.MessageProcessor;

/**
 * A {@link org.mule.processor.NonBlockingMessageProcessor} differs from other message processors in that it supports
 * both blocking and non-blocking processing.
 * <br />
 *
 * If the current {@link org.mule.api.MuleEvent} is not synchronous and a {@link org.mule.MessageExchangePattern} that
 * expects a response then the implementations of this message processor should return an instance of
 * {@link org.mule.NonBlockingVoidMuleEvent} and invoke the events {@link org.mule.api.transport.ReplyToHandler} when
 * a response is available or an exception errors.
 * <br />
 *
 * Alternatively if the {@link org.mule.api.MuleEvent} is synchronous, then the response should be returned and the
 * {@link org.mule.api.transport.ReplyToHandler} not used.
 *
 * @since 3.7
 */
public interface NonBlockingMessageProcessor extends MessageProcessor, NonBlockingSupported
{

}
