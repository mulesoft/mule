/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.IOUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpMuleMessageFactory extends AbstractMuleMessageFactory
{
    private static Log log = LogFactory.getLog(HttpMuleMessageFactory.class);
    private static final String DEFAULT_ENCODING = "UTF-8";

    private boolean enableCookies = false;
    private String cookieSpec;
    private MessageExchangePattern exchangePattern = MessageExchangePattern.REQUEST_RESPONSE;

    public HttpMuleMessageFactory()
    {
    }

    /**
     * @deprecated use {@link #HttpMuleMessageFactory()}  instead
     */
    @Deprecated
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
        Object body = httpRequest.getBody();

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

        return body;
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
        Map<String, Object> httpHeaders = new HashMap<String, Object>();
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        if (transportMessage instanceof HttpRequest)
        {
            HttpRequest httpRequest = (HttpRequest) transportMessage;
            method = httpRequest.getRequestLine().getMethod();
            httpVersion = httpRequest.getRequestLine().getHttpVersion();
            uri = httpRequest.getRequestLine().getUri();
            headers = convertHeadersToMap(httpRequest.getHeaders(), uri);
            convertMultiPartHeaders(headers);
        }
        else if (transportMessage instanceof HttpMethod)
        {
            HttpMethod httpMethod = (HttpMethod) transportMessage;
            method = httpMethod.getName();
            httpVersion = HttpVersion.parse(httpMethod.getStatusLine().getHttpVersion());
            uri = httpMethod.getURI().toString();
            statusCode = String.valueOf(httpMethod.getStatusCode());
            headers = convertHeadersToMap(httpMethod.getResponseHeaders(), uri);
        }
        else
        {
            // This should never happen because of the supported type checking in our superclass
            throw new MessageTypeNotSupportedException(transportMessage, getClass());
        }

        rewriteConnectionAndKeepAliveHeaders(headers);

        headers = processIncomingHeaders(headers);

        httpHeaders.put(HttpConnector.HTTP_HEADERS, new HashMap<String, Object>(headers));

        String encoding = getEncoding(headers);
        
        queryParameters.put(HttpConnector.HTTP_QUERY_PARAMS, processQueryParams(uri, encoding));

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
        message.addInboundProperties(httpHeaders);
        message.addInboundProperties(queryParameters);

        // The encoding is stored as message property. To avoid overriding it from the message
        // properties, it must be initialized last
        initEncoding(message, encoding);
    }

    protected Map<String, Object> processIncomingHeaders(Map<String, Object> headers) throws Exception
    {
        Map<String, Object> outHeaders = new HashMap<String, Object>();

        for (Map.Entry<String, Object> header : headers.entrySet())
        {
            String headerName = header.getKey();

            // fix Mule headers?
            if (headerName.startsWith("X-MULE"))
            {
                headerName = headerName.substring(2);
            }

            // accept header & value
            outHeaders.put(headerName, header.getValue());
        }

        return outHeaders;
    }

    Map<String, Object> convertHeadersToMap(Header[] headersArray, String uri)
        throws URISyntaxException
    {
        Map<String, Object> headersMap = new CaseInsensitiveHashMap();
        for (int i = 0; i < headersArray.length; i++)
        {
            final Header header = headersArray[i];
            // Cookies are a special case because there may be more than one
            // cookie.
            if (HttpConnector.HTTP_COOKIES_PROPERTY.equals(header.getName())
                || HttpConstants.HEADER_COOKIE.equals(header.getName()))
            {
                putCookieHeaderInMapAsAServer(headersMap, header, uri);
            }
            else if (HttpConstants.HEADER_COOKIE_SET.equals(header.getName()))
            {
                putCookieHeaderInMapAsAClient(headersMap, header, uri);
            }
            else
            {
                if (headersMap.containsKey(header.getName()))
                {
                    if (headersMap.get(header.getName()) instanceof String)
                    {
                        // concat
                        headersMap.put(header.getName(),
                            headersMap.get(header.getName()) + "," + header.getValue());
                    }
                    else
                    {
                        // override
                        headersMap.put(header.getName(), header.getValue());
                    }
                }
                else
                {
                    headersMap.put(header.getName(), header.getValue());
                }
            }
        }
        return headersMap;
    }

    private void putCookieHeaderInMapAsAClient(Map<String, Object> headersMap, final Header header, String uri)
        throws URISyntaxException
    {
        try
        {
            final Cookie[] newCookies = CookieHelper.parseCookiesAsAClient(header.getValue(), cookieSpec,
                new URI(uri));
            final Object preExistentCookies = headersMap.get(HttpConstants.HEADER_COOKIE_SET);
            final Object mergedCookie = CookieHelper.putAndMergeCookie(preExistentCookies, newCookies);
            headersMap.put(HttpConstants.HEADER_COOKIE_SET, mergedCookie);
        }
        catch (MalformedCookieException e)
        {
            log.warn("Received an invalid cookie: " + header, e);
        }
    }

    private void putCookieHeaderInMapAsAServer(Map<String, Object> headersMap, final Header header, String uri)
        throws URISyntaxException
    {
        if (enableCookies)
        {
            Cookie[] newCookies = CookieHelper.parseCookiesAsAServer(header.getValue(), new URI(uri));
            if (newCookies.length > 0)
            {
                Object oldCookies = headersMap.get(HttpConnector.HTTP_COOKIES_PROPERTY);
                Object mergedCookies = CookieHelper.putAndMergeCookie(oldCookies, newCookies);
                headersMap.put(HttpConnector.HTTP_COOKIES_PROPERTY, mergedCookies);
            }
        }
    }

    private String getContentType(Map<String, Object> headers)
    {
        return (String) headers.get(HttpConstants.HEADER_CONTENT_TYPE);
    }

    private String getEncoding(Map<String, Object> headers)
    {
        String encoding = DEFAULT_ENCODING;
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
                    encoding = param.getValue();
                }
            }
        }
        return encoding;
    }
    
    private void initEncoding(MuleMessage message, String encoding)
    {
        message.setEncoding(encoding);
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
        String queryString = "";
        if(i > -1)
        {
            queryString = uri.substring(i + 1);
            headers.putAll(PropertiesUtils.getPropertiesFromQueryString(queryString));
        }
        headers.put(HttpConnector.HTTP_QUERY_STRING, queryString);
    }
    
    protected Map<String, Object> processQueryParams(String uri, String encoding) throws UnsupportedEncodingException
    {
        Map<String, Object> httpParams = new HashMap<String, Object>();
        
        int i = uri.indexOf("?");
        if(i > -1)
        {
            String queryString = uri.substring(i + 1);
            for (StringTokenizer st = new StringTokenizer(queryString, "&"); st.hasMoreTokens();)
            {
                String token = st.nextToken();
                int idx = token.indexOf('=');
                if (idx < 0)
                {
                    addQueryParamToMap(httpParams, unescape(token, encoding), null);
                }
                else if (idx > 0)
                {
                    addQueryParamToMap(httpParams, unescape(token.substring(0, idx), encoding),
                        unescape(token.substring(idx + 1), encoding));
                }
            }
        }
            
        return httpParams;
    }

    private void addQueryParamToMap(Map<String, Object> httpParams, String key, String value)
    {
        Object existingValue = httpParams.get(key);
        if (existingValue == null)
        {
            httpParams.put(key, value);
        }
        else if (existingValue instanceof List)
        {
            List<String> list = (List<String>) existingValue;
            list.add(value);
        }
        else if (existingValue instanceof String)
        {
            List<String> list = new ArrayList<String>();
            list.add((String) existingValue);
            list.add(value);
            httpParams.put(key, list);
        }
    }
    
    private String unescape(String escapedValue, String encoding) throws UnsupportedEncodingException
    {
        if(escapedValue != null)
        {
            return URLDecoder.decode(escapedValue, encoding);
        }
        return escapedValue;
    }
    

    protected void convertMultiPartHeaders(Map<String, Object> headers)
    {
        // template method
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
