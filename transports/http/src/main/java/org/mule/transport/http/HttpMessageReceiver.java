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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.MuleServer;
import org.mule.NullSessionHandler;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.ConnectException;
import org.mule.transport.NullPayload;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.tcp.TcpMessageReceiver;
import org.mule.util.MapUtils;
import org.mule.util.ObjectUtils;
import org.mule.util.monitor.Expirable;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.resource.spi.work.Work;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used to
 * listen for HTTP requests on a particular port.
 */
public class HttpMessageReceiver extends TcpMessageReceiver
{
    protected final Log logger = LogFactory.getLog(getClass());

    public HttpMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);
    }

    @Override
    protected Work createWork(Socket socket) throws IOException
    {
        return new HttpWorker(socket);
    }

    @Override
    protected void doConnect() throws ConnectException
    {
        // If we already have an endpoint listening on this socket don't try and
        // start another serversocket
        if (this.shouldConnect())
        {
            super.doConnect();
        }
    }

    protected boolean shouldConnect()
    {
        StringBuffer requestUri = new StringBuffer(80);
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(':').append(endpoint.getEndpointURI().getPort());
        requestUri.append('*');

        MessageReceiver[] receivers = connector.getReceivers(requestUri.toString());
        for (int i = 0; i < receivers.length; i++)
        {
            if (receivers[i].isConnected())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    protected MuleMessage handleUnacceptedFilter(MuleMessage message)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Message request '" + message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY)
                    + "' is being rejected since it does not match the filter on this endpoint: " + endpoint);
        }
        message.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.SC_NOT_ACCEPTABLE));
        return message;
    }

    protected class HttpWorker implements Work, Expirable
    {
        private HttpServerConnection conn;
        private String cookieSpec;
        private boolean enableCookies;
        private String remoteClientAddress;

        public HttpWorker(Socket socket) throws IOException
        {
            String encoding = endpoint.getEncoding();
            if (encoding == null)
            {
                encoding = MuleServer.getMuleContext().getConfiguration().getDefaultEncoding();
            }

            conn = new HttpServerConnection(socket, encoding, (HttpConnector) connector);

            cookieSpec =
                    MapUtils.getString(endpoint.getProperties(), HttpConnector.HTTP_COOKIE_SPEC_PROPERTY,
                            ((HttpConnector) connector).getCookieSpec());
            enableCookies =
                    MapUtils.getBooleanValue(endpoint.getProperties(), HttpConnector.HTTP_ENABLE_COOKIES_PROPERTY,
                            ((HttpConnector) connector).isEnableCookies());

            final SocketAddress clientAddress = socket.getRemoteSocketAddress();
            if (clientAddress != null)
            {
                remoteClientAddress = clientAddress.toString();
            }
        }
        
        public void expired()
        {
            if (conn.isOpen())
            {
                conn.close();
            }
        }

        public void run()
        {
            long keepAliveTimeout = ((TcpConnector) connector).getKeepAliveTimeout();
            
            try
            {
                do
                {
                    conn.setKeepAlive(false);
                    
                    // Only add a monitor if the timeout has been set
                    if (keepAliveTimeout > 0)
                    {
                        ((HttpConnector) connector).getKeepAliveMonitor().addExpirable(
                            keepAliveTimeout, TimeUnit.MILLISECONDS, this);
                    }
                    
                    HttpRequest request = conn.readRequest();
                    if (request == null)
                    {
                        break;
                    }
                    
                    // Ensure that we drop any monitors, we'll add again for the next request
                    ((HttpConnector) connector).getKeepAliveMonitor().removeExpirable(this);
                    
                    conn.writeResponse(processRequest(request));
                    if (request.getBody() != null) 
                    {
                        request.getBody().close();
                    }
                }
                while (conn.isKeepAlive());
            }
            catch (Exception e)
            {
                handleException(e);
            }
            finally
            {
                logger.debug("Closing HTTP connection.");

                if (conn.isOpen())
                {
                    conn.close();
                    conn = null;
                    
                    // Ensure that we drop any monitors
                    ((HttpConnector) connector).getKeepAliveMonitor().removeExpirable(this);
                }
            }
        }

        protected HttpResponse processRequest(HttpRequest request) throws MuleException, IOException
        {
            RequestLine requestLine = request.getRequestLine();
            String method = requestLine.getMethod();

            if (method.equals(HttpConstants.METHOD_GET)
                    || method.equals(HttpConstants.METHOD_HEAD)
                    || method.equals(HttpConstants.METHOD_POST)
                    || method.equals(HttpConstants.METHOD_OPTIONS)
                    || method.equals(HttpConstants.METHOD_PUT)
                    || method.equals(HttpConstants.METHOD_DELETE)
                    || method.equals(HttpConstants.METHOD_TRACE)
                    || method.equals(HttpConstants.METHOD_CONNECT))
            {
                return doRequest(request, requestLine);
            }
            else
            {
                return doBad(requestLine);
            }
        }


        protected HttpResponse doRequest(HttpRequest request,
                                         RequestLine requestLine) throws IOException, MuleException
        {
            Map headers = parseHeaders(request);

            // TODO Mule 2.0 generic way to set stream message adapter
            MessageAdapter adapter = buildStandardAdapter(request, headers);

            MuleMessage message = new DefaultMuleMessage(adapter);

            String path = (String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
            int i = path.indexOf('?');
            if (i > -1)
            {
                path = path.substring(0, i);
            }

            message.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, path);
            
            if (logger.isDebugEnabled())
            {
                logger.debug(message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
            }

            message.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, 
                HttpConnector.normalizeUrl(endpoint.getEndpointURI().getPath()));
            
            // determine if the request path on this request denotes a different receiver
            MessageReceiver receiver = getTargetReceiver(message, endpoint);

            HttpResponse response;
            // the response only needs to be transformed explicitly if
            // A) the request was not served or B) a null result was returned
            if (receiver != null)
            {
                preRouteMessage(message);
                MuleMessage returnMessage = receiver.routeMessage(message, endpoint.isSynchronous(), null);

                Object tempResponse;
                if (returnMessage != null)
                {
                    tempResponse = returnMessage.getPayload();
                }
                else
                {
                    tempResponse = NullPayload.getInstance();
                }
                // This removes the need for users to explicitly adding the response transformer
                // ObjectToHttpResponse in their config
                if (tempResponse instanceof HttpResponse)
                {
                    response = (HttpResponse) tempResponse;
                }
                else
                {
                    response = transformResponse(returnMessage);
                }
                
                HttpConnector httpConnector = (HttpConnector) connector;
                response.disableKeepAlive(!httpConnector.isKeepAlive());
                
                // Check if endpoint has a keep-alive property configured. Note the translation from
                // keep-alive in the schema to keepAlive here.
                boolean endpointOverride = true;
                String keepAliveEndpointValue = (String) endpoint.getProperty("keepAlive");
                if (keepAliveEndpointValue != null)
                {
                    endpointOverride = Boolean.parseBoolean(keepAliveEndpointValue);
                }
                
                Header connectionHeader = request.getFirstHeader("Connection");
                if (connectionHeader != null)
                {
                    String value = connectionHeader.getValue();
					if ("keep-alive".equalsIgnoreCase(value) && endpointOverride) 
                    {
                        response.setKeepAlive(true);
                    	
                        Header header = new Header(HttpConstants.HEADER_KEEP_ALIVE, "timeout=" 
                            + httpConnector.getKeepAliveTimeout());
                        response.addHeader(header); 
                        
                        if (response.getHttpVersion().equals(HttpVersion.HTTP_1_0))
                        {
                            connectionHeader = new Header(HttpConstants.HEADER_CONNECTION, "Keep-Alive");
                            response.setHeader(connectionHeader);
                        }
                    }
                    else if ("close".equalsIgnoreCase(value))
                    {
                        response.setKeepAlive(false);
                    } 
                    else if (response.getHttpVersion().greaterEquals(HttpVersion.HTTP_1_1))
                    {
                        response.setKeepAlive(true);
                    }
                    else
                    {
                        response.setKeepAlive(false);
                    }
                }
            }
            else
            {
                response = buildFailureResponse(requestLine, message);
            }
            return response;
        }

        protected HttpResponse doOtherValid(RequestLine requestLine, String method) throws MuleException
        {
            MuleMessage message = new DefaultMuleMessage(NullPayload.getInstance());
            MuleEvent event = new DefaultMuleEvent(message, endpoint, new DefaultMuleSession(message, new NullSessionHandler(), connector.getMuleContext()), true);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_METHOD_NOT_ALLOWED);
            response.setBody(HttpMessages.methodNotAllowed(method).toString() + HttpConstants.CRLF);
            return transformResponse(response);
        }

        protected HttpResponse doBad(RequestLine requestLine) throws MuleException
        {
            MuleMessage message = new DefaultMuleMessage(NullPayload.getInstance());
            MuleEvent event = new DefaultMuleEvent(message, endpoint, new DefaultMuleSession(message, new NullSessionHandler(), connector.getMuleContext()), true);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_BAD_REQUEST);
            response.setBody(HttpMessages.malformedSyntax().toString() + HttpConstants.CRLF);
            return transformResponse(response);
        }

        protected MessageAdapter buildStandardAdapter(final HttpRequest request,
                                                         final Map headers) throws MessagingException, TransformerException, IOException
        {
            final RequestLine requestLine = request.getRequestLine();

            sendExpect100(headers, requestLine);

            Object body = request.getBody();
            if (body == null)
            {
                body = requestLine.getUri();
            }

            return connector.getMessageAdapter(new Object[]{body, headers});
        }

        private void sendExpect100(Map headers, RequestLine requestLine)
            throws TransformerException, IOException
        {
            // respond with status code 100, for Expect handshake
            // according to rfc 2616 and http 1.1
            // the processing will continue and the request will be fully
            // read immediately after
            if (HttpConstants.HTTP11.equals(headers.get(HttpConnector.HTTP_VERSION_PROPERTY)))
            {
                // just in case we have something other than String in
                // the headers map
                String expectHeaderValue = ObjectUtils.toString(
                        headers.get(HttpConstants.HEADER_EXPECT)).toLowerCase();
                if (HttpConstants.HEADER_EXPECT_CONTINUE_REQUEST_VALUE.equals(expectHeaderValue))
                {
                    HttpResponse expected = new HttpResponse();
                    expected.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_CONTINUE);
                    final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(expected), endpoint,
                            new DefaultMuleSession(service, connector.getMuleContext()), true);
                    RequestContext.setEvent(event);
                    conn.writeResponse(transformResponse(expected));
                }
            }
        }

        protected HttpResponse buildFailureResponse(RequestLine requestLine, MuleMessage message) throws TransformerException
        {
            EndpointURI uri = endpoint.getEndpointURI();
            String failedPath = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort()
                    + message.getProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);

            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to bind to " + failedPath);
            }

            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_NOT_FOUND);
            response.setBody(HttpMessages.cannotBindToAddress(failedPath).toString());
            RequestContext.setEvent(new DefaultMuleEvent(new DefaultMuleMessage(response), endpoint,
                    new DefaultMuleSession(service, connector.getMuleContext()), true));
            // The DefaultResponseTransformer will set the necessary headers
            return transformResponse(response);
        }

        protected Map parseHeaders(HttpRequest request) throws MalformedCookieException
        {
            RequestLine requestLine = request.getRequestLine();
            Map<String, Object> headers = new HashMap<String, Object>();

            for (Iterator rhi = request.getHeaderIterator(); rhi.hasNext();)
            {
                Header header = (Header) rhi.next();
                String headerName = header.getName();
                Object headerValue = header.getValue();

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
                        Cookie[] cookies = CookieHelper.parseCookies(header, cookieSpec);
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
                headers.put(headerName, headerValue);
            }

            headers.put(HttpConnector.HTTP_METHOD_PROPERTY, requestLine.getMethod());
            headers.put(HttpConnector.HTTP_REQUEST_PROPERTY, requestLine.getUri());
            headers.put(HttpConnector.HTTP_VERSION_PROPERTY, requestLine.getHttpVersion().toString());
            headers.put(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, cookieSpec);
            return headers;
        }

        protected void preRouteMessage(MuleMessage message)
        {
            message.setProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS, remoteClientAddress);
        }

        public void release()
        {
            conn.close();
            conn = null;
        }
    }

    protected MessageReceiver getTargetReceiver(MuleMessage message, ImmutableEndpoint endpoint)
            throws ConnectException
    {
        String path = (String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf('?');
        if (i > -1)
        {
            path = path.substring(0, i);
        }

        StringBuffer requestUri = new StringBuffer(80);
        if (path.indexOf("://") == -1)
        {
            requestUri.append(endpoint.getProtocol()).append("://");
            requestUri.append(endpoint.getEndpointURI().getHost());
            requestUri.append(':').append(endpoint.getEndpointURI().getPort());
            
            if (!"/".equals(path)) {
                requestUri.append(path);
            }
        }
        
        String uriStr = requestUri.toString();
        // first check that there is a receiver on the root address
        if (logger.isTraceEnabled())
        {
            logger.trace("Looking up receiver on connector: " + connector.getName() + " with URI key: "
                    + requestUri.toString());
        }

        MessageReceiver receiver = connector.lookupReceiver(uriStr);

        // If no receiver on the root and there is a request path, look up the
        // received based on the root plus request path
        if (receiver == null && !"/".equals(path))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Secondary lookup of receiver on connector: " + connector.getName()
                        + " with URI key: " + requestUri.toString());
            }

            if (receiver == null)
            {
                receiver = findReceiverByStem(connector.getReceivers(), uriStr);
            }

            if (receiver == null && logger.isWarnEnabled())
            {
                logger.warn("No receiver found with secondary lookup on connector: " + connector.getName()
                        + " with URI key: " + requestUri.toString());
                logger.warn("Receivers on connector are: "
                        + MapUtils.toString(connector.getReceivers(), true));
            }
        }

        return receiver;
    }

    protected HttpResponse transformResponse(Object response) throws TransformerException
    {
        MuleMessage message;
        if (response instanceof MuleMessage)
        {
            message = (MuleMessage) response;
        }
        else
        {
            message = new DefaultMuleMessage(response);
        }
        //TODO RM*: Maybe we can have a generic Transformer wrapper rather that using DefaultMuleMessage (or another static utility
        //class
        message.applyTransformers(connector.getDefaultResponseTransformers(), HttpResponse.class);
        return (HttpResponse) message.getPayload();
    }

    public static MessageReceiver findReceiverByStem(Map receivers, String uriStr)
    {
        int match = 0;
        MessageReceiver receiver = null;
        for (Iterator itr = receivers.entrySet().iterator(); itr.hasNext();)
        {
            Map.Entry e = (Map.Entry) itr.next();
            String key = (String) e.getKey();
            MessageReceiver candidate = (MessageReceiver) e.getValue();
            if (uriStr.startsWith(key) && match < key.length())
            {
                match = key.length();
                receiver = candidate;
            }
        }
        return receiver;
    }

}
