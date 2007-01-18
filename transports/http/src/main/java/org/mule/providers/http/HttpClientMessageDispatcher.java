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

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.impl.message.ExceptionPayload;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.http.transformers.HttpClientMethodResponseToObject;
import org.mule.providers.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOStreamMessageAdapter;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.StringUtils;

/**
 * <code>HttpClientMessageDispatcher</code> dispatches Mule events over HTTP.
 */
public class HttpClientMessageDispatcher extends AbstractMessageDispatcher
{
    private final HttpConnector connector;
    private volatile HttpClient client = null;
    private final UMOTransformer receiveTransformer;

    public HttpClientMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (HttpConnector)endpoint.getConnector();
        receiveTransformer = new HttpClientMethodResponseToObject();
    }

    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = new HttpClient();

            HttpState state = new HttpState();
            if (connector.getProxyUsername() != null)
            {
                state.setProxyCredentials(new AuthScope(null, -1, null, null),
                    new UsernamePasswordCredentials(connector.getProxyUsername(),
                        connector.getProxyPassword()));
            }
            client.setState(state);
            client.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());

            // test the connection
            // HeadMethod method = new
            // HeadMethod(endpoint.getEndpointURI().getAddress());
            // client.executeMethod(getHostConfig(endpoint.getEndpointURI().getUri()),
            // method);
        }

    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnectorSession#doDispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);
        execute(event, httpMethod, true);
        if (httpMethod.getStatusCode() >= 400)
        {
            logger.error(httpMethod.getResponseBodyAsString());
            throw new DispatchException(event.getMessage(), event.getEndpoint(), new Exception(
                "Http call returned a status of: " + httpMethod.getStatusCode() + " "
                                + httpMethod.getStatusText()));
        }
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
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
        try
        {
            HttpClient client = new HttpClient();
            client.executeMethod(httpMethod);

            if (httpMethod.getStatusCode() == HttpStatus.SC_OK)
            {
                return (UMOMessage)receiveTransformer.transform(httpMethod);
            }
            else
            {
                throw new ReceiveException(new Message("http", 3, httpMethod.getStatusLine().toString()),
                    endpoint, timeout);
            }
        }
        catch (ReceiveException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
        finally
        {
            httpMethod.releaseConnection();
        }
    }

    protected HttpMethod execute(UMOEvent event, HttpMethod httpMethod, boolean closeConnection)
        throws Exception
    {
        // TODO set connection timeout buffer etc
        try
        {
            URI uri = event.getEndpoint().getEndpointURI().getUri();

            processCookies(event);
            // TODO can we use this code for better reporting?
            int code = client.executeMethod(getHostConfig(uri), httpMethod);

            return httpMethod;
        }
        catch (ConnectException cex)
        {
            // TODO employ dispatcher reconnection strategy at this point
            throw new DispatchException(event.getMessage(), event.getEndpoint(), cex);
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
        finally
        {
            if (httpMethod != null && closeConnection)
            {
                httpMethod.releaseConnection();
            }
        }
    }

    protected void processCookies(UMOEvent event)
    {
        UMOMessage msg = event.getMessage();
        Cookie[] cookies = (Cookie[])msg.removeProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (cookies != null && cookies.length > 0)
        {
            String policy = (String)msg.removeProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
            client.getParams().setCookiePolicy(CookieHelper.getCookiePolicy(policy));
            client.getState().addCookies(cookies);
        }
    }

    protected HttpMethod getMethod(UMOEvent event) throws TransformerException
    {
        UMOMessage msg = event.getMessage();
        String method = msg.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
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
        else
        {
            PostMethod postMethod = new PostMethod(uri.toString());

            if (body instanceof String)
            {
                ObjectToHttpClientMethodRequest trans = new ObjectToHttpClientMethodRequest();
                httpMethod = (HttpMethod)trans.transform(body.toString());
            }
            else if (body instanceof HttpMethod)
            {
                httpMethod = (HttpMethod)body;
            }
            else if (body instanceof UMOStreamMessageAdapter)
            {
                UMOStreamMessageAdapter sma = (UMOStreamMessageAdapter)body;
                Map headers = sma.getOutputHandler().getHeaders(event);
                for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry entry = (Map.Entry)iterator.next();
                    postMethod.addRequestHeader((String)entry.getKey(), (String)entry.getValue());
                }
                postMethod.setRequestEntity(new StreamPayloadRequestEntity((StreamMessageAdapter)body, event));
                postMethod.setContentChunked(true);
                httpMethod = postMethod;
            }
            else
            {
                byte[] buffer = event.getTransformedMessageAsBytes();
                postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, event.getEncoding()));
                httpMethod = postMethod;
            }

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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);

        httpMethod = execute(event, httpMethod, false);

        try
        {
            Properties h = new Properties();
            Header[] headers = httpMethod.getResponseHeaders();
            for (int i = 0; i < headers.length; i++)
            {
                h.setProperty(headers[i].getName(), headers[i].getValue());
            }

            String status = String.valueOf(httpMethod.getStatusCode());

            h.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, status);
            if (logger.isDebugEnabled())
            {
                logger.debug("Http response is: " + status);
            }
            ExceptionPayload ep = null;
            if (httpMethod.getStatusCode() >= 400)
            {
                ep = new ExceptionPayload(new DispatchException(event.getMessage(), event.getEndpoint(),
                    new Exception("Http call returned a status of: " + httpMethod.getStatusCode() + " "
                                  + httpMethod.getStatusText())));
            }
            UMOMessage m;
            // text or binary content?
            Header header = httpMethod.getResponseHeader(HttpConstants.HEADER_CONTENT_TYPE);
            if ((header != null) && event.isStreaming())
            {
                HttpStreamMessageAdapter sp = (HttpStreamMessageAdapter)connector.getStreamMessageAdapter(
                    httpMethod.getResponseBodyAsStream(), null);
                sp.setHttpMethod(httpMethod);
                m = new MuleMessage(sp, h);
            }
            else
            {
                Object body = IOUtils.toByteArray(httpMethod.getResponseBodyAsStream());
                if (body == null)
                {
                    body = StringUtils.EMPTY;
                }
                UMOMessageAdapter adapter = connector.getMessageAdapter(new Object[]{body, h});
                m = new MuleMessage(adapter);
            }
            m.setExceptionPayload(ep);
            return m;
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
        finally
        {
            if (httpMethod != null && !event.isStreaming())
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

    private class StreamPayloadRequestEntity implements RequestEntity
    {
        private UMOStreamMessageAdapter messageAdapter;
        private UMOEvent event;

        public StreamPayloadRequestEntity(UMOStreamMessageAdapter messageAdapter, UMOEvent event)
        {
            this.messageAdapter = messageAdapter;
            this.event = event;
        }

        public boolean isRepeatable()
        {
            return true;
        }

        public void writeRequest(OutputStream outputStream) throws IOException
        {
            messageAdapter.getOutputHandler().write(event, outputStream);
        }

        public long getContentLength()
        {
            return -1L;
        }

        public String getContentType()
        {
            return event.getMessage().getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, null);
        }
    }

}
