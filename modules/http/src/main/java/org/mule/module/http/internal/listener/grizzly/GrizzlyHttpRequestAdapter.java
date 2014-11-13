/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.api.MuleRuntimeException;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.HttpProtocol;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.util.Collection;

import javax.servlet.http.Part;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Protocol;

public class GrizzlyHttpRequestAdapter implements HttpRequest
{

    private final HttpRequestPacket grizzlyRequest;
    private final InputStream grizzlyContent;
    private final int contentLength;
    private HttpProtocol protocol;
    private String uri;
    private String path;
    private String method;
    private HttpEntity body;
    private ParameterMap headers;

    public GrizzlyHttpRequestAdapter(HttpRequestPacket request, InputStream content, int contentLength)
    {
        this.grizzlyRequest = request;
        this.grizzlyContent = content;
        this.contentLength = contentLength;
    }

    @Override
    public HttpProtocol getProtocol()
    {
        if (this.protocol == null)
        {
            this.protocol = grizzlyRequest.getProtocol().equals(Protocol.HTTP_1_0) ? HttpProtocol.HTTP_1_0 : HttpProtocol.HTTP_1_1;
        }
        return this.protocol;
    }

    @Override
    public String getPath()
    {
        if (this.path == null)
        {
            String uri = getUri();
            this.path = HttpParser.extractPath(uri);
        }
        return this.path;
    }

    @Override
    public String getMethod()
    {
        if (this.method == null)
        {
            this.method = grizzlyRequest.getMethod().getMethodString();
        }
        return this.method;
    }

    @Override
    public Collection<String> getHeaderNames()
    {
        if (this.headers == null)
        {
            initializeHeaders();
        }
        return this.headers.keySet();
    }

    @Override
    public String getHeaderValue(String headerName)
    {
        if (this.headers == null)
        {
            initializeHeaders();
        }
        return this.headers.get(headerName.toLowerCase());
    }

    @Override
    public Collection<String> getHeaderValues(String headerName)
    {
        if (this.headers == null)
        {
            initializeHeaders();
        }
        return this.headers.getAsList(headerName.toLowerCase());
    }

    private void initializeHeaders()
    {
        this.headers = new ParameterMap();
        for (String grizzlyHeaderName : grizzlyRequest.getHeaders().names())
        {
            final Iterable<String> headerValues = grizzlyRequest.getHeaders().values(grizzlyHeaderName);
            for (String headerValue : headerValues)
            {
                this.headers.put(grizzlyHeaderName, headerValue);
            }
        }
    }

    @Override
    public HttpEntity getEntity()
    {
        try
        {
            if (this.body == null)
            {
                final String contentTypeValue = getHeaderValue(HttpHeaders.Names.CONTENT_TYPE);
                if (contentTypeValue != null && contentTypeValue.contains("multipart"))
                {

                    final Collection<Part> parts = HttpParser.parseMultipartContent(grizzlyContent, contentTypeValue);
                    this.body = new MultipartHttpEntity(parts);
                }
                else if (getHeaderValue(HttpHeaders.Names.TRANSFER_ENCODING) != null)
                {
                    this.body = new InputStreamHttpEntity(grizzlyContent);
                }
                else if (contentLength > 0)
                {
                    this.body = new InputStreamHttpEntity(contentLength, grizzlyContent);
                }
                else
                {
                    this.body = new EmptyHttpEntity();
                }
            }
            return this.body;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public String getUri()
    {
        if (this.uri == null)
        {
            this.uri = grizzlyRequest.getRequestURI() + (StringUtils.isEmpty(grizzlyRequest.getQueryString()) ? "" : "?" + grizzlyRequest.getQueryString());
        }
        return this.uri;
    }

    @Override
    public InputStreamHttpEntity getInputStreamEntity()
    {
        if (this.grizzlyContent == null)
        {
            return null;
        }
        if (getHeaderValue(HttpHeaders.Names.TRANSFER_ENCODING) != null)
        {
            return new InputStreamHttpEntity(grizzlyContent);
        }
        if (contentLength > 0)
        {
            return new InputStreamHttpEntity(contentLength, grizzlyContent);
        }
        return null;
    }
}
