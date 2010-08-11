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

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.http.multipart.MultiPartInputStream;
import org.mule.transport.http.multipart.Part;
import org.mule.transport.http.multipart.PartDataSource;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.IOUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpMuleMessageFactory extends AbstractMuleMessageFactory
{
    private static Log log = LogFactory.getLog(HttpMuleMessageFactory.class);

    private boolean enableCookies = false;
    private String cookieSpec;
    private MessageExchangePattern exchangePattern;
    private Collection<Part> parts;

    public HttpMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{HttpRequest.class, HttpMethod.class};
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        if (transportMessage instanceof HttpRequest)
        {
            return extractPayloadFromHttpRequest((HttpRequest) transportMessage);
        }
        else if (transportMessage instanceof HttpMethod)
        {
            return extractPayloadFromHttpMethod((HttpMethod) transportMessage);
        }
        else
        {
            // This should never happen because of the supported type checking
            throw new MessageTypeNotSupportedException(transportMessage, getClass());
        }
    }

    protected Object extractPayloadFromHttpRequest(HttpRequest httpRequest) throws IOException
    {
        Object body = null;

        if (httpRequest.getContentType().contains("multipart/form-data"))
        {
            MultiPartInputStream in = new MultiPartInputStream(httpRequest.getBody(), httpRequest.getContentType(), null);

            //We need to store this so that the headers for the part can be read
            parts = in.getParts();
            for (Part part : parts)
            {
                if (part.getName().equals("payload"))
                {
                    body = part.getInputStream();
                    break;
                }
            }
            if (body == null)
            {
                throw new IllegalArgumentException("todo");
            }
        }
        else
        {

            body = httpRequest.getBody();

            // If http method is GET we use the request uri as the payload.
            if (body == null)
            {
                body = httpRequest.getRequestLine().getUri();
            }
            else
            {
                // If we are running async we need to read stream into a byte[].
                // Passing along the InputStream doesn't work because the
                // HttpConnection gets closed and closes the InputStream, often
                // before it can be read.
                if (!exchangePattern.hasResponse())
                {
                    log.debug("Reading HTTP POST InputStream into byte[] for asynchronous messaging.");
                    body = IOUtils.toByteArray((InputStream) body);
                }
            }
        }

        return body;
    }

    @Override
    protected void addAttachments(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        if (parts != null)
        {
            try
            {
                for (Part part : parts)
                {
                    if (!part.getName().equals("payload"))
                    {
                        message.addInboundAttachment(part.getName(), new DataHandler(new PartDataSource(part)));
                    }
                }
            }
            finally
            {
                //Attachments are the last thing to get processed
                parts.clear();
                parts = null;
            }
        }
    }

    protected Object extractPayloadFromHttpMethod(HttpMethod httpMethod) throws IOException
    {
        InputStream body = httpMethod.getResponseBodyAsStream();
        if (body != null)
        {
            return new ReleasingInputStream(body, httpMethod);
        }
        else
        {
            return StringUtils.EMPTY;
        }
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        String method;
        HttpVersion httpVersion;
        String uri;
        String statusCode = null;
        Map<String, Object> headers;
        // This is a shortcut for now
        Header cookieHeader;

        if (transportMessage instanceof HttpRequest)
        {
            HttpRequest httpRequest = (HttpRequest) transportMessage;
            method = httpRequest.getRequestLine().getMethod();
            httpVersion = httpRequest.getRequestLine().getHttpVersion();
            uri = httpRequest.getRequestLine().getUri();
            headers = convertHeadersToMap(httpRequest.getHeaders());
            convertMultiPartHeaders(headers);

            cookieHeader = httpRequest.getFirstHeader(HttpConnector.HTTP_COOKIES_PROPERTY);
        }
        else if (transportMessage instanceof HttpMethod)
        {
            HttpMethod httpMethod = (HttpMethod) transportMessage;
            method = httpMethod.getName();
            httpVersion = HttpVersion.parse(httpMethod.getStatusLine().getHttpVersion());
            uri = httpMethod.getURI().toString();
            statusCode = String.valueOf(httpMethod.getStatusCode());
            headers = convertHeadersToMap(httpMethod.getResponseHeaders());
            cookieHeader = httpMethod.getResponseHeader(HttpConnector.HTTP_COOKIES_PROPERTY);
        }
        else
        {
            // This should never happen because of the supported type checking in our superclass
            throw new MessageTypeNotSupportedException(transportMessage, getClass());
        }

        rewriteConnectionAndKeepAliveHeaders(headers);

        headers = processIncomingHeaders(headers, uri, cookieHeader);

        //Make any URI params available ans inbound message headers
        addUriParamsAsHeaders(headers, uri);

        headers.put(HttpConnector.HTTP_METHOD_PROPERTY, method);
        headers.put(HttpConnector.HTTP_REQUEST_PROPERTY, uri);
        headers.put(HttpConnector.HTTP_VERSION_PROPERTY, httpVersion.toString());
        if (enableCookies)
        {
            headers.put(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, cookieSpec);
        }

        if (statusCode != null)
        {
            headers.put(HttpConnector.HTTP_STATUS_PROPERTY, statusCode);
        }

        message.addInboundProperties(headers);

        // The encoding is stored as message property. To avoid overriding it from the message
        // properties, it must be initialized last
        initEncoding(message, headers);
    }

    protected Map<String, Object> processIncomingHeaders(Map<String, Object> headers, String uri,
                                                         Header cookieHeader) throws Exception
    {
        Map<String, Object> outHeaders = new HashMap<String, Object>();

        for (String headerName : headers.keySet())
        {
            Object headerValue = headers.get(headerName);

            // fix Mule headers?
            if (headerName.startsWith("X-MULE"))
            {
                headerName = headerName.substring(2);
            }
            // Parse cookies?
            else if (headerName.equals(HttpConnector.HTTP_COOKIES_PROPERTY))
            {
                if (enableCookies)
                {
                    CookieSpec cs = CookieHelper.getCookieSpec(cookieSpec);
                    URI u = new URI(uri);

                    Cookie[] cookies = cs.parse(u.getHost(), u.getPort(), u.getPath(),
                            u.getScheme().equalsIgnoreCase("https"), cookieHeader);
                    if (cookies.length > 0)
                    {
                        // yum!
                        headerValue = cookies;
                    }
                    else
                    {
                        // bad cookies?!
                        continue;
                    }
                }
                else
                {
                    // no cookies for you!
                    continue;
                }
            }

            // accept header & value
            outHeaders.put(headerName, headerValue);
        }

        return outHeaders;
    }

    private Map<String, Object> convertHeadersToMap(Header[] headers)
    {
        Map<String, Object> headersMap = new CaseInsensitiveHashMap();
        for (int i = 0; i < headers.length; i++)
        {
            headersMap.put(headers[i].getName(), headers[i].getValue());
        }
        return headersMap;
    }

    private void initEncoding(MuleMessage message, Map<String, Object> headers)
    {
        Object contentType = headers.get(HttpConstants.HEADER_CONTENT_TYPE);
        if (contentType != null)
        {
            // use HttpClient classes to parse the charset part from the Content-Type
            // header (e.g. "text/html; charset=UTF-16BE")
            Header contentTypeHeader = new Header(HttpConstants.HEADER_CONTENT_TYPE,
                    contentType.toString());
            HeaderElement values[] = contentTypeHeader.getElements();
            if (values.length == 1)
            {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null)
                {
                    message.setEncoding(param.getValue());
                }
            }
        }
    }

    private void rewriteConnectionAndKeepAliveHeaders(Map<String, Object> headers)
    {
        // rewrite Connection and Keep-Alive headers based on HTTP version
        String headerValue;
        if (!isHttp11(headers))
        {
            String connection = (String) headers.get(HttpConstants.HEADER_CONNECTION);
            if ((connection != null) && connection.equalsIgnoreCase("close"))
            {
                headerValue = Boolean.FALSE.toString();
            }
            else
            {
                headerValue = Boolean.TRUE.toString();
            }
        }
        else
        {
            headerValue = (headers.get(HttpConstants.HEADER_CONNECTION) != null
                    ? Boolean.TRUE.toString()
                    : Boolean.FALSE.toString());
        }

        headers.put(HttpConstants.HEADER_CONNECTION, headerValue);
        headers.put(HttpConstants.HEADER_KEEP_ALIVE, headerValue);
    }

    private boolean isHttp11(Map<String, Object> headers)
    {
        String httpVersion = (String) headers.get(HttpConnector.HTTP_VERSION_PROPERTY);
        return !HttpConstants.HTTP10.equalsIgnoreCase(httpVersion);
    }

    protected void addUriParamsAsHeaders(Map headers, String uri)
    {
        int i = uri.indexOf("?");
        if (i > -1)
        {
            headers.putAll(PropertiesUtils.getPropertiesFromQueryString(uri.substring(i + 1)));
        }
    }

    protected void convertMultiPartHeaders(Map headers)
    {
        if (parts != null)
        {
            for (Part part : parts)
            {
                if (part.getName().equals("payload"))
                {
                    for (String name : part.getHeaderNames())
                    {
                        headers.put(name, part.getHeader(name));
                    }
                    break;
                }
            }

        }

    }

    public void setEnableCookies(boolean enableCookies)
    {
        this.enableCookies = enableCookies;
    }

    public void setCookieSpec(String cookieSpec)
    {
        this.cookieSpec = cookieSpec;
    }

    public void setExchangePattern(MessageExchangePattern mep)
    {
        exchangePattern = mep;
    }
}
