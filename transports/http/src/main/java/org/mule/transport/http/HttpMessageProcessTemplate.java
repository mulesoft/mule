/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.message.processing.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.transport.AbstractTransportMessageProcessTemplate;
import org.mule.message.processing.EndPhaseTemplate;
import org.mule.message.processing.ThrottlingPhaseTemplate;
import org.mule.transport.NullPayload;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.concurrent.Latch;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;

public class HttpMessageProcessTemplate extends AbstractTransportMessageProcessTemplate<HttpMessageReceiver, HttpConnector> implements ThrottlingPhaseTemplate, EndPhaseTemplate, RequestResponseFlowProcessingPhaseTemplate
{

    public static final int MESSAGE_DISCARD_STATUS_CODE = Integer.valueOf(System.getProperty("mule.transport.http.throttling.discardstatuscode","429"));

    private final HttpServerConnection httpServerConnection;
    private HttpRequest request;
    private boolean badRequest;
    private Latch messageProcessedLatch = new Latch();
    private boolean failureSendingResponse;

    public HttpMessageProcessTemplate(final HttpMessageReceiver messageReceiver, final HttpServerConnection httpServerConnection, final WorkManager flowExecutionWorkManager)
    {
        super(messageReceiver,flowExecutionWorkManager);
        this.httpServerConnection = httpServerConnection;
    }

    @Override
    public void afterFailureProcessingFlow(MessagingException messagingException) throws MuleException
    {
        if (!failureSendingResponse)
        {
            Exception e = messagingException;
            MuleEvent response = messagingException.getEvent();
            if (response != null &&
                response.getMessage().getExceptionPayload() != null &&
                response.getMessage().getExceptionPayload().getException() instanceof MessagingException)
            {
                e = (Exception) response.getMessage().getExceptionPayload().getException();
            }

            String temp = ExceptionHelper.getErrorMapping(getInboundEndpoint().getConnector().getProtocol(), messagingException.getClass(), getMuleContext());
            int httpStatus = Integer.valueOf(temp);
            try
            {
                if (e instanceof  MessagingException)
                {
                    httpStatus = response.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY) != null ? Integer.valueOf(response.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString()) : httpStatus;
                    httpServerConnection.writeFailureResponse(httpStatus,e.getMessage());
                }
                else
                {
                    httpServerConnection.writeFailureResponse(httpStatus, e.getMessage());
                }
            }
            catch (IOException ioException)
            {
                throw new DefaultMuleException(ioException);
            }
        }
    }

    @Override
    public void afterFailureProcessingFlow(MuleException exception) throws MuleException
    {
        if (!failureSendingResponse)
        {
            String temp = ExceptionHelper.getErrorMapping(getConnector().getProtocol(), exception.getClass(),getMuleContext());
            int httpStatus = Integer.valueOf(temp);
            try
            {
                httpServerConnection.writeFailureResponse(httpStatus, exception.getMessage());
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

    @Override
    public void sendResponseToClient(MuleEvent muleEvent) throws MuleException
    {
        sendHttpResponse(muleEvent);
    }

    @Override
    public MuleEvent beforeRouteEvent(MuleEvent muleEvent) throws MuleException
    {
        try
        {
            sendExpect100(request);
            return muleEvent;
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
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
                                                  getMuleContext()), getInboundEndpoint(), getFlowConstruct());
                    RequestContext.setEvent(event);
                    httpServerConnection.writeResponse(transformResponse(expected));
                }
            }
        }
    }

    private void sendHttpResponse(MuleEvent responseMuleEvent) throws MessagingException
    {
        try
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Sending http response");
            }
            MuleMessage returnMessage = responseMuleEvent == null ? null : responseMuleEvent.getMessage();

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
            HttpResponse response;
            if (tempResponse instanceof HttpResponse)
            {
                response = (HttpResponse) tempResponse;
            }
            else
            {
                response = transformResponse(returnMessage);
            }

            response.setupKeepAliveFromRequestVersion(request.getRequestLine().getHttpVersion());
            HttpConnector httpConnector = (HttpConnector) getMessageReceiver().getEndpoint().getConnector();
            response.disableKeepAlive(!httpConnector.isKeepAlive());

            Header connectionHeader = request.getFirstHeader("Connection");
            if (connectionHeader != null)
            {
                String value = connectionHeader.getValue();
                boolean endpointOverride = getEndpointKeepAliveValue(getMessageReceiver().getEndpoint());
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
            try
            {
                httpServerConnection.writeResponse(response);
            }
            catch (Exception e)
            {
                failureSendingResponse = true;
            }
            if (logger.isTraceEnabled())
            {
                logger.trace("HTTP response sent successfully");
            }
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Exception while sending http response");
                logger.debug(e);
            }
            throw new MessagingException(responseMuleEvent,e);
        }
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

    protected HttpResponse transformResponse(Object response) throws MuleException
    {
        MuleMessage message;
        if (response instanceof MuleMessage)
        {
            message = (MuleMessage) response;
        }
        else
        {
            message = new DefaultMuleMessage(response, getMessageReceiver().getEndpoint().getConnector().getMuleContext());
        }
        //TODO RM*: Maybe we can have a generic Transformer wrapper rather that using DefaultMuleMessage (or another static utility
        //class
        message.applyTransformers(null, getMessageReceiver().getResponseTransportTransformers(), HttpResponse.class);
        return (HttpResponse) message.getPayload();
    }

    protected MuleMessage createMessageFromSource(Object message) throws MuleException
    {
        MuleMessage muleMessage = super.createMessageFromSource(message);
        String path = muleMessage.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf('?');
        if (i > -1)
        {
            path = path.substring(0, i);
        }

        muleMessage.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, path, PropertyScope.INBOUND);

        if (logger.isDebugEnabled())
        {
            logger.debug(muleMessage.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
        }

        // determine if the request path on this request denotes a different receiver
        //final MessageReceiver receiver = getTargetReceiver(message, endpoint);

        // the response only needs to be transformed explicitly if
        // A) the request was not served or B) a null result was returned
        String contextPath = HttpConnector.normalizeUrl(getInboundEndpoint().getEndpointURI().getPath());
        muleMessage.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY,
                            contextPath,
                            PropertyScope.INBOUND);

        muleMessage.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY,
                                getInboundEndpoint().getEndpointURI().getAddress(),
                            PropertyScope.INBOUND);

        muleMessage.setProperty(HttpConnector.HTTP_RELATIVE_PATH_PROPERTY,
                            processRelativePath(contextPath, path),
                            PropertyScope.INBOUND);

        muleMessage.setProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS, httpServerConnection.getRemoteClientAddress(), PropertyScope.INBOUND);
        return muleMessage;
    }

    protected String processRelativePath(String contextPath, String path)
    {
        String relativePath = path.substring(contextPath.length());
        if (relativePath.startsWith("/"))
        {
            return relativePath.substring(1);
        }
        return relativePath;
    }

    @Override
    public Object acquireMessage() throws MuleException
    {
        final HttpRequest request;
        try
        {
            request = httpServerConnection.readRequest();
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
        if (request == null)
        {
            throw new HttpMessageReceiver.EmptyRequestException();
        }
        this.request = request;
        return request;
    }

    public boolean validateMessage()
    {
        try
        {
            this.request = httpServerConnection.readRequest();
            if (request == null)
            {
                return false;
            }

            RequestLine requestLine = request.getRequestLine();
            String method = requestLine.getMethod();

            if (!(method.equals(HttpConstants.METHOD_GET)
                || method.equals(HttpConstants.METHOD_HEAD)
                || method.equals(HttpConstants.METHOD_POST)
                || method.equals(HttpConstants.METHOD_OPTIONS)
                || method.equals(HttpConstants.METHOD_PUT)
                || method.equals(HttpConstants.METHOD_DELETE)
                || method.equals(HttpConstants.METHOD_TRACE)
                || method.equals(HttpConstants.METHOD_CONNECT)
                || method.equals(HttpConstants.METHOD_PATCH)))
            {
                badRequest = true;
                return false;
            }
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }


    @Override
    public void discardInvalidMessage() throws MuleException
    {
        if (badRequest)
        {
            try
            {
                httpServerConnection.writeResponse(doBad(request.getRequestLine()));
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

    @Override
    public boolean supportsAsynchronousProcessing()
    {
        return true;
    }

    protected HttpResponse doBad(RequestLine requestLine) throws MuleException
    {
        MuleMessage message = getMessageReceiver().createMuleMessage(null);
        MuleEvent event = new DefaultMuleEvent(message, getInboundEndpoint(), getFlowConstruct());
        OptimizedRequestContext.unsafeSetEvent(event);
        HttpResponse response = new HttpResponse();
        response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_BAD_REQUEST);
        response.setBody(HttpMessages.malformedSyntax().toString() + HttpConstants.CRLF);
        return transformResponse(response);
    }

    protected HttpServerConnection getHttpServerConnection()
    {
        return httpServerConnection;
    }

    public Latch getMessageProcessedLatch()
    {
        return messageProcessedLatch;
    }

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        try
        {
            httpServerConnection.writeFailureResponse(MESSAGE_DISCARD_STATUS_CODE, "API calls exceeded");
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void messageProcessingEnded()
    {
        messageProcessedLatch.release();
    }


}
