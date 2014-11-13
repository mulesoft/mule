/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.listener;

import org.mule.api.MuleException;
import org.mule.api.source.MessageSource;

/**
 * Represents an listener for http requests.
 */
public interface HttpListener extends MessageSource
{

    /**
     * @return the config for this listener.
     */
    public HttpListenerConfig getConfig();

    /**
     * Stops this listener. Any subsequent call made to the listener will return 503.
     *
     * @throws MuleException if there's was a problem stopping the listener
     */
    public void stop() throws MuleException;

    /**
     * Starts an stopped listener. The listener will start to accept requests again.
     *
     * Doing start of an started listener has no effect.
     *
     * @throws MuleException
     */
    public void start() throws MuleException;

    /**
     * Get rid of this listener. Subsequent call made to the listener will return 404 unless
     * there's another listener which path matches the request criteria.
     */
    public void dispose();

}
