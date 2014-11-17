/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.mule.module.http.internal.request.ResponseValidator;

public class HttpRequesterBuilder
{
    private static final String DEFAULT_HTTP_REQUEST_CONFIG_NAME = "_muleDefaultHttpRequestConfig";

    private final DefaultHttpRequester httpRequester;
    private final MuleContext muleContext;

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

    public HttpRequesterBuilder setMethod(String method)
    {
        httpRequester.setMethod(method);
        return this;
    }

    public HttpRequesterBuilder setUrl(String url)
    {
        httpRequester.setUrl(url);
        return this;
    }

    public HttpRequesterBuilder setConfig(DefaultHttpRequesterConfig config)
    {
        httpRequester.setConfig(config);
        return this;
    }

    public HttpRequesterBuilder setParseResponse(boolean parseResponse)
    {
        httpRequester.setParseResponse(Boolean.toString(parseResponse));
        return this;
    }

    public HttpRequesterBuilder setFollowRedirects(String followRedirects)
    {
        httpRequester.setFollowRedirects(followRedirects);
        return this;
    }

    public HttpRequesterBuilder setResponseValidator(ResponseValidator responseValidator)
    {
        httpRequester.setResponseValidator(responseValidator);
        return this;
    }

    public HttpRequester build() throws MuleException
    {
        try
        {
            if (httpRequester.getConfig() == null)
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
        catch (InitialisationException e)
        {
            throw new DefaultMuleException(e);
        }
    }


}
