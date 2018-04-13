/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.execution.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.execution.ThrottlingPhaseTemplate;
import org.mule.module.http.internal.listener.HttpThrottlingHeadersMapBuilder;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.AbstractTransportMessageProcessTemplate;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.ServletResponseWriter;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JettyMessageProcessTemplate extends AbstractTransportMessageProcessTemplate implements RequestResponseFlowProcessingPhaseTemplate, ThrottlingPhaseTemplate
{

    private final HttpThrottlingHeadersMapBuilder httpThrottlingHeadersMapBuilder = new HttpThrottlingHeadersMapBuilder();;
    private final ServletResponseWriter servletResponseWriter = new ServletResponseWriter();
    private final HttpServletRequest request;
    private final HttpServletResponse servletResponse;
    private final MuleContext muleContext;
    private boolean failureResponseSentToClient;
    private Map<String, String> extraHeaders = new HashMap<String, String>();

    public JettyMessageProcessTemplate(HttpServletRequest request, HttpServletResponse response, AbstractMessageReceiver messageReceiver, MuleContext muleContext)
    {
        super(messageReceiver);
        this.request = request;
        this.servletResponse = response;
        this.muleContext = muleContext;
    }

    @Override
    public Object acquireMessage() throws MuleException
    {
        return request;
    }

    @Override
    protected MuleMessage createMessageFromSource(Object message) throws MuleException
    {
        MuleMessage muleMessage = super.createMessageFromSource(message);

        String contextPath = HttpConnector.normalizeUrl(getInboundEndpoint().getEndpointURI().getPath());
        muleMessage.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, contextPath, PropertyScope.INBOUND);

        return muleMessage;
    }

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        try
        {
            servletResponseWriter.writeNonHtmlErrorResponse(servletResponse, 429, "API calls exceeded", httpThrottlingHeadersMapBuilder.build());
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod, long timeUntilNextPeriodInMillis)
    {
        httpThrottlingHeadersMapBuilder.setThrottlingPolicyStatistics(remainingRequestInCurrentPeriod, maximumRequestAllowedPerPeriod, timeUntilNextPeriodInMillis);
    }

    @Override
    public void sendResponseToClient(MuleEvent muleEvent) throws MuleException
    {
        try
        {
            final MuleMessage message = muleEvent.getMessage();
            if (message == null)
            {
                servletResponseWriter.writeEmptyResponse(servletResponse, getThrottlingHeaders());
            }
            else
            {
                servletResponseWriter.writeResponse(servletResponse, message, getThrottlingHeaders());
            }
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    private Map<String, String> getThrottlingHeaders() {
        Map<String, String> throttlingHeaders = httpThrottlingHeadersMapBuilder.build();

        for ( String headerName : extraHeaders.keySet() )
        {
            throttlingHeaders.put(headerName, extraHeaders.get(headerName));
        }

        return throttlingHeaders;
    }

    @Override
    public void sendFailureResponseToClient(MessagingException messagingException) throws MuleException
    {
        try
        {
            int statusCode = retrieveStatusCode(messagingException);
            servletResponseWriter.writeErrorResponse(servletResponse, messagingException.getEvent().getMessage(), statusCode, messagingException.getMessage(), getThrottlingHeaders());
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
        failureResponseSentToClient = true;
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
                servletResponseWriter.writeErrorResponse(servletResponse, httpStatus, exception.getMessage(), new HashMap<String, String>());
            }
            catch (Exception e)
            {
                logger.warn("Exception sending Jetty HTTP response after error: " + e.getMessage());
                if (logger.isDebugEnabled())
                {
                    logger.debug(e);
                }
            }
        }
    }

    private int retrieveStatusCode(MessagingException messagingException)
    {
        final Object statusCodeValue = messagingException.getEvent().getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        if (statusCodeValue != null)
        {
            if (statusCodeValue instanceof Number)
            {
                return ((Number)statusCodeValue).intValue();
            }
            return Integer.valueOf((String) statusCodeValue);
        }
        else
        {
            return Integer.valueOf(ExceptionHelper.getErrorMapping("http", messagingException.getClass(), muleContext));
        }
    }

    protected ServletResponseWriter getServletResponseWriter()
    {
        return servletResponseWriter;
    }

    protected HttpServletResponse getServletResponse()
    {
        return servletResponse;
    }

    @Override
    public void addExtraHeader(String headerName, String value) {
        extraHeaders.put(headerName, value);
    }
}
