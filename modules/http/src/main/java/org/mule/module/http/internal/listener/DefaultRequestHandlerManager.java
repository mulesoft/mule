/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

public class DefaultRequestHandlerManager implements RequestHandlerManager
{

    private final HttpListenerRegistry.Path requestHandlerOwner;
    private final HttpListenerRegistry.RequestHandlerMatcherPair requestHandlerMatcherPair;
    private final HttpListenerRegistry.ServerAddressRequestHandlerRegistry requestHandlerRegistry;

    public DefaultRequestHandlerManager(HttpListenerRegistry.Path requestHandlerOwner,
                                        HttpListenerRegistry.RequestHandlerMatcherPair requestHandlerMatcherPair,
                                        HttpListenerRegistry.ServerAddressRequestHandlerRegistry serverAddressRequestHandlerRegistry)
    {
        this.requestHandlerOwner = requestHandlerOwner;
        this.requestHandlerMatcherPair = requestHandlerMatcherPair;
        this.requestHandlerRegistry = serverAddressRequestHandlerRegistry;
    }

    @Override
    public void stop()
    {
        requestHandlerMatcherPair.setIsRunning(false);
    }

    @Override
    public void start()
    {
        requestHandlerMatcherPair.setIsRunning(true);
    }

    @Override
    public void dispose()
    {
        requestHandlerOwner.removeRequestHandlerMatcherPair(requestHandlerMatcherPair);
        requestHandlerRegistry.removeRequestHandler(requestHandlerMatcherPair.getRequestMatcher());
    }
}
