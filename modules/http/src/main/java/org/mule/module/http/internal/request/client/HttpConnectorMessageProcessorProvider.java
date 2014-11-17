/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.client;

import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.internal.request.SuccessStatusCodeValidator.alwaysSuccessValidator;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.client.Options;
import org.mule.api.connector.ConnectorOperationProvider;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpRequesterBuilder;
import org.mule.module.http.internal.HttpStreamingType;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;

/**
 * Provider for operations of the HTTP module.
 */
public class HttpConnectorMessageProcessorProvider implements ConnectorOperationProvider, MuleContextAware
{

    private MuleContext muleContext;

    @Override
    public boolean supportsUrl(String url)
    {
        return url.startsWith(HTTP) || url.startsWith(HTTPS);
    }

    @Override
    public MessageProcessor getMessageProcessor(String url) throws MuleException
    {
        if (muleContext.getConfiguration().useHttpTransportByDefault())
        {
            return null;
        }
        return new HttpRequesterBuilder(muleContext).setUrl(url).build();
    }

    @Override
    public MessageProcessor getMessageProcessor(String url, Options options) throws MuleException
    {
        if (muleContext.getConfiguration().useHttpTransportByDefault())
        {
            return null;
        }
        final HttpRequesterBuilder httpRequesterBuilder = new HttpRequesterBuilder(muleContext).setUrl(url);
        if (options.getResponseTimeout() != null)
        {
            httpRequesterBuilder.setResponseTimeout(options.getResponseTimeout());
        }
        else
        {
            httpRequesterBuilder.setResponseTimeout(muleContext.getConfiguration().getDefaultResponseTimeout());
        }
        if (options instanceof HttpRequestOptions)
        {
            HttpRequestOptions httpRequestOptions = (HttpRequestOptions) options;
            if (httpRequestOptions.getMethod() != null)
            {
                httpRequesterBuilder.setMethod(httpRequestOptions.getMethod());
            }
            if (httpRequestOptions.isFollowsRedirect() != null)
            {
                httpRequesterBuilder.setFollowRedirects(httpRequestOptions.isFollowsRedirect().toString());
            }
            if (httpRequestOptions.alwaysStreamRequest())
            {
                httpRequesterBuilder.setRequestStreamingMode(HttpStreamingType.ALWAYS.name());
            }
            else if (httpRequestOptions.neverStreamRequest())
            {
                httpRequesterBuilder.setRequestStreamingMode(HttpStreamingType.NEVER.name());
            }
            if (httpRequestOptions.getRequesterConfig() != null)
            {
                httpRequesterBuilder.setConfig((DefaultHttpRequesterConfig) httpRequestOptions.getRequesterConfig());
            }
            if (httpRequestOptions.isStatusCodeValidationDisabled())
            {
                httpRequesterBuilder.setResponseValidator(alwaysSuccessValidator());
            }
        }
        return httpRequesterBuilder.build();
    }

    @Override
    public MessageProcessor getOneWayMessageProcessor(String url) throws MuleException
    {
        if (muleContext.getConfiguration().useHttpTransportByDefault())
        {
            return null;
        }
        return new OneWayHttpRequester(getMessageProcessor(url));
    }

    @Override
    public MessageProcessor getOneWayMessageProcessor(String url, Options options) throws MuleException
    {
        if (muleContext.getConfiguration().useHttpTransportByDefault())
        {
            return null;
        }
        return new OneWayHttpRequester(getMessageProcessor(url, options));
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
