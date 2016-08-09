/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.connector;

/**
 * Marker interface for {@link ReplyToHandler}s used for non-blocking callbacks rather than for processing the ReplyTo of a
 * {@link org.mule.runtime.core.MessageExchangePattern#ONE_WAY} Flow.
 */
public interface NonBlockingReplyToHandler extends ReplyToHandler {

}
