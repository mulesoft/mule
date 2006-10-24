/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.ContentLengthInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mule.MuleManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * A http request wrapper
 */
public class HttpRequest
{

    private RequestLine requestLine = null;
    private HeaderGroup headers = new HeaderGroup();
    private InputStream entity = null;

    public HttpRequest()
    {
        super();
    }

    public HttpRequest(final RequestLine requestLine, final Header[] headers, final InputStream content)
        throws IOException
    {
        super();
        if (requestLine == null)
        {
            throw new IllegalArgumentException("Request line may not be null");
        }
        this.requestLine = requestLine;
        if (headers != null)
        {
            this.headers.setHeaders(headers);
        }
        if (content != null)
        {
            // only PUT and POST have content
            String methodname = requestLine.getMethod();
            if (HttpConstants.METHOD_POST.equalsIgnoreCase(methodname)
                || HttpConstants.METHOD_PUT.equalsIgnoreCase(methodname))
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
    }

    public HttpRequest(final RequestLine requestLine, final Header[] headers) throws IOException
    {
        this(requestLine, headers, null);
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
        Header[] headers = this.headers.getHeaders(s);
        for (int i = 0; i < headers.length; i++)
        {
            this.headers.removeHeader(headers[i]);
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

    public Iterator getHeaderIterator()
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
            return MuleManager.getConfiguration().getEncoding();
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
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            IOUtils.copy(in, buffer);
            return buffer.toByteArray();
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

}
