/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class HttpRequestBuilder
{

    private String path;
    private String uri;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private HttpEntity entity;

    public HttpRequestBuilder setUri(String uri)
    {
        this.path = HttpParser.extractPath(uri.toString());
        this.uri = uri;
        return this;
    }

    public HttpRequestBuilder setMethod(String method)
    {
        this.method = method;
        return this;
    }

    public HttpRequestBuilder setHeaders(Map headers)
    {
        this.headers = headers;
        return this;
    }

    public HttpRequestBuilder addHeader(String headerName, String headerValue)
    {
        this.headers.put(headerName, headerValue);
        return this;
    }

    public HttpRequestBuilder removeHeader(String headerName)
    {
        this.headers.remove(headerName);
        return this;
    }

    // TODO: MULE-8045 Refactor MuleEventToHttpRequest so that this is getter is not needed.
    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public HttpRequestBuilder setQueryParams(Map queryParams)
    {
        this.queryParams = queryParams;
        return this;
    }

    public HttpRequestBuilder setEntity(HttpEntity entity)
    {
        this.entity = entity;
        return this;
    }

    public HttpRequest build()
    {
        return new DefaultHttpRequest(uri, path, method, (headers == null) ? null : ImmutableMap.copyOf(headers), queryParams == null ? queryParams : ImmutableMap.copyOf(queryParams), entity);

    }

}
