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
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transformer.TransformerChain;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
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
import org.apache.commons.lang.BooleanUtils;

/**
 * <code>HttpClientMessageDispatcher</code> dispatches Mule events over HTTP.
 */
public class HttpClientMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * Range start for http error status codes.
     */
    public static final int ERROR_STATUS_CODE_RANGE_START = 400;
    public static final int REDIRECT_STATUS_CODE_RANGE_START = 300;
    protected final HttpConnector connector;
    private volatile HttpClient client = null;
    private final Transformer sendTransformer;

    public HttpClientMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (HttpConnector) endpoint.getConnector();
        List<Transformer> ts = connector.getDefaultOutboundTransformers();
        if (ts.size() == 1)
        {
            this.sendTransformer = ts.get(0);
        }
        else if (ts.size() == 0)
        {
            this.sendTransformer = new ObjectToHttpClientMethodRequest();
        }
        else
        {
            this.sendTransformer = new TransformerChain(ts);
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        //This seems wrong
        sendTransformer.setMuleContext(connector.getMuleContext());
        sendTransformer.initialise();
    }

    @Override
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = connector.doClientConnect();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(endpoint.getResponseTimeout());
            client.getHttpConnectionManager().getParams().setSoTimeout(endpoint.getResponseTimeout());
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);
        connector.setupClientAuthorization(event, httpMethod, client, endpoint);

        try
        {
            execute(event, httpMethod);

            if (returnException(event, httpMethod))
            {
                logger.error(httpMethod.getResponseBodyAsString());
                throw new DispatchException(event.getMessage(), event.getEndpoint(), new Exception(
                        "Http call returned a status of: " + httpMethod.getStatusCode() + " "
                                + httpMethod.getStatusText()));
            }
            else if (httpMethod.getStatusCode() >= REDIRECT_STATUS_CODE_RANGE_START)
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("Received a redirect response code: " + httpMethod.getStatusCode() + " " + httpMethod.getStatusText());
                }
            }
        }
        finally
        {
            httpMethod.releaseConnection();
        }
    }

    protected HttpMethod execute(MuleEvent event, HttpMethod httpMethod) throws Exception
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

        Object cookiesProperty = msg.removeProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        String cookieSpecProperty = (String) msg.removeProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
        processCookies(cookiesProperty, cookieSpecProperty, event);

        cookiesProperty = endpoint.getProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        cookieSpecProperty = (String) endpoint.getProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
        processCookies(cookiesProperty, cookieSpecProperty, event);
    }

    private void processCookies(Object cookieObject, String policy, MuleEvent event)
    {
        if (cookieObject instanceof Cookie[])
        {
            // cookies came in via a regular HTTP request
            Cookie[] cookies = (Cookie[]) cookieObject;
            if (cookies != null && cookies.length > 0)
            {
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
                String cookieValue = (String) cookieMap.get(key);

                String value = event.getMuleContext().getExpressionManager().parse(cookieValue,
                        event.getMessage());

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
            httpMethod = (HttpMethod) body;
        }
        else
        {
            httpMethod = (HttpMethod) sendTransformer.transform(msg);
        }

        httpMethod.setFollowRedirects("true".equalsIgnoreCase((String)endpoint.getProperty("followRedirects")));
        return httpMethod;
    }

    protected void setPropertyFromEndpoint(MuleEvent event, MuleMessage msg, String prop)
    {
        Object o = msg.getProperty(prop, PropertyScope.OUTBOUND);
        if (o == null)
        {
            o = event.getEndpoint().getProperty(prop);
            if (o != null)
            {
                msg.setProperty(prop, o);
            }
        }
    }

    protected HttpMethod createEntityMethod(MuleEvent event, Object body, EntityEnclosingMethod postMethod) throws TransformerException
    {
        HttpMethod httpMethod;
        if (body instanceof String)
        {
            httpMethod = (HttpMethod) sendTransformer.transform(body.toString());
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

            OutputHandler outputHandler = (OutputHandler) body;
            postMethod.setRequestEntity(new StreamPayloadRequestEntity(outputHandler, event));
            postMethod.setContentChunked(true);
            httpMethod = postMethod;
        }

        return httpMethod;
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);
        connector.setupClientAuthorization(event, httpMethod, client, endpoint);

        httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new MuleHttpMethodRetryHandler());
        boolean releaseConn = false;
        try
        {
            httpMethod = execute(event, httpMethod);

            DefaultExceptionPayload ep = null;

            if (returnException(event, httpMethod))
            {
                ep = new DefaultExceptionPayload(new DispatchException(event.getMessage(), event.getEndpoint(),
                        new HttpResponseException(httpMethod.getStatusText(), httpMethod.getStatusCode())));
            }
            else if (httpMethod.getStatusCode() >= REDIRECT_STATUS_CODE_RANGE_START)
            {
                try
                {
                    return handleRedirect(httpMethod, event);
                }
                catch (Exception e)
                {
                    ep = new DefaultExceptionPayload(new DispatchException(event.getMessage(), event.getEndpoint(), e));
                    return getResponseFromMethod(httpMethod, ep);
                }
            }
            releaseConn = httpMethod.getResponseBodyAsStream() == null;
            return getResponseFromMethod(httpMethod, ep);
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

    protected MuleMessage handleRedirect(HttpMethod method, MuleEvent event) throws HttpResponseException, MuleException, IOException
    {
        String followRedirects = (String)endpoint.getProperty("followRedirects");
        if (followRedirects==null || "false".equalsIgnoreCase(followRedirects))
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Received a redirect, but followRedirects=false. Response code: " + method.getStatusCode() + " " + method.getStatusText());
            }
            return getResponseFromMethod(method, null);
        }
        Header locationHeader = method.getResponseHeader(HttpConstants.HEADER_LOCATION);
        if (locationHeader == null)
        {
            throw new HttpResponseException(method.getStatusText(), method.getStatusCode());
        }
        OutboundEndpoint out = new EndpointURIEndpointBuilder(locationHeader.getValue(), connector.getMuleContext()).buildOutboundEndpoint();
        if (event.isSynchronous())
        {
            return connector.send(out, event);
        }
        else
        {
            connector.dispatch(out, event);
            return null;
        }
    }

    protected MuleMessage getResponseFromMethod(HttpMethod httpMethod, ExceptionPayload ep) throws IOException
    {
        Object body = null;

        InputStream is = httpMethod.getResponseBodyAsStream();
        if (is == null)
        {
            body = StringUtils.EMPTY;
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

        MuleMessage m = new DefaultMuleMessage(adapter, connector.getMuleContext());

        m.setExceptionPayload(ep);
        return m;
    }

    protected boolean returnException(MuleEvent event, HttpMethod httpMethod)
    {
        return httpMethod.getStatusCode() >= ERROR_STATUS_CODE_RANGE_START
                && !BooleanUtils.toBoolean((String) event.getMessage().getProperty(HttpConnector.HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK));
    }

    protected HostConfiguration getHostConfig(URI uri) throws Exception
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

    @Override
    protected void doDispose()
    {
        // template method
    }

}
