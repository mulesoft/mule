/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers.http;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.NullPayload;
import org.mule.providers.http.transformers.HttpClientMethodResponseToObject;
import org.mule.providers.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * <p><code>HttpClientMessageDispatcher</code> dispatches Mule events over http.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpClientMessageDispatcher extends AbstractMessageDispatcher
{
    private HttpConnector connector;
    private HttpState state;
    private UMOTransformer receiveTransformer;

    public HttpClientMessageDispatcher(HttpConnector connector)
    {
        super(connector);
        this.connector = connector;
        receiveTransformer = new HttpClientMethodResponseToObject();


        state = new HttpState();
        if (connector.getProxyUsername() != null)
        {
            state.setProxyCredentials(null, null, new UsernamePasswordCredentials(connector.getProxyUsername(), connector.getProxyPassword()));
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnectorSession#doDispatch(org.mule.umo.UMOEvent)
     */
    public void doDispatch(UMOEvent event) throws Exception
    {
        send(event);
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnectorSession#getConnector()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnectorSession#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnectorSession#receive(java.lang.String, org.mule.umo.UMOEvent)
     */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        if (endpointUri == null) return null;

        HttpMethod  httpMethod = new GetMethod(endpointUri.getAddress());

        HttpConnection connection = null;
        try
        {
            connection = getConnection(endpointUri.getUri());
            if (connection.isProxied() && connection.isSecure())
            {
                httpMethod = new ConnectMethod(httpMethod);
            }
            httpMethod.execute(state, connection);

            if (httpMethod.getStatusCode() == HttpStatus.SC_OK)
            {
                return (UMOMessage)receiveTransformer.transform(httpMethod);
            } else
            {
                throw new ReceiveException(new Message("http", 3, httpMethod.getStatusLine().toString()), endpointUri, timeout);
            }
        } catch (ReceiveException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new ReceiveException(endpointUri, timeout, e);
        } finally
        {
            if (httpMethod != null) httpMethod.releaseConnection();
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        String method = (String) event.getProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
        URI uri = event.getEndpoint().getEndpointURI().getUri();
        HttpMethod httpMethod = null;
        Object body = event.getTransformedMessage();
        if(body instanceof HttpMethod) {
            httpMethod = (HttpMethod)body;
        } else if ("GET".equals(method) || body instanceof NullPayload)
        {
            httpMethod = new GetMethod(uri.toString());
        } else
        {
            PostMethod postMethod = new PostMethod(uri.toString());

            if(body instanceof String) {
                ObjectToHttpClientMethodRequest trans = new ObjectToHttpClientMethodRequest();
                httpMethod = (HttpMethod)trans.transform(body.toString());
//                postMethod.setRequestBody(body.toString());
//                postMethod.setRequestContentLength(body.toString().length());
//                httpMethod = postMethod;
            } else if (body instanceof HttpMethod) {
                httpMethod= (HttpMethod)body;
            } else {
                byte[] buffer = event.getTransformedMessageAsBytes();
                postMethod.setRequestBody(new ByteArrayInputStream(buffer));
                postMethod.setRequestContentLength(buffer.length);
                httpMethod = postMethod;
            }

        }
        HttpConnection connection = null;
        try
        {
            connection = getConnection(uri);

            if (connection.isProxied() && connection.isSecure())
            {
                httpMethod = new ConnectMethod(httpMethod);
            }
            httpMethod.setDoAuthentication(true);
            httpMethod.execute(state, connection);

            Properties h = new Properties();
            Header[] headers = httpMethod.getRequestHeaders();
            for (int i = 0; i < headers.length; i++)
            {
                h.setProperty(headers[i].getName(), headers[i].getValue());
            }
            String status = String.valueOf(httpMethod.getStatusCode());
            h.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, status);
            logger.debug("Http response is: " + status);
            return new MuleMessage(httpMethod.getResponseBodyAsString(), h);
        } catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        } finally
        {
            if (httpMethod != null) httpMethod.releaseConnection();
        }
    }


    protected HttpConnection getConnection(URI uri) throws URISyntaxException
    {
        HttpConnection connection = null;

        Protocol protocol = Protocol.getProtocol(connector.getProtocol().toLowerCase());

        String host = uri.getHost();
        int port = uri.getPort();
        
        connection = new HttpConnection(host, port, protocol);
        connection.setProxyHost(connector.getProxyHostname());
        connection.setProxyPort(connector.getProxyPort());
        return connection;
    }

    public void doDispose()
    {
        state = null;
    }

}
