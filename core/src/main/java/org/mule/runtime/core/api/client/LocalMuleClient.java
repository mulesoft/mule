/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.client;

import org.mule.runtime.core.api.MuleException;

/**
 * Extends {@link MuleClient} adding methods that allow the use of an endpoint
 * instance.
 */
public interface LocalMuleClient extends MuleClient
{

    /**
     * Will register the specified processor as a listener for the inbound endpoint.
     * This may be implemented by subscription or polling depending on the transport
     * implementation
     * 
     * @param endpoint
     * @param processor
     * @throws MuleException
     */
    // void receive(InboundEndpoint endpoint, MessageProcessor processor) throws
    // MuleException;

}
