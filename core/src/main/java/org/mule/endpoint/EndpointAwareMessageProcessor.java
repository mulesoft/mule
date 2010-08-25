/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;

/**
 * A message processor into which an endpoint needs to be injected.
 */
public interface EndpointAwareMessageProcessor extends MessageProcessor
{
    /**
     * Inject the endpoint.  Note that this might return a different MessageProcessor, or null if no
     * MessageProcessor is needed for the given endpoint type
     */
    MessageProcessor injectEndpoint(ImmutableEndpoint ep);
}
