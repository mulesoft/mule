/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.transport.ConnectException;
import org.mule.transport.NullPayload;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.tcp.TcpMessageReceiver;
import org.mule.util.MapUtils;
import org.mule.util.monitor.Expirable;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used to
 * listen for HTTP requests on a particular port.
 */
public class OldHttpMessageReceiver extends TcpMessageReceiver
{
    public OldHttpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
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
        StringBuilder requestUri = new StringBuilder(80);
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(':').append(endpoint.getEndpointURI().getPort());
        requestUri.append('*');

        MessageReceiver[] receivers = connector.getReceivers(requestUri.toString());
        for (MessageReceiver receiver : receivers)
        {
            if (receiver.isConnected())
            {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("synthetic-access")
    protected class HttpWorker implements Work, Expirable
    {
        private HttpServerConnection conn;
        private String remoteClientAddress;

        public HttpWorker(Socket socket) throws IOException
        {
            String encoding = endpoint.getEncoding();
            if (encoding == null)
            {
                encoding = getEndpoint().getMuleContext().getConfiguration().getDefaultEncoding();
            }

            conn = new HttpServerConnection(socket, encoding, (HttpConnector) connector);

            final SocketAddress clientAddress = socket.getRemoteSocketAddress();
            if (clientAddress != null)
            {
                remoteClientAddress = clientAddress.toString();
            }
        }

        @Override
        public void expired()
        {
            if (conn.isOpen())
            {
                conn.close();
            }
        }

        @Override
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

                    final HttpRequest request = conn.readRequest();
                    if (request == null)
                    {
                        break;
                    }

                    try
                    {
                        HttpResponse httpResponse = processRequest(request);
                        conn.writeResponse(httpResponse);
                    }
                    catch (Exception e)
                    {
                        MuleEvent response = null;
                        if (e instanceof MessagingException)
                        {
                            response = ((MessagingException) e).getEvent();
                        }
                        else
                        {
                            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
                        }

                        if (response != null &&
                            response.getMessage().getExceptionPayload() != null &&
                            response.getMessage().getExceptionPayload().getException() instanceof MessagingException)
                        {
                            e = (Exception) response.getMessage().getExceptionPayload().getException();
                        }
                        //MULE-5656 There was custom code here for mapping status codes to exceptions
                        //I have removed this code and now make an explicit call to the Exception helper,
                        //but the real fix is to make sure Mule handles this automatically through the
                        //InboundExceptionDetailsMessageProcessor

                        //Response code mappings are loaded from META-INF/services/org/mule/config/http-exception-mappings.properties
                        String temp = ExceptionHelper.getErrorMapping(connector.getProtocol(), e.getClass(),flowConstruct.getMuleContext());
                        int httpStatus = Integer.valueOf(temp);

                        if (e instanceof MessagingException)
                        {
                            MuleEvent event = ((MessagingException) e).getEvent();
                            httpStatus = event.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY) != null ? Integer.valueOf(event.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString()) : httpStatus;
                            conn.writeResponse(buildFailureResponse(event, e.getMessage(),httpStatus));
                        }
                        else
                        {
                            conn.writeResponse(buildFailureResponse(request.getRequestLine().getHttpVersion(), httpStatus, e.getMessage()));
                        }
                        break;
                    }
                    finally
                    {
                        // Ensure that we drop any monitors
                        ((HttpConnector) connector).getKeepAliveMonitor().removeExpirable(this);
                        conn.reset();
                        if (request.getBody() != null)
                        {
                            request.getBody().close();
                        }
                    }
                }
                while (conn.isKeepAlive());
            }
            catch (Exception e)
            {
                getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            }
            finally
            {
                logger.debug("Closing HTTP connection.");

                if (conn.isOpen())
                {
                    conn.close();
                    conn = null;
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
                || method.equals(HttpConstants.METHOD_CONNECT)
                || method.equals(HttpConstants.METHOD_PATCH))
            {
                return doRequest(request);
            }
            else
            {
                return doBad(requestLine);
            }
        }

        protected HttpResponse doRequest(HttpRequest request) throws IOException, MuleException
        {
            sendExpect100(request);

            final MuleMessage message = createMuleMessage(request);

            String path = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
            int i = path.indexOf('?');
            if (i > -1)
            {
                path = path.substring(0, i);
            }

            message.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, path, PropertyScope.INBOUND);

            if (logger.isDebugEnabled())
            {
                logger.debug(message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
            }

            // determine if the request path on this request denotes a different receiver
            final MessageReceiver receiver = getTargetReceiver(message, endpoint);

            HttpResponse response;
            // the response only needs to be transformed explicitly if
            // A) the request was not served or B) a null result was returned
            if (receiver != null)
            {
                String contextPath = HttpConnector.normalizeUrl(receiver.getEndpointURI().getPath());
                message.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY,
                                    contextPath,
                                    PropertyScope.INBOUND);

                message.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY,
                                    receiver.getEndpointURI().getAddress(),
                                    PropertyScope.INBOUND);

                message.setProperty(HttpConnector.HTTP_RELATIVE_PATH_PROPERTY,
                                    processRelativePath(contextPath, path),
                                    PropertyScope.INBOUND);

                ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();

                MuleEvent returnEvent;
                try
                {
                    returnEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
                    {
                        @Override
                        public MuleEvent process() throws Exception
                        {
                            preRouteMessage(message);
                            return receiver.routeMessage(message);
                        }
                    });
                }
                catch (MuleException e)
                {
                    throw e;
                }
                catch (IOException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new DefaultMuleException(e);
                }

                MuleMessage returnMessage = returnEvent == null ? null : returnEvent.getMessage();

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
                    response = transformResponse(returnMessage, returnEvent);
                }

                response.setupKeepAliveFromRequestVersion(request.getRequestLine().getHttpVersion());
                HttpConnector httpConnector = (HttpConnector) connector;
                response.disableKeepAlive(!httpConnector.isKeepAlive());

                Header connectionHeader = request.getFirstHeader("Connection");
                if (connectionHeader != null)
                {
                    String value = connectionHeader.getValue();
                    boolean endpointOverride = getEndpointKeepAliveValue(endpoint);
                    if ("keep-alive".equalsIgnoreCase(value) && endpointOverride)
                    {
                        response.setKeepAlive(true);

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
                }
            }
            else
            {
                EndpointURI uri = endpoint.getEndpointURI();
                String failedPath = String.format("%s://%s:%d%s",
                                                  uri.getScheme(), uri.getHost(), uri.getPort(),
                                                  message.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                response = buildFailureResponse(request.getRequestLine().getHttpVersion(), HttpConstants.SC_NOT_FOUND,
                                                HttpMessages.cannotBindToAddress(failedPath).toString());
            }
            return response;
        }

        /**
         * Check if endpoint has a keep-alive property configured. Note the translation from
         * keep-alive in the schema to keepAlive here.
         */
        private boolean getEndpointKeepAliveValue(ImmutableEndpoint ep)
        {
            String value = (String) ep.getProperty("keepAlive");
            if (value != null)
            {
                return Boolean.parseBoolean(value);
            }
            return true;
        }


        protected HttpResponse doOtherValid(RequestLine requestLine, String method) throws MuleException
        {
            MuleMessage message = createMuleMessage(null);
            MuleEvent event = new DefaultMuleEvent(message, (InboundEndpoint) endpoint, flowConstruct);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_METHOD_NOT_ALLOWED);
            response.setBody(HttpMessages.methodNotAllowed(method).toString() + HttpConstants.CRLF);
            return transformResponse(response, event);
        }

        protected HttpResponse doBad(RequestLine requestLine) throws MuleException
        {
            MuleMessage message = createMuleMessage(null);
            MuleEvent event = new DefaultMuleEvent(message, (InboundEndpoint) endpoint, flowConstruct);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_BAD_REQUEST);
            response.setBody(HttpMessages.malformedSyntax().toString() + HttpConstants.CRLF);
            return transformResponse(response, event);
        }

        private void sendExpect100(HttpRequest request) throws MuleException, IOException
        {
            RequestLine requestLine = request.getRequestLine();

            // respond with status code 100, for Expect handshake
            // according to rfc 2616 and http 1.1
            // the processing will continue and the request will be fully
            // read immediately after
            HttpVersion requestVersion = requestLine.getHttpVersion();
            if (HttpVersion.HTTP_1_1.equals(requestVersion))
            {
                Header expectHeader = request.getFirstHeader(HttpConstants.HEADER_EXPECT);
                if (expectHeader != null)
                {
                    String expectHeaderValue = expectHeader.getValue();
                    if (HttpConstants.HEADER_EXPECT_CONTINUE_REQUEST_VALUE.equals(expectHeaderValue))
                    {
                        HttpResponse expected = new HttpResponse();
                        expected.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_CONTINUE);
                        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(expected,
                                                                                                   getEndpoint().getMuleContext()), (InboundEndpoint) endpoint, flowConstruct);
                        RequestContext.setEvent(event);
                        conn.writeResponse(transformResponse(expected, event));
                    }
                }
            }
        }

        private HttpResponse buildFailureResponse(MuleEvent event, String description, int httpStatusCode) throws MuleException
        {
            event.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY,httpStatusCode);
            event.getMessage().setPayload(description);
            return transformResponse(event.getMessage(), event);
        }

        protected HttpResponse buildFailureResponse(HttpVersion version, int statusCode, String description) throws MuleException
        {
            HttpResponse response = new HttpResponse();
            response.setStatusLine(version, statusCode);
            response.setBody(description);
            DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(response,
                                                                                 getEndpoint().getMuleContext()), (InboundEndpoint) endpoint, flowConstruct);
            RequestContext.setEvent(event);
            // The DefaultResponseTransformer will set the necessary headers
            return transformResponse(response, event);
        }

        protected void preRouteMessage(MuleMessage message) throws MessagingException
        {
            message.setProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS, remoteClientAddress, PropertyScope.INBOUND);
        }

        @Override
        public void release()
        {
            conn.close();
            conn = null;
        }
    }

    protected String processRelativePath(String contextPath, String path)
    {
        String relativePath = path.substring(contextPath.length());
        if(relativePath.startsWith("/"))
        {
            return relativePath.substring(1);
        }
        return relativePath;
    }

    protected MessageReceiver getTargetReceiver(MuleMessage message, ImmutableEndpoint ep)
            throws ConnectException
    {
        String path = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf('?');
        if (i > -1)
        {
            path = path.substring(0, i);
        }

        StringBuilder requestUri = new StringBuilder(80);
        if (path.indexOf("://") == -1)
        {
            requestUri.append(ep.getProtocol()).append("://");
            requestUri.append(ep.getEndpointURI().getHost());
            requestUri.append(':').append(ep.getEndpointURI().getPort());

            if (!"/".equals(path))
            {
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

            receiver = findReceiverByStem(connector.getReceivers(), uriStr);

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

    protected HttpResponse transformResponse(Object response, MuleEvent event) throws MuleException
    {
        MuleMessage message;
        if (response instanceof MuleMessage)
        {
            message = (MuleMessage) response;
        }
        else
        {
            message = new DefaultMuleMessage(response, getEndpoint().getMuleContext());
        }
        //TODO RM*: Maybe we can have a generic Transformer wrapper rather that using DefaultMuleMessage (or another static utility
        //class
        message.applyTransformers(null, defaultResponseTransformers, HttpResponse.class);
        return (HttpResponse) message.getPayload();
    }

    public static MessageReceiver findReceiverByStem(Map<Object, MessageReceiver> receivers, String uriStr)
    {
        int match = 0;
        MessageReceiver receiver = null;
        for (Map.Entry<Object, MessageReceiver> e : receivers.entrySet())
        {
            String key = (String) e.getKey();
            MessageReceiver candidate = e.getValue();
            if (uriStr.startsWith(key) && match < key.length())
            {
                match = key.length();
                receiver = candidate;
            }
        }
        return receiver;
    }

    @Override
    protected void initializeMessageFactory() throws InitialisationException
    {
        HttpMuleMessageFactory factory;
        try
        {
            factory = (HttpMuleMessageFactory) super.createMuleMessageFactory();

            boolean enableCookies = MapUtils.getBooleanValue(endpoint.getProperties(),
                                                             HttpConnector.HTTP_ENABLE_COOKIES_PROPERTY, ((HttpConnector) connector).isEnableCookies());
            factory.setEnableCookies(enableCookies);

            String cookieSpec = MapUtils.getString(endpoint.getProperties(),
                                                   HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, ((HttpConnector) connector).getCookieSpec());
            factory.setCookieSpec(cookieSpec);

            factory.setExchangePattern(endpoint.getExchangePattern());

            muleMessageFactory = factory;
        }
        catch (CreateException ce)
        {
            Message message = MessageFactory.createStaticMessage(ce.getMessage());
            throw new InitialisationException(message, ce, this);
        }
    }

    @Override
    protected MuleMessage handleUnacceptedFilter(MuleMessage message)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Message request '"
                         + message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY)
                         + "' is being rejected since it does not match the filter on this endpoint: "
                         + endpoint);
        }
        message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.SC_NOT_ACCEPTABLE));
        return message;
    }
}
