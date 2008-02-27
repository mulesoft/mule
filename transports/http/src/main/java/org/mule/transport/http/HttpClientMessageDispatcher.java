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
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.OutputHandler;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * <code>HttpClientMessageDispatcher</code> dispatches Mule events over HTTP.
 */
public class HttpClientMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * Range start for http error status codes.
     */
    public static final int ERROR_STATUS_CODE_RANGE_START = 400;
    private final HttpConnector connector;
    private volatile HttpClient client = null;
    private final Transformer sendTransformer;

    public HttpClientMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (HttpConnector) endpoint.getConnector();
        this.sendTransformer = new ObjectToHttpClientMethodRequest();
    }
    
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = connector.doClientConnect();
        }
    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);
        try
        {
            execute(event, httpMethod);
            
            if (httpMethod.getStatusCode() >= ERROR_STATUS_CODE_RANGE_START)
            {
                logger.error(httpMethod.getResponseBodyAsString());
                throw new DispatchException(event.getMessage(), event.getEndpoint(), new Exception(
                    "Http call returned a status of: " + httpMethod.getStatusCode() + " "
                                    + httpMethod.getStatusText()));
            }
        }
        finally
        {
            httpMethod.releaseConnection();
        }
    }

    protected HttpMethod execute(MuleEvent event, HttpMethod httpMethod)
        throws Exception
    {
        // TODO set connection timeout buffer etc
        try
        {
            URI uri = event.getEndpoint().getEndpointURI().getUri();

            this.processCookies(event);

            // TODO can we use the return code for better reporting?
            client.executeMethod(getHostConfig(uri), httpMethod);

            return httpMethod;
        }
        catch (IOException e)
        {
            // TODO employ dispatcher reconnection strategy at this point
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
        
    }

    protected void processCookies(MuleEvent event)
    {
        MuleMessage msg = event.getMessage();
        Object cookieObject = msg.removeProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (cookieObject instanceof Cookie[])
        {
            // cookies came in via a regular HTTP request
            Cookie[] cookies = (Cookie[]) cookieObject;
            if (cookies != null && cookies.length > 0)
            {
                String policy = (String) msg.removeProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
                client.getParams().setCookiePolicy(CookieHelper.getCookiePolicy(policy));
                client.getState().addCookies(cookies);
            }
        }
        else if (cookieObject instanceof Map)
        {
            // cookies were configured on the endpoint
            client.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

            String host = this.getEndpoint().getEndpointURI().getHost();
            String path = this.getEndpoint().getEndpointURI().getPath();
            Map cookieMap = (Map) cookieObject;
            Iterator keyIter = cookieMap.keySet().iterator();
            while (keyIter.hasNext())
            {
                String key = (String) keyIter.next();
                String value = (String) cookieMap.get(key);
                Cookie cookie = new Cookie(host, key, value, path, null, false);
                client.getState().addCookie(cookie);
            }
        }
        else if (cookieObject != null)
        {
            throw new IllegalArgumentException("Invalid cookies " + cookieObject);
        }
    }

    protected HttpMethod getMethod(MuleEvent event) throws TransformerException
    {
        MuleMessage msg = event.getMessage();
        setPropertyFromEndpoint(event, msg, HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);
        
        HttpMethod httpMethod;
        Object body = event.transformMessage();

        if (body instanceof HttpMethod)
        {
            httpMethod = (HttpMethod)body;
        }
        else 
        {
            httpMethod = (HttpMethod) sendTransformer.transform(msg);
        }
        
        
        return httpMethod;
    }

    protected void setPropertyFromEndpoint(MuleEvent event, MuleMessage msg, String prop)
    {
        Object o = msg.getProperty(prop, null);
        if (o == null) {
            
            o = event.getEndpoint().getProperty(prop);
            if (o != null) {
                msg.setProperty(prop, o);
            }
        }
    }

    protected HttpMethod createEntityMethod(MuleEvent event, Object body, EntityEnclosingMethod postMethod)
        throws TransformerException
    {
        HttpMethod httpMethod;
        if (body instanceof String)
        {
            ObjectToHttpClientMethodRequest trans = new ObjectToHttpClientMethodRequest();
            httpMethod = (HttpMethod)trans.transform(body.toString());
        }
        else if (body instanceof byte[])
        {
            byte[] buffer = event.transformMessageToBytes();
            postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, event.getEncoding()));
            httpMethod = postMethod;
        }
        else 
        {
            if (!(body instanceof OutputHandler)) 
            {
                body = event.transformMessage(OutputHandler.class);
            }
            
            OutputHandler outputHandler = (OutputHandler)body;
            postMethod.setRequestEntity(new StreamPayloadRequestEntity(outputHandler, event));
            postMethod.setContentChunked(true);
            httpMethod = postMethod;
        }
        
        return httpMethod;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.transport.Connector#send(org.mule.api.MuleEvent)
     */
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {        
        HttpMethod httpMethod = getMethod(event);
        connector.setupClientAuthorization(event, httpMethod, client, endpoint);
        
        httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new MuleHttpMethodRetryHandler());

        Object body = null;
        boolean releaseConn = false;
        try
        {
            httpMethod = execute(event, httpMethod);

            DefaultExceptionPayload ep = null;
            if (httpMethod.getStatusCode() >= ERROR_STATUS_CODE_RANGE_START)
            {
                ep = new DefaultExceptionPayload(new DispatchException(event.getMessage(), event.getEndpoint(),
                    new Exception("Http call returned a status of: " + httpMethod.getStatusCode() + " "
                                  + httpMethod.getStatusText())));
            }
            
            
            InputStream is = httpMethod.getResponseBodyAsStream();
            if (is == null)
            {
                body = StringUtils.EMPTY;
                releaseConn = true;
            }            
            else
            {
                is = new ReleasingInputStream(is, httpMethod);
                body = is;
            }
            
            Header[] headers = httpMethod.getResponseHeaders();
            HttpMessageAdapter adapter = new HttpMessageAdapter(new Object[]{body, headers});

            String status = String.valueOf(httpMethod.getStatusCode());

            adapter.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, status);
            if (logger.isDebugEnabled())
            {
                logger.debug("Http response is: " + status);
            }
            
            MuleMessage m = new DefaultMuleMessage(adapter);
          
            m.setExceptionPayload(ep);
            return m;
        }
        catch (Exception e)
        {
            releaseConn = true;
            if (e instanceof DispatchException)
            {
                throw (DispatchException) e;
            }
            
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
        finally
        {
            if (releaseConn) 
            {
                httpMethod.releaseConnection();
            }
        }
    }

    protected HostConfiguration getHostConfig(URI uri) throws URISyntaxException
    {
        Protocol protocol = Protocol.getProtocol(uri.getScheme().toLowerCase());

        String host = uri.getHost();
        int port = uri.getPort();
        HostConfiguration config = new HostConfiguration();
        config.setHost(host, port, protocol);
        if (StringUtils.isNotBlank(connector.getProxyHostname()))
        {
            // add proxy support
            config.setProxy(connector.getProxyHostname(), connector.getProxyPort());
        }
        return config;
    }

    protected void doDispose()
    {
        // template method
    }
}
