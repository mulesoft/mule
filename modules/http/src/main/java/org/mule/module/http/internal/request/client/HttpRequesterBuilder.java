/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.client;

import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.internal.request.SuccessStatusCodeValidator.NULL_VALIDATOR;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpRequestOperationConfig;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.util.ObjectNameHelper;

public class HttpRequesterBuilder implements HttpRequestOperationConfig<HttpRequesterBuilder>
{
    public static final String DEFAULT_HTTP_REQUEST_CONFIG_NAME = "_muleDefaultHttpRequestConfig";

    private final DefaultHttpRequester httpRequester;
    private final MuleContext muleContext;
    private TlsContextFactory tlsContextFactory;

    public HttpRequesterBuilder(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        httpRequester = new DefaultHttpRequester();
        httpRequester.setMuleContext(muleContext);
    }

    public HttpRequesterBuilder setHost(String host)
    {
        httpRequester.setHost(host);
        return this;
    }

    public HttpRequesterBuilder setPort(String port)
    {
        httpRequester.setPort(port);
        return this;
    }

    public HttpRequesterBuilder setPath(String path)
    {
        httpRequester.setPath(path);
        return this;
    }

    @Override
    public HttpRequesterBuilder method(String method)
    {
        httpRequester.setMethod(method);
        return this;
    }

    @Override
    public HttpRequesterBuilder enableFollowsRedirect()
    {
        httpRequester.setFollowRedirects(Boolean.TRUE.toString());
        return this;
    }

    @Override
    public HttpRequesterBuilder disableFollowsRedirect()
    {
        httpRequester.setFollowRedirects(Boolean.FALSE.toString());
        return this;
    }

    @Override
    public HttpRequesterBuilder requestStreamingMode(HttpStreamingType mode)
    {
        httpRequester.setRequestStreamingMode(mode.name());
        return this;
    }

    @Override
    public HttpRequesterBuilder requestConfig(HttpRequesterConfig requestConfig)
    {
        httpRequester.setConfig((DefaultHttpRequesterConfig) requestConfig);
        return this;
    }

    @Override
    public HttpRequesterBuilder disableStatusCodeValidation()
    {
        httpRequester.setResponseValidator(NULL_VALIDATOR);
        return this;
    }

    @Override
    public HttpRequesterBuilder disableParseResponse()
    {
        httpRequester.setParseResponse(Boolean.FALSE.toString());
        return this;
    }

    public HttpRequesterBuilder setUrl(String url)
    {
        httpRequester.setUrl(url);
        return this;
    }

    @Override
    public HttpRequesterBuilder responseTimeout(long responseTimeout)
    {
        this.httpRequester.setResponseTimeout(String.valueOf(responseTimeout));
        return this;
    }

    public DefaultHttpRequester build() throws MuleException
    {
        if (httpRequester.getConfig() != null && tlsContextFactory != null)
        {
            throw new IllegalStateException("The request config and the TLS context factory cannot be configured at the same time");
        }
        if (tlsContextFactory != null)
        {
            DefaultHttpRequesterConfig requesterConfig = new DefaultHttpRequesterConfig();
            requesterConfig.setTlsContext(tlsContextFactory);
            requesterConfig.setProtocol(HTTPS);
            muleContext.getRegistry().registerObject(new ObjectNameHelper(muleContext).getUniqueName("auto-generated-request-config"), requesterConfig);
            httpRequester.setConfig(requesterConfig);
        }
        else if (httpRequester.getConfig() == null)
        {
            DefaultHttpRequesterConfig requestConfig = muleContext.getRegistry().get(DEFAULT_HTTP_REQUEST_CONFIG_NAME);

            if (requestConfig == null)
            {
                requestConfig = new DefaultHttpRequesterConfig();
                muleContext.getRegistry().registerObject(DEFAULT_HTTP_REQUEST_CONFIG_NAME, requestConfig);
            }

            httpRequester.setConfig(requestConfig);
        }

        httpRequester.initialise();
        return httpRequester;
    }

    public HttpRequesterBuilder setOperationConfig(HttpRequestOptions operationOptions)
    {
        if (operationOptions.getMethod() != null)
        {
            httpRequester.setMethod(operationOptions.getMethod());
        }
        if (operationOptions.isFollowsRedirect() != null && !operationOptions.isFollowsRedirect())
        {
            httpRequester.setFollowRedirects(operationOptions.isFollowsRedirect().toString());
        }
        if (operationOptions.getRequestStreamingMode() != null)
        {
            httpRequester.setRequestStreamingMode(operationOptions.getRequestStreamingMode().name());
        }
        if (operationOptions.getRequesterConfig() != null)
        {
            httpRequester.setConfig((DefaultHttpRequesterConfig) operationOptions.getRequesterConfig());
        }
        if (operationOptions.isStatusCodeValidationDisabled())
        {
            httpRequester.setResponseValidator(NULL_VALIDATOR);
        }
        if (operationOptions.isParseResponseDisabled())
        {
            httpRequester.setParseResponse(Boolean.FALSE.toString());
        }
        if (operationOptions.getResponseTimeout() != null)
        {
            httpRequester.setResponseTimeout(String.valueOf(operationOptions.getResponseTimeout()));
        }
        this.tlsContextFactory = operationOptions.getTlsContextFactory();
        return this;
    }
}
