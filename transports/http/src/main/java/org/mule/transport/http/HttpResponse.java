/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.NullPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.ContentLengthInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.StatusLine;

/**
 * A generic HTTP response wrapper.
 */
public class HttpResponse
{

    public static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";

    private HttpVersion ver = HttpVersion.HTTP_1_1;
    private int statusCode = HttpStatus.SC_OK;
    private String phrase = HttpStatus.getStatusText(HttpStatus.SC_OK);
    private HeaderGroup headers = new HeaderGroup();
    private boolean keepAlive = false;
    private boolean disableKeepAlive = false;
    private String fallbackCharset = DEFAULT_CONTENT_CHARSET;
    private OutputHandler outputHandler;

    public HttpResponse()
    {
        super();
    }

    public HttpResponse(final StatusLine statusline, final Header[] headers, final InputStream content)
        throws IOException
    {
        super();
        if (statusline == null)
        {
            throw new IllegalArgumentException("Status line may not be null");
        }
        setStatusLine(HttpVersion.parse(statusline.getHttpVersion()), statusline.getStatusCode(),
            statusline.getReasonPhrase());
        setHeaders(headers);
        if (content != null)
        {
            InputStream in = content;
            Header contentLength = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_LENGTH);
            Header transferEncoding = this.headers.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);

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
        }
    }

    public void setStatusLine(final HttpVersion ver, int statuscode, final String phrase)
    {
        if (ver == null)
        {
            throw new IllegalArgumentException("HTTP version may not be null");
        }
        if (statuscode <= 0)
        {
            throw new IllegalArgumentException("Status code may not be negative or zero");
        }
        this.ver = ver;
        this.statusCode = statuscode;
        if (phrase != null)
        {
            this.phrase = phrase;
        }
        else
        {
            this.phrase = HttpStatus.getStatusText(statuscode);
        }
    }

    public void setStatusLine(final HttpVersion ver, int statuscode)
    {
        setStatusLine(ver, statuscode, null);
    }

    public String getPhrase()
    {
        return this.phrase;
    }

    /**
     * @deprecated use {@link #getStatusCode()} instead
     * @return HTTP status code
     */
    public int getStatuscode()
    {
        return this.getStatusCode();
    }

    public int getStatusCode()
    {
        return this.statusCode;
    }

    public HttpVersion getHttpVersion()
    {
        return this.ver;
    }

    public String getStatusLine()
    {
        StringBuffer buffer = new StringBuffer(64);
        buffer.append(this.ver);
        buffer.append(' ');
        buffer.append(this.statusCode);
        if (this.phrase != null)
        {
            buffer.append(' ');
            buffer.append(this.phrase);
        }
        return buffer.toString();
    }

    public boolean containsHeader(final String name)
    {
        return this.headers.containsHeader(name);
    }

    public Header[] getHeaders()
    {
        return this.headers.getAllHeaders();
    }

    public Header getFirstHeader(final String name)
    {
        return this.headers.getFirstHeader(name);
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

    public void setHeaders(final Header[] headers)
    {
        if (headers == null)
        {
            return;
        }
        this.headers.setHeaders(headers);
    }

    public Iterator getHeaderIterator()
    {
        return this.headers.getIterator();
    }

    public String getCharset()
    {
        String charset = getFallbackCharset();
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
        return charset;
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

    public boolean hasBody()
    {
        return outputHandler != null;
    }

    public OutputHandler getBody() throws TransformerException 
    {
        return outputHandler; 
    }
    
    public void setBody(MuleMessage msg) throws TransformerException 
    {
        if (msg == null) return;
        
        Object payload = msg.getPayload();
        if (payload instanceof String)
        {
            setBody(payload.toString());
        }
        else if (payload instanceof NullPayload) 
        {
            return;
        }
        else if (payload instanceof byte[]) 
        {
            setBody((byte[]) payload);
        }
        else 
        {
            setBody((OutputHandler) msg.getPayload(OutputHandler.class));
        }
    }
    
    public void setBody(OutputHandler outputHandler) 
    {
        this.outputHandler = outputHandler;
    }
    
    public void setBody(final String string)
    {
        byte[] raw;
        try
        {
            raw = string.getBytes(getCharset());
        }
        catch (UnsupportedEncodingException e)
        {
            raw = string.getBytes();
        }
        setBody(raw);
    }

    private void setBody(final byte[] raw)
    {
        if (!containsHeader(HttpConstants.HEADER_CONTENT_TYPE))
        {
            setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE));
        }
        setHeader(new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(raw.length)));
        
        this.outputHandler = new OutputHandler() {

            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                out.write(raw);
            }
            
        };
    }
    
    public String getBodyAsString() throws IOException 
    {
        if (!hasBody()) return "";
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        outputHandler.write(RequestContext.getEvent(), out);
        
        try
        {
            return new String(out.toByteArray(), getCharset());
        }
        catch (UnsupportedEncodingException e)
        {
            return new String(out.toByteArray());
        }
    }
    
    public boolean isKeepAlive()
    {
        return !disableKeepAlive && keepAlive;
    }

    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }
    
    /**
     * The HTTTP spec suggests that for HTTP 1.1 persistent connections should be used, 
     * for HTTP 1.0 the connection should not be kept alive. This method sets up the keepAlive flag
     * according to the <code>version</code> that was passed in.
     */
    protected void setupKeepAliveFromRequestVersion(HttpVersion version)
    {
        setKeepAlive(version.equals(HttpVersion.HTTP_1_1));
    }

    public void disableKeepAlive(boolean keepalive)
    {
        disableKeepAlive = keepalive;
    }

    public String getFallbackCharset()
    {
        return fallbackCharset;
    }

    public void setFallbackCharset(String overrideCharset)
    {
        this.fallbackCharset = overrideCharset;
    }

}
