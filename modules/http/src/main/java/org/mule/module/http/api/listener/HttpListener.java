/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.listener;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.source.MessageSource;
import org.mule.api.source.NonBlockingMessageSource;

/**
 * Represents an listener for HTTP requests.
 */
public interface HttpListener extends NonBlockingMessageSource, Lifecycle
{

    /**
     * @return the config for this listener.
     */
    public HttpListenerConfig getConfig();

    /**
     * Stops this listener. Any subsequent call made to the listener will return {@link org.mule.module.http.internal.listener.ServiceTemporarilyUnavailableListenerRequestHandler#SERVICE_TEMPORARILY_UNAVAILABLE_STATUS_CODE}.
     *
     * @throws MuleException if there's was a problem stopping the listener
     */
    public void stop() throws MuleException;

    /**
     * Starts an stopped listener. The listener will start to accept requests again.
     *
     * @throws MuleException
     */
    public void start() throws MuleException;

    /**
     * Get rid of this listener. Subsequent call made to the listener will return {@link org.mule.module.http.internal.listener.NoListenerRequestHandler#RESOURCE_NOT_FOUND_STATUS_CODE} unless
     * there's another listener which path matches the request criteria.
     */
    public void dispose();

    /**
     * @return the path in which this listener is listening for incoming requests
     */
    public String getPath();

    /**
     * @return the http methods that this listener can process.
     */
    public String[] getAllowedMethods();

}
