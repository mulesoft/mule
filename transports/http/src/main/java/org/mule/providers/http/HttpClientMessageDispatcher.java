/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.impl.MuleMessage;
import org.mule.impl.message.ExceptionPayload;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.http.i18n.HttpMessages;
import org.mule.providers.http.transformers.HttpClientMethodResponseToObject;
import org.mule.providers.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.OutputHandler;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
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
    private final UMOTransformer receiveTransformer;

    public HttpClientMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (HttpConnector) endpoint.getConnector();
        this.receiveTransformer = new HttpClientMethodResponseToObject();
    }
    
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            HttpState state = new HttpState();

            if (connector.getProxyUsername() != null)
            {
                state.setProxyCredentials(new AuthScope(null, -1, null, null),
                    new UsernamePasswordCredentials(connector.getProxyUsername(),
                        connector.getProxyPassword()));
            }

            client = new HttpClient();
            client.setState(state);
            client.setHttpConnectionManager(connector.getClientConnectionManager());
        }

    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    protected void doDispatch(UMOEvent event) throws Exception
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

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        HttpMethod httpMethod = new GetMethod(endpoint.getEndpointURI().getAddress());
        httpMethod.setDoAuthentication(true);
        if (endpoint.getEndpointURI().getUserInfo() != null
            && endpoint.getProperty(HttpConstants.HEADER_AUTHORIZATION) == null)
        {
            // Add User Creds
            StringBuffer header = new StringBuffer(128);
            header.append("Basic ");
            header.append(new String(Base64.encodeBase64(endpoint.getEndpointURI().getUserInfo().getBytes(
                endpoint.getEncoding()))));
            httpMethod.addRequestHeader(HttpConstants.HEADER_AUTHORIZATION, header.toString());
        }
        
        boolean releaseConn = false;
        try
        {
            HttpClient client = new HttpClient();
            client.executeMethod(httpMethod);

            if (httpMethod.getStatusCode() == HttpStatus.SC_OK)
            {
                UMOMessage res = (UMOMessage) receiveTransformer.transform(httpMethod);
                if (StringUtils.EMPTY.equals(res.getPayload()))
                {
                    releaseConn = true;
                }
                return res;
            }
            else
            {
                releaseConn = true;
                throw new ReceiveException(
                    HttpMessages.requestFailedWithStatus(httpMethod.getStatusLine().toString()),
                    endpoint, timeout);
            }
        }
        catch (ReceiveException e)
        {
            releaseConn = true;
            throw e;
        }
        catch (Exception e)
        {   
            releaseConn = true;
            throw new ReceiveException(endpoint, timeout, e);
        }
        finally
        {
            if (releaseConn)
            {
                httpMethod.releaseConnection();
            }
        }
    }

    protected HttpMethod execute(UMOEvent event, HttpMethod httpMethod)
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

    protected void processCookies(UMOEvent event)
    {
        UMOMessage msg = event.getMessage();
        Cookie[] cookies = (Cookie[]) msg.removeProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (cookies != null && cookies.length > 0)
        {
            String policy = (String) msg.removeProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
            client.getParams().setCookiePolicy(CookieHelper.getCookiePolicy(policy));
            client.getState().addCookies(cookies);
        }
    }

    protected HttpMethod getMethod(UMOEvent event) throws TransformerException
    {
        UMOMessage msg = event.getMessage();
        String method = msg.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
        
        
        setPropertyFromEndpoint(event, msg, HttpConstants.HEADER_CONTENT_TYPE);
        setPropertyFromEndpoint(event, msg, HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);
        
        URI uri = event.getEndpoint().getEndpointURI().getUri();
        HttpMethod httpMethod;
        Object body = event.getTransformedMessage();

        if (body instanceof HttpMethod)
        {
            httpMethod = (HttpMethod)body;
        }
        else if (HttpConstants.METHOD_GET.equalsIgnoreCase(method))
        {
            httpMethod = new GetMethod(uri.toString());
        }
        else if (HttpConstants.METHOD_PUT.equalsIgnoreCase(method))
        {
            PutMethod postMethod = new PutMethod(uri.toString());

            httpMethod = createEntityMethod(event, body, postMethod);
        }
        else if (HttpConstants.METHOD_POST.equalsIgnoreCase(method))
        {
            PostMethod postMethod = new PostMethod(uri.toString());

            httpMethod = createEntityMethod(event, body, postMethod);
        }
        else if (HttpConstants.METHOD_DELETE.equalsIgnoreCase(method))
        {
            httpMethod = new DeleteMethod(uri.toString());
        }
        else if (HttpConstants.METHOD_HEAD.equalsIgnoreCase(method))
        {
            httpMethod = new HeadMethod(uri.toString());
        }
        else if (HttpConstants.METHOD_OPTIONS.equalsIgnoreCase(method))
        {
            httpMethod = new OptionsMethod(uri.toString());
        }
        else if (HttpConstants.METHOD_TRACE.equalsIgnoreCase(method))
        {
            httpMethod = new TraceMethod(uri.toString());
        }
        else
        {
            throw new TransformerException(HttpMessages.unsupportedMethod(method));
        }
        
        httpMethod.setDoAuthentication(true);
        if (event.getCredentials() != null)
        {
            String authScopeHost = msg.getStringProperty("http.auth.scope.host", null);
            int authScopePort = msg.getIntProperty("http.auth.scope.port", -1);
            String authScopeRealm = msg.getStringProperty("http.auth.scope.realm", null);
            String authScopeScheme = msg.getStringProperty("http.auth.scope.scheme", null);
            client.getState().setCredentials(
                new AuthScope(authScopeHost, authScopePort, authScopeRealm, authScopeScheme),
                new UsernamePasswordCredentials(event.getCredentials().getUsername(), new String(
                    event.getCredentials().getPassword())));
            client.getParams().setAuthenticationPreemptive(true);
        }
        else
        {
            // don't use preemptive if there are no credentials to send
            client.getParams().setAuthenticationPreemptive(false);
        }
        return httpMethod;
    }

    protected void setPropertyFromEndpoint(UMOEvent event, UMOMessage msg, String prop)
    {
        Object o = msg.getProperty(prop, null);
        if (o == null) {
            
            o = event.getEndpoint().getProperty(prop);
            if (o != null) {
                msg.setProperty(prop, o);
            }
        }
    }

    protected HttpMethod createEntityMethod(UMOEvent event, Object body, EntityEnclosingMethod postMethod)
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
            byte[] buffer = event.getTransformedMessageAsBytes();
            postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, event.getEncoding()));
            httpMethod = postMethod;
        }
        else 
        {
            if (!(body instanceof OutputHandler)) 
            {
                body = event.getTransformedMessage(OutputHandler.class);
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
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {        
        HttpMethod httpMethod = getMethod(event);
        httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new MuleHttpMethodRetryHandler());

        Object body = null;
        boolean releaseConn = false;
        try
        {
            httpMethod = execute(event, httpMethod);

            ExceptionPayload ep = null;
            if (httpMethod.getStatusCode() >= ERROR_STATUS_CODE_RANGE_START)
            {
                ep = new ExceptionPayload(new DispatchException(event.getMessage(), event.getEndpoint(),
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
            
            UMOMessage m = new MuleMessage(adapter);
          
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
