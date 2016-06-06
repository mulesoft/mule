/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.compatibility.core.DefaultMuleEventEndpointUtils;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.transport.AbstractTransportMessageProcessTemplate;
import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.execution.EndPhaseTemplate;
import org.mule.runtime.core.execution.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseDispatchException;
import org.mule.runtime.core.execution.ThrottlingPhaseTemplate;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.http.internal.listener.HttpMessageProcessorTemplate;
import org.mule.runtime.module.http.internal.listener.HttpThrottlingHeadersMapBuilder;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;

public class HttpMessageProcessTemplate extends AbstractTransportMessageProcessTemplate<HttpMessageReceiver, HttpConnector> implements RequestResponseFlowProcessingPhaseTemplate, ThrottlingPhaseTemplate, EndPhaseTemplate
{

    public static final int MESSAGE_DISCARD_STATUS_CODE = HttpMessageProcessorTemplate.MESSAGE_DISCARD_STATUS_CODE;
    public static final String MESSAGE_THROTTLED_REASON_PHRASE = "API calls exceeded";

    public static final String X_RATE_LIMIT_LIMIT_HEADER = HttpMessageProcessorTemplate.X_RATE_LIMIT_LIMIT_HEADER;
    public static final String X_RATE_LIMIT_REMAINING_HEADER = HttpMessageProcessorTemplate.X_RATE_LIMIT_REMAINING_HEADER;
    public static final String X_RATE_LIMIT_RESET_HEADER = HttpMessageProcessorTemplate.X_RATE_LIMIT_RESET_HEADER;

    private final HttpServerConnection httpServerConnection;
    private HttpRequest request;
    private boolean badRequest;
    private Latch messageProcessedLatch = new Latch();
    private RequestLine requestLine;
    private boolean failureResponseSentToClient;
    private HttpThrottlingHeadersMapBuilder httpThrottlingHeadersMapBuilder;

    public HttpMessageProcessTemplate(final HttpMessageReceiver messageReceiver, final HttpServerConnection httpServerConnection)
    {
        super(messageReceiver);
        this.httpServerConnection = httpServerConnection;
        this.httpThrottlingHeadersMapBuilder = new HttpThrottlingHeadersMapBuilder();
    }

    @Override
    public void sendResponseToClient(MuleEvent responseMuleEvent) throws MuleException
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
            boolean endpointOverride = getMessageReceiver().getEndpoint().getProperty("keepAlive") != null;
            boolean endpointKeepAliveValue = getEndpointKeepAliveValue(getMessageReceiver().getEndpoint());

            if (endpointOverride)
            {
                response.disableKeepAlive(!endpointKeepAliveValue);
            }
            else
            {
                response.disableKeepAlive(!httpConnector.isKeepAlive());
            }

            if (connectionHeader != null)
            {
                String value = connectionHeader.getValue();
                if ("keep-alive".equalsIgnoreCase(value) && endpointKeepAliveValue)
                {
                    response.setKeepAlive(true);

                    if (response.getHttpVersion().equals(HttpVersion.HTTP_1_0))
                    {
                        connectionHeader = new Header(HttpConstants.HEADER_CONNECTION, "Keep-Alive");
                        response.setHeader(connectionHeader);
                    }
                }
                else if ("close".equalsIgnoreCase(value) || !endpointKeepAliveValue)
                {
                    response.setKeepAlive(false);
                }
            }
            else if (request.getRequestLine().getHttpVersion().equals(HttpVersion.HTTP_1_1))
            {
                response.setKeepAlive(endpointKeepAliveValue);
            }

            try
            {
                httpServerConnection.writeResponse(response,getThrottlingHeaders());
            }
            catch (Exception e)
            {
                throw new ResponseDispatchException(responseMuleEvent, e);
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

    @Override
    public void sendFailureResponseToClient(MessagingException messagingException) throws MuleException
    {
        MuleEvent response = messagingException.getEvent();
        MessagingException e = getExceptionForCreatingFailureResponse(messagingException, response);
        String temp = ExceptionHelper.getErrorMapping(getInboundEndpoint().getConnector().getProtocol(), messagingException.getClass(), getMuleContext());
        int httpStatus = Integer.valueOf(temp);
        try
        {
            sendFailureResponseToClient(e, httpStatus);
        }
        catch (IOException ioException)
        {
            throw new DefaultMuleException(ioException);
        }
        failureResponseSentToClient = true;
    }

    private MessagingException getExceptionForCreatingFailureResponse(MessagingException messagingException, MuleEvent response)
    {
        MessagingException e = messagingException;
        if (response != null &&
            response.getMessage().getExceptionPayload() != null &&
            response.getMessage().getExceptionPayload().getException() instanceof MessagingException)
        {
            e = (MessagingException) response.getMessage().getExceptionPayload().getException();
        }
        return e;
    }

    @Override
    public void afterFailureProcessingFlow(MuleException exception) throws MuleException
    {
        if (!failureResponseSentToClient)
        {
            String temp = ExceptionHelper.getErrorMapping(getConnector().getProtocol(), exception.getClass(), getMuleContext());
            int httpStatus = Integer.valueOf(temp);
            try
            {
                sendFailureResponseToClient(httpStatus, exception.getMessage());
            }
            catch (Exception e)
            {
                logger.warn("Exception sending http response after error: " + e.getMessage());
                if (logger.isDebugEnabled())
                {
                    logger.debug(e);
                }
            }
        }
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
                            getMuleContext()), getFlowConstruct());
                    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event, getInboundEndpoint());

                    RequestContext.setEvent(event);
                    httpServerConnection.writeResponse(transformResponse(expected));
                }
            }
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
            message = new DefaultMuleMessage(response, getMessageReceiver().getEndpoint().getMuleContext());
        }
        //TODO RM*: Maybe we can have a generic Transformer wrapper rather that using DefaultMuleMessage (or another static utility
        //class
        return (HttpResponse) getMuleContext().getTransformationService().applyTransformers(message, null, getMessageReceiver()
                                                                                                    .getResponseTransportTransformers())
                                              .getPayload();
    }

    @Override
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

        processRemoteAddresses(muleMessage);
        return muleMessage;
    }

    /**
     *  For a given MuleMessage will set the <code>MULE_REMOTE_CLIENT_ADDRESS</code> property taking into consideration
     * if the header <code>X-Forwarded-For</code> is present in the request or not. In case it is, this method will
     * also set the <code>MULE_PROXY_ADDRESS</code> property. If a proxy address is not passed in
     * <code>X-Forwarded-For</code>, the connection address will be set as <code>MULE_PROXY_ADDRESS</code>.
     *
     * @param muleMessage MuleMessage to be enriched
     * @see <a href="https://en.wikipedia.org/wiki/X-Forwarded-For">https://en.wikipedia.org/wiki/X-Forwarded-For</a>
     */
    protected void processRemoteAddresses(MuleMessage muleMessage)
    {
        String xForwardedFor = muleMessage.getInboundProperty(HttpConstants.HEADER_X_FORWARDED_FOR);

        if (StringUtils.isEmpty(xForwardedFor))
        {
            muleMessage.setProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS,
                    httpServerConnection.getRemoteClientAddress(), PropertyScope.INBOUND);
            return;
        }

        String[] xForwardedForItems = StringUtils.splitAndTrim(xForwardedFor, ",");
        if (!ArrayUtils.isEmpty(xForwardedForItems))
        {
            muleMessage.setProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS,
                    xForwardedForItems[0], PropertyScope.INBOUND);
            if (xForwardedForItems.length > 1)
            {
                muleMessage.setProperty(MuleProperties.MULE_PROXY_ADDRESS,
                        xForwardedForItems[xForwardedForItems.length-1], PropertyScope.INBOUND);
            }
            else
            {
                // If only one address has been passed, we can assume the connection address is a proxy
                muleMessage.setProperty(MuleProperties.MULE_PROXY_ADDRESS,
                        httpServerConnection.getRemoteClientAddress(), PropertyScope.INBOUND);
            }
        }
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

    @Override
    public boolean validateMessage()
    {
        try
        {
            this.requestLine = httpServerConnection.getRequestLine();
            if (requestLine == null)
            {
                return false;
            }

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
                httpServerConnection.writeResponse(doBad(requestLine));
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

    protected HttpResponse doBad(RequestLine requestLine) throws MuleException
    {
        MuleMessage message = getMessageReceiver().createMuleMessage(null);
        DefaultMuleEvent event = new DefaultMuleEvent(message, getFlowConstruct());
        DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event, getInboundEndpoint());
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

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        try
        {
            sendFailureResponseToClient(MESSAGE_DISCARD_STATUS_CODE, MESSAGE_THROTTLED_REASON_PHRASE);
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod, long timeUntilNextPeriodInMillis)
    {
        httpThrottlingHeadersMapBuilder.setThrottlingPolicyStatistics(remainingRequestInCurrentPeriod, maximumRequestAllowedPerPeriod, timeUntilNextPeriodInMillis);
    }

    private void sendFailureResponseToClient(int httpStatus, String message) throws IOException
    {
        httpServerConnection.writeFailureResponse(httpStatus,message,getThrottlingHeaders());
    }

    private void sendFailureResponseToClient(MessagingException exception, int httpStatus) throws IOException, MuleException
    {
        MuleEvent response = exception.getEvent();
        response.getMessage().setPayload(exception.getMessage());
        httpStatus = response.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY) != null ? Integer.valueOf(response.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString()) : httpStatus;
        response.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, httpStatus);
        HttpResponse httpResponse = transformResponse(response.getMessage());
        httpServerConnection.writeResponse(httpResponse, getThrottlingHeaders());
    }

    private Map<String,String> getThrottlingHeaders()
    {
        return httpThrottlingHeadersMapBuilder.build();
    }

    @Override
    public void messageProcessingEnded()
    {
        messageProcessedLatch.release();
    }


    public void awaitTermination() throws InterruptedException
    {
        this.messageProcessedLatch.await();
    }
}
