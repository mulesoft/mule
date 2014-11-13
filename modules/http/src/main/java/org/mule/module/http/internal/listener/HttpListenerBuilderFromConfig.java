/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.util.StringUtils;

public class HttpListenerBuilderFromConfig
{

    private final HttpListenerConfig listenerConfig;
    private final MuleContext muleContext;
    private final DefaultHttpListener listener;

    public HttpListenerBuilderFromConfig(final HttpListenerConfig listenerConfig, final MuleContext muleContext)
    {
        this.listenerConfig = listenerConfig;
        this.listener = new DefaultHttpListener();
        this.muleContext = muleContext;
    }

    public HttpListenerBuilderFromConfig setPath(final String path)
    {
        listener.setPath(path);
        return this;
    }

    public HttpListenerBuilderFromConfig setAllowedMethods(String[] allowedMethods)
    {
        listener.setAllowedMethods(StringUtils.join(allowedMethods, ","));
        return this;
    }

    public HttpListener build() throws MuleException
    {
        this.listener.start();
        return listener;
    }

}
