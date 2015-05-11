/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.source;

/**
 * NonBlockingMessageSource's create instances of MuleEvent that contain a {@link org.mule.api.transport.ReplyToHandler}
 * allowing for a response or error to be returned asynchronously by a different thread freeing up the request thread.
 * <p/>
 * Implementations must support both blocking and non-blocking and therefore need to test the response {@link org.mule.api.MuleEvent}
 * returned when invoking {@link org.mule.api.processor.MessageProcessor#process(org.mule.api.MuleEvent)} on the listener.
 * If the event returned is an instance of {@link org.mule.NonBlockingVoidMuleEvent} then the {@link MessageSource} should wait for the
 * {@link org.mule.api.transport.ReplyToHandler} to be invoked before sending a response, otherwise the response should be
 * sent immediatly.
 * <p/>
 * <b>Note:</b> If {@link org.mule.execution.AsyncResponseFlowProcessingPhase} is used then non-blocking will be supported, it is still
 * necessary that the MessageSource implements this interface though.
 *
 * @since 3.7
 */
public interface NonBlockingMessageSource extends MessageSource
{

}
