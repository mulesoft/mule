/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.client;

import org.mule.api.client.BaseOptionsBuilder;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.internal.HttpStreamingType;

/**
 * Builder object for {@link org.mule.module.http.api.client.HttpRequestOptions}
 */
public class HttpRequestOptionsBuilder extends BaseOptionsBuilder<HttpRequestOptionsBuilder, HttpRequestOptions>
{

    private String method;
    private Boolean followsRedirect;
    private HttpStreamingType requestStreamingMode;
    private HttpRequesterConfig requestConfig;
    private boolean disableStatusCodeValidation;

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
     * calling this method will make the HTTP request to always be sent using streaming (transfer-encoding chunk).
     *
     * @return the builder
     */
    public HttpRequestOptionsBuilder alwaysStreamRequest()
    {
        this.requestStreamingMode = requestStreamingMode.ALWAYS;
        return this;
    }

    /**
     * calling this method will make the HTTP request never to be sent using streaming (transfer-encoding chunk).
     *
     * @return the builder
     */
    public HttpRequestOptionsBuilder neverStreamRequest()
    {
        this.requestStreamingMode = requestStreamingMode.NEVER;
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

    /**
     * @return a {@link org.mule.module.http.api.client.HttpRequestOptions} instance
     */
    @Override
    public HttpRequestOptions build()
    {
        return new HttpRequestOptions()
        {
            @Override
            public String getMethod()
            {
                return method;
            }

            public boolean alwaysStreamRequest()
            {
                return HttpStreamingType.ALWAYS.equals(requestStreamingMode);
            }

            public boolean neverStreamRequest()
            {
                return HttpStreamingType.NEVER.equals(requestStreamingMode);
            }

            @Override
            public HttpRequesterConfig getRequesterConfig()
            {
                return requestConfig;
            }

            @Override
            public Boolean isFollowsRedirect()
            {
                return followsRedirect;
            }

            @Override
            public Long getResponseTimeout()
            {
                return HttpRequestOptionsBuilder.this.getResponseTimeout();
            }

            @Override
            public boolean isStatusCodeValidationDisabled()
            {
                return disableStatusCodeValidation;
            }

        };
    }

    /**
     * @return factory method for creating a new builder.
     */
    public static HttpRequestOptionsBuilder newOptions()
    {
        return new HttpRequestOptionsBuilder();
    }


}
