/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.client;

import org.mule.api.client.AbstractBaseOptionsBuilder;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.internal.request.client.DefaultHttpRequestOptions;
import org.mule.transport.ssl.api.TlsContextFactory;

/**
 * Builder object for {@link org.mule.module.http.api.client.HttpRequestOptions}
 */
public class HttpRequestOptionsBuilder extends AbstractBaseOptionsBuilder<HttpRequestOptionsBuilder, HttpRequestOptions>
{

    private String method;
    private Boolean followsRedirect;
    private HttpStreamingType requestStreamingMode;
    private HttpRequesterConfig requestConfig;
    private boolean disableStatusCodeValidation;
    private boolean disableParseResponse;
    private TlsContextFactory tlsContextFactory;

    private HttpRequestOptionsBuilder()
    {
    }

    /**
     * @param method HTTP method for the HTTP request
     * @return the builder
     */
    public HttpRequestOptionsBuilder method(String method)
    {
        this.method = method;
        return this;
    }

    /**
     * Configures the requester to follows redirects
     *
     * @return the builder
     */
    public HttpRequestOptionsBuilder enableFollowsRedirect()
    {
        this.followsRedirect = true;
        return this;
    }

    /**
     * Configures the requester to not follow redirects
     *
     * @return the builder
     */
    public HttpRequestOptionsBuilder disableFollowsRedirect()
    {
        this.followsRedirect = false;
        return this;
    }

    /**
     * Configures the streaming mode for sending the HTTP request.
     *
     * @return the builder
     */
    public HttpRequestOptionsBuilder requestStreamingMode(HttpStreamingType mode)
    {
        this.requestStreamingMode = mode;
        return this;
    }

    /**
     * @param requestConfig the config to use for the requester
     * @return the builder
     */
    public HttpRequestOptionsBuilder requestConfig(HttpRequesterConfig requestConfig)
    {
        this.requestConfig = requestConfig;
        return this;
    }

    /**
     * Disables the status code validation for the response
     *
     * @return the builder
     */
    public HttpRequestOptionsBuilder disableStatusCodeValidation()
    {
        this.disableStatusCodeValidation = true;
        return this;
    }

    public HttpRequestOptionsBuilder disableParseResponse()
    {
        disableParseResponse = true;
        return this;
    }

    public HttpRequestOptionsBuilder tlsContextFactory(TlsContextFactory tlsContextFactory)
    {
        this.tlsContextFactory = tlsContextFactory;
        return this;
    }

    /**
     * @return a {@link org.mule.module.http.api.client.HttpRequestOptions} instance
     */
    @Override
    public HttpRequestOptions build()
    {
        return new DefaultHttpRequestOptions(method, followsRedirect, requestStreamingMode, requestConfig, disableStatusCodeValidation, disableParseResponse, tlsContextFactory, getResponseTimeout());
    }

    /**
     * @return factory method for creating a new builder.
     */
    public static HttpRequestOptionsBuilder newOptions()
    {
        return new HttpRequestOptionsBuilder();
    }

}
