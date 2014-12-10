/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.client;

import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.transport.ssl.api.TlsContextFactory;

/**
 * Default implementation for {@link org.mule.module.http.api.client.HttpRequestOptions}
 */
public class DefaultHttpRequestOptions implements HttpRequestOptions
{

    private final String method;
    private final Boolean followsRedirect;
    private final HttpStreamingType requestStreamingMode;
    private final HttpRequesterConfig requestConfig;
    private final boolean disableStatusCodeValidation;
    private final boolean disableParseResponse;
    private final Long responseTimeout;
    private final TlsContextFactory tlsContextFactory;

    public DefaultHttpRequestOptions(String method, Boolean followsRedirect, HttpStreamingType requestStreamingMode, HttpRequesterConfig requestConfig, boolean disableStatusCodeValidation, boolean disableParseResponse, TlsContextFactory tlsContextFactory, Long responseTimeout)
    {
        this.method = method;
        this.followsRedirect = followsRedirect;
        this.requestStreamingMode = requestStreamingMode;
        this.requestConfig = requestConfig;
        this.disableStatusCodeValidation = disableStatusCodeValidation;
        this.disableParseResponse = disableParseResponse;
        this.tlsContextFactory = tlsContextFactory;
        this.responseTimeout = responseTimeout;
    }

    @Override
    public String getMethod()
    {
        return method;
    }

    @Override
    public HttpStreamingType getRequestStreamingMode()
    {
        return requestStreamingMode;
    }

    @Override
    public boolean isParseResponseDisabled()
    {
        return disableParseResponse;
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
        return responseTimeout;
    }

    @Override
    public boolean isStatusCodeValidationDisabled()
    {
        return disableStatusCodeValidation;
    }

    @Override
    public TlsContextFactory getTlsContextFactory()
    {
        return tlsContextFactory;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof HttpRequestOptions))
        {
            return false;
        }

        DefaultHttpRequestOptions that = (DefaultHttpRequestOptions) o;

        if (disableStatusCodeValidation != that.isStatusCodeValidationDisabled())
        {
            return false;
        }
        if (followsRedirect == null ? that.isFollowsRedirect() != null : !followsRedirect.equals(that.isFollowsRedirect()))
        {
            return false;
        }
        if (method != null ? !method.equals(that.getMethod()) : that.getMethod() != null)
        {
            return false;
        }
        if (requestConfig != null ? !requestConfig.equals(that.getRequesterConfig()) : that.getRequesterConfig() != null)
        {
            return false;
        }
        if (requestStreamingMode == null ? that.requestStreamingMode != null : requestStreamingMode != that.requestStreamingMode)
        {
            return false;
        }
        if (disableParseResponse != that.isParseResponseDisabled())
        {
            return false;
        }

        if (tlsContextFactory == null ? that.tlsContextFactory != null : !tlsContextFactory.equals(that.tlsContextFactory))
        {
            return false;
        }

        if (disableParseResponse != that.isParseResponseDisabled())
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        final int hashcodePrimerNumber = 31;
        int result = method != null ? method.hashCode() : 0;
        result = hashcodePrimerNumber * result + (followsRedirect != null ? followsRedirect.hashCode() : 0);
        result = hashcodePrimerNumber * result + (requestStreamingMode != null ? requestStreamingMode.hashCode() : 0);
        result = hashcodePrimerNumber * result + (requestConfig != null ? requestConfig.hashCode() : 0);
        result = hashcodePrimerNumber * result + (tlsContextFactory != null ? tlsContextFactory.hashCode() : 0);
        result = hashcodePrimerNumber * result + (disableStatusCodeValidation ? 1 : 0);
        result = hashcodePrimerNumber * result + (disableParseResponse ? 1 : 0);
        return result;
    }
}
