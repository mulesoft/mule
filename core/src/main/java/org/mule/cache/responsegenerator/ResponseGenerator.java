/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cache.responsegenerator;

import org.mule.api.MuleEvent;

/**
 * Generates {@link MuleEvent} responses from an request event and a previously
 * cached response.
 * <p/>
 * There is no constraint on the way the response is generated, it could be the
 * same cached response, or could be a new instance with different properties.
 */
public interface ResponseGenerator
{

    /**
     * Generates a response event from a request and a cached response for the
     * request.
     *
     * @param request        the request event
     * @param cachedResponse the cached response for the request
     * @return a response event generated form the passed information. Could
     *         be null.
     */
    MuleEvent create(MuleEvent request, MuleEvent cachedResponse);
}
