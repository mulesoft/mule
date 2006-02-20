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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.impl.message.ExceptionPayload;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.http.transformers.HttpClientMethodResponseToObject;
import org.mule.providers.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.Utility;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * <p>
 * <code>HttpClientMessageDispatcher</code> dispatches Mule events over http.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpClientMessageDispatcher extends AbstractMessageDispatcher
{
    private HttpConnector connector;
    private HttpClient client = null;
    private UMOTransformer receiveTransformer;

    public HttpClientMessageDispatcher(HttpConnector connector)
    {
        super(connector);
        this.connector = connector;
        receiveTransformer = new HttpClientMethodResponseToObject();
        client = new HttpClient();

        HttpState state = new HttpState();
        if (connector.getProxyUsername() != null) {
            state.setProxyCredentials(new AuthScope( null, -1, null, null), new UsernamePasswordCredentials(connector.getProxyUsername(),
                                                                                  connector.getProxyPassword()));
        }
        client.setState(state);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.providers.AbstractConnectorSession#doDispatch(org.mule.umo.UMOEvent)
     */
    public void doDispatch(UMOEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);
        execute(event, httpMethod, false);
        //if(httpMethod!=null) {
            httpMethod.releaseConnection();
            if(httpMethod.getStatusCode() >= 400 ) {
                logger.error(httpMethod.getResponseBodyAsString());
                throw new DispatchException(event.getMessage(), event.getEndpoint(),
                        new Exception("Http call returned a status of: " + httpMethod.getStatusCode() + " " + httpMethod.getStatusText()));
            }
       // }
    }

    public void doStreaming(UMOEvent event, StreamMessageAdapter messageAdapter) throws Exception
    {
        doDispatch(event);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnectorSession#getConnector()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnectorSession#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnectorSession#receive(java.lang.String,
     *      org.mule.umo.UMOEvent)
     */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {

            if (endpointUri == null)
                return null;

            HttpMethod httpMethod = new GetMethod(endpointUri.getAddress());
            httpMethod.setDoAuthentication(true);
            if (endpointUri.getUserInfo() != null) {
                // Add User Creds
                StringBuffer header = new StringBuffer();
                header.append("Basic ");
                header.append(new BASE64Encoder().encode(endpointUri.getUserInfo().getBytes()));
                httpMethod.addRequestHeader(HttpConstants.HEADER_AUTHORIZATION, header.toString());
            }
         try {
            HttpClient client = new HttpClient();

            client.executeMethod(httpMethod);
            //httpMethod.execute(state, connection);

            if (httpMethod.getStatusCode() == HttpStatus.SC_OK) {
                return (UMOMessage) receiveTransformer.transform(httpMethod);
            } else {
                throw new ReceiveException(new Message("http", 3, httpMethod.getStatusLine().toString()),
                                           endpointUri,
                                           timeout);
            }
        } catch (ReceiveException e) {
            throw e;
        } catch (Exception e) {
            throw new ReceiveException(endpointUri, timeout, e);
        } finally {
            if (httpMethod != null)
                httpMethod.releaseConnection();
        }
    }

    protected HttpMethod execute(UMOEvent event, HttpMethod httpMethod, boolean closeConnection) throws Exception
    {

        try {
            URI uri = event.getEndpoint().getEndpointURI().getUri();

            try {
                client.executeMethod(getHostConfig(uri), httpMethod);
            } catch(Exception e) {
                logger.error(e, e);
            }
            return httpMethod;
        } catch (Exception e) {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        } finally {
            if (httpMethod != null && closeConnection)
                httpMethod.releaseConnection();
        }
    }

    protected HttpMethod getMethod(UMOEvent event) throws TransformerException {
        String method = (String) event.getProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
        URI uri = event.getEndpoint().getEndpointURI().getUri();
        HttpMethod httpMethod = null;
        Object body = event.getTransformedMessage();

         if (body instanceof HttpMethod) {
            httpMethod = (HttpMethod) body;
        //todo } else if ("GET".equalsIgnoreCase(method) || body instanceof NullPayload) {
        } else if ("GET".equalsIgnoreCase(method)) {
            httpMethod = new GetMethod(uri.toString());
        } else {
            PostMethod postMethod = new PostMethod(uri.toString());

            if (body instanceof String) {
                ObjectToHttpClientMethodRequest trans = new ObjectToHttpClientMethodRequest();
                httpMethod = (HttpMethod) trans.transform(body.toString());
            } else if (body instanceof HttpMethod) {
                httpMethod = (HttpMethod) body;
            } else if(body instanceof StreamMessageAdapter) {
                postMethod.setRequestEntity(new StreamPayloadRequestEntity((StreamMessageAdapter)body, event));
                postMethod.setContentChunked(true);
                httpMethod = postMethod;
            } else{
                byte[] buffer = event.getTransformedMessageAsBytes();
                //todo MULE20 Encoding
                postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer /*, event.getEncoding()*/));
                httpMethod = postMethod;
            }

        }
        httpMethod.setDoAuthentication(true);
        if (event.getCredentials() != null) {
            String authScopeHost = event.getStringProperty("http.auth.scope.host",null);
            int authScopePort = event.getIntProperty("http.auth.scope.port", -1);
            String authScopeRealm = event.getStringProperty("http.auth.scope.realm",null);
            String authScopeScheme = event.getStringProperty("http.auth.scope.scheme",null);
            client.getState().setCredentials(new AuthScope(authScopeHost, authScopePort, authScopeRealm, authScopeScheme),
                    new UsernamePasswordCredentials(event.getCredentials().getUsername(), new String(event.getCredentials().getPassword())));
        }
        return httpMethod;
    }
    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        HttpMethod httpMethod = getMethod(event);

        httpMethod = execute(event, httpMethod, false);

        try {
            Properties h = new Properties();
            Header[] headers = httpMethod.getResponseHeaders();
            for (int i = 0; i < headers.length; i++) {
                h.setProperty(headers[i].getName(), headers[i].getValue());
            }
            String status = String.valueOf(httpMethod.getStatusCode());

            h.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, status);
            logger.debug("Http response is: " + status);
            ExceptionPayload ep = null;
            if(httpMethod.getStatusCode() >= 400 ) {
                logger.error(Utility.getInputStreamAsString(httpMethod.getResponseBodyAsStream()));
                ep = new ExceptionPayload(new DispatchException(event.getMessage(), event.getEndpoint(),
                        new Exception("Http call returned a status of: " + httpMethod.getStatusCode() + " " + httpMethod.getStatusText())));
            }
            UMOMessage m = null;
            // text or binary content?
            Header header = httpMethod.getResponseHeader(HttpConstants.HEADER_CONTENT_TYPE);
            if((header != null) && event.isStreaming()) {
                StreamMessageAdapter sp = (StreamMessageAdapter)event.getMessage().getAdapter();
                sp.setResponse(httpMethod.getResponseBodyAsStream());
                m = new MuleMessage(sp, h);
            } else if ((header != null) && header.getValue().startsWith("text/")) {
                m = new MuleMessage(httpMethod.getResponseBodyAsString(), h);
            } else {
                m = new MuleMessage(httpMethod.getResponseBody(), h);
            }
            m.setExceptionPayload(ep);
            return m;
        } catch (Exception e) {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        } finally {
            if (httpMethod != null && !event.isStreaming())
                httpMethod.releaseConnection();
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
    protected HostConfiguration getHostConfig(URI uri) throws URISyntaxException
    {
        Protocol protocol = Protocol.getProtocol(uri.getScheme().toLowerCase());

        String host = uri.getHost();
        int port = uri.getPort();
        HostConfiguration config = new HostConfiguration();
        config.setHost(host, port, protocol);

        return config;
    }

    public void doDispose()
    {
        client = null;
    }

    private class StreamPayloadRequestEntity implements RequestEntity {

        private StreamMessageAdapter messageAdapter;
        private UMOEvent event;

        public StreamPayloadRequestEntity(StreamMessageAdapter messageAdapter, UMOEvent event) {
            this.messageAdapter = messageAdapter;
            this.event = event;
        }

        public boolean isRepeatable() {
            return true;
        }

        public void writeRequest(OutputStream outputStream) throws IOException {
           messageAdapter.getOutputHandler().write(event, outputStream);
        }

        public long getContentLength() {
            return -1L;
        }

        public String getContentType() {
            return event.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, null);
        }
    }
}
