/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleException;
import org.mule.api.exception.MessagingExceptionHandler;

/**
 * Factory for outbound endpoint ready to be used in a pipeline.
 */
public interface OutboundEndpointExecutorFactory
{

    /**
     * Creates an outbound endpoint with it's own execution context which follows the outbound endpoint
     * execution model.
     *
     * When working with outbound endpoints, the execution of it may be done in the current thread or
     * a new thread depending on:
     *  - the outbound endpoint exchange pattern
     *  - the outbound endpoint connector dispatcher threading profile configuration
     *  - the existence of a transaction in the execution context
     *
     * @param outboundEndpoint outbound endpoint to execute
     * @param messagingExceptionHandler exception handler in case the outbound endpoint execution fails
     * @return an outbound endpoint with it's execution context configured
     * @throws MuleException
     */
    OutboundEndpoint getOutboundEndpointExecutor(OutboundEndpoint outboundEndpoint, MessagingExceptionHandler messagingExceptionHandler) throws MuleException;

}
