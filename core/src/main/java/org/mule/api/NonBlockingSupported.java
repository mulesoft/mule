/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

/**
 * Marker interface for those {@link org.mule.api.processor.InterceptingMessageProcessor} and
 * {@link org.mule.api.processor.MessageRouter} implementations that support non-blocking processing.
 * <br />
 *
 * Unsupported implementations will cause Mule to fall back to synchronous blocking processing.
 * <br />
 *
 * Supporting non blocking means that
 * i) This components does not require synchronous processing
 * ii) That any response processing (processing done on the result after invoking a child/next
 * {@link org.mule.api.processor.MessageProcessor} will also be processed when running
 * non-blocking.
 * <br />
 *
 * One easy way of supporting non-blocking if a components needs to do response processing is to extend
 * {@link org.mule.processor.AbstractRequestResponseMessageProcessor}
 * <p/>
 * Note: All standard {@link org.mule.api.processor.MessageProcessor}
 * implementations that aren't one of the two types mentioned above are implicitly supported.
 *
 * @since 3.7
 */
public interface NonBlockingSupported
{

}
