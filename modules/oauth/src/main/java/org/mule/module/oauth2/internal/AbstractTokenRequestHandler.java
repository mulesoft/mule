/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.module.http.api.HttpConstants.Protocols;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpRequesterConfigBuilder;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTokenRequestHandler implements MuleContextAware, Disposable
{

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private MuleContext muleContext;
    private String refreshTokenWhen = OAuthConstants.DEFAULT_REFRESH_TOKEN_WHEN_EXPRESSION;
    private String tokenUrl;
    private HttpRequesterConfig httpRequesterConfig;
    private HttpRequestOptions httpRequestOptions = HttpRequestOptionsBuilder.newOptions().method(POST.name()).disableStatusCodeValidation().build();
    private TlsContextFactory tlsContextFactory;

    /**
     * @param refreshTokenWhen expression to use to determine if the response from a request to the API requires a new token
     */
    public void setRefreshTokenWhen(String refreshTokenWhen)
    {
        this.refreshTokenWhen = refreshTokenWhen;
    }

    public String getRefreshTokenWhen()
    {
        return refreshTokenWhen;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setTokenUrl(String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

    public void buildHttpRequestOptions(final TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig) throws MuleException
    {
        if (httpRequesterConfig == null)
        {
            Protocols protocol = tlsContextFactory != null ? HTTPS : HTTP;
            httpRequesterConfig = new HttpRequesterConfigBuilder(muleContext).setProxyConfig(proxyConfig).setProtocol(protocol).setTlsContext(tlsContextFactory).build();
            httpRequestOptions = HttpRequestOptionsBuilder.newOptions().method(POST.name()).disableStatusCodeValidation().requestConfig(httpRequesterConfig).build();
        }
    }

    protected MuleEvent invokeTokenUrl(final MuleEvent event) throws MuleException, TokenUrlResponseException
    {
        event.setMessage(muleContext.getClient().send(tokenUrl, event.getMessage(), httpRequestOptions));
        if (event.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY) >= BAD_REQUEST.getStatusCode())
        {
            throw new TokenUrlResponseException(event);
        }
        return event;
    }

    protected String getTokenUrl()
    {
        return tokenUrl;
    }

    protected class TokenUrlResponseException extends Exception
    {
        private MuleEvent tokenUrlResponse;

        public TokenUrlResponseException(final MuleEvent tokenUrlResponse)
        {
            this.tokenUrlResponse = tokenUrlResponse;
        }

        public MuleEvent getTokenUrlResponse()
        {
            return tokenUrlResponse;
        }
    }
    
    @Override
    public void dispose()
    {
        try
        {
            httpRequesterConfig.stop();
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
        disposeIfNeeded(httpRequesterConfig, logger);
    }
}
