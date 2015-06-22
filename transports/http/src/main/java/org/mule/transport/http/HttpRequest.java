/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;


import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.ContentLengthInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.NameValuePair;

/**
 * A http request wrapper
 */
public class HttpRequest
{
    private RequestLine requestLine = null;
    private HeaderGroup headers = new HeaderGroup();
    private InputStream entity = null;
    private String defaultEncoding;

    public HttpRequest(final RequestLine requestLine, final Header[] headers, String defaultEncoding) throws IOException
    {
        this(requestLine, headers, null, defaultEncoding);
    }

    public HttpRequest(final RequestLine requestLine, final Header[] headers, final InputStream content, String defaultEncoding)
            throws IOException
    {
        super();
        if (requestLine == null)
        {
            throw new IllegalArgumentException("Request line may not be null");
        }
        this.requestLine = requestLine;
        this.defaultEncoding = defaultEncoding;
        if (headers != null)
        {
            this.headers.setHeaders(headers);
        }
        if (content != null && shouldProcessContent())
        {
            Header contentLength = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_LENGTH);
            Header transferEncoding = this.headers.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
            InputStream in = content;
            if (transferEncoding != null)
            {
                if (transferEncoding.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1)
                {
                    in = new ChunkedInputStream(in);
                }
            }
            else if (contentLength != null)
            {
                long len = getContentLength();
                if (len >= 0)
                {
                    in = new ContentLengthInputStream(in, len);
                }
            }
            this.entity = in;
        }
    }

    private boolean shouldProcessContent()
    {
        String methodName = requestLine.getMethod();
        if (HttpConstants.METHOD_POST.equalsIgnoreCase(methodName) ||
            HttpConstants.METHOD_PUT.equalsIgnoreCase(methodName) ||
            HttpConstants.METHOD_PATCH.equalsIgnoreCase(methodName))
        {
            return true;
        }
        else if (HttpConstants.METHOD_GET.equalsIgnoreCase(methodName) ||
                 HttpConstants.METHOD_DELETE.equalsIgnoreCase(methodName))
        {
            Header contentLength = headers.getFirstHeader(HttpConstants.HEADER_CONTENT_LENGTH);
            Header transferEncoding = headers.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
            return transferEncoding != null || (contentLength != null && Integer.valueOf(contentLength.getValue()) > 0);
        }
        return false;
    }

    public RequestLine getRequestLine()
    {
        return this.requestLine;
    }

    public void setRequestLine(final RequestLine requestline)
    {
        if (requestline == null)
        {
            throw new IllegalArgumentException("Request line may not be null");
        }
        this.requestLine = requestline;
    }

    public boolean containsHeader(final String name)
    {
        return this.headers.containsHeader(name);
    }

    public Header[] getHeaders()
    {
        return this.headers.getAllHeaders();
    }

    public Header getFirstHeader(final String s)
    {
        return this.headers.getFirstHeader(s);
    }

    public void removeHeaders(final String s)
    {
        if (s == null)
        {
            return;
        }
        Header[] headersToRemove = this.headers.getHeaders(s);
        for (int i = 0; i < headersToRemove.length; i++)
        {
            this.headers.removeHeader(headersToRemove[i]);
        }
    }

    public void addHeader(final Header header)
    {
        if (header == null)
        {
            return;
        }
        this.headers.addHeader(header);
    }

    public void setHeader(final Header header)
    {
        if (header == null)
        {
            return;
        }
        removeHeaders(header.getName());
        addHeader(header);
    }

    public Iterator<?> getHeaderIterator()
    {
        return this.headers.getIterator();
    }

    public String getContentType()
    {
        Header contenttype = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_TYPE);
        if (contenttype != null)
        {
            return contenttype.getValue();
        }
        else
        {
            return HttpConstants.DEFAULT_CONTENT_TYPE;
        }
    }

    public String getCharset()
    {
        String charset = null;
        Header contenttype = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_TYPE);
        if (contenttype != null)
        {
            HeaderElement values[] = contenttype.getElements();
            if (values.length == 1)
            {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null)
                {
                    charset = param.getValue();
                }
            }
        }
        if (charset != null)
        {
            return charset;
        }
        else
        {
            return defaultEncoding;
        }
    }

    public long getContentLength()
    {
        Header contentLength = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_LENGTH);
        if (contentLength != null)
        {
            try
            {
                return Long.parseLong(contentLength.getValue());
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }
        else
        {
            return -1;
        }
    }

    public InputStream getBody()
    {
        return this.entity;
    }

    public byte[] getBodyBytes() throws IOException
    {
        InputStream in = getBody();
        if (in != null)
        {
            return IOUtils.toByteArray(in);
        }
        else
        {
            return null;
        }
    }

    public String getBodyString() throws IOException
    {
        byte[] raw = getBodyBytes();
        if (raw != null)
        {
            return new String(raw, getCharset());
        }
        else
        {
            return null;
        }
    }

    public String getUrlWithoutParams()
    {
        return this.requestLine.getUrlWithoutParams();
    }

}
