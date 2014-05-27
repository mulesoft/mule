/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.ExceptionHelper;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A base servlet used to receive requests from a servlet container and route
 * them into Mule
 */

public abstract class AbstractReceiverServlet extends HttpServlet
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public static final String REQUEST_TIMEOUT_PROPERTY = "org.mule.servlet.timeout";
    public static final String FEEDBACK_PROPERTY = "org.mule.servlet.feedback";
    public static final String DEFAULT_CONTENT_TYPE_PROPERTY = "org.mule.servlet.default.content.type";

    /**
     * The name of the servlet connector to use with this Servlet
     * @deprecated Use {@link org.mule.transport.servlet.MuleServletContextListener#CONNECTOR_NAME} instead
     */
    @Deprecated
    public static final String SERVLET_CONNECTOR_NAME_PROPERTY = "org.mule.servlet.connector.name";

    public static final String PAYLOAD_PARAMETER_NAME = "org.mule.servlet.payload.param";
    public static final String DEFAULT_PAYLOAD_PARAMETER_NAME = "payload";

    public static final long DEFAULT_GET_TIMEOUT = 10000L;

    protected String payloadParameterName;
    protected long timeout = DEFAULT_GET_TIMEOUT;
    protected boolean feedback = true;
    protected String defaultContentType = HttpConstants.DEFAULT_CONTENT_TYPE;
    protected MuleContext muleContext;

    private final MuleMessageToHttpResponse responseTransformer = new MuleMessageToHttpResponse();
    private final ServletResponseWriter servletResponseWriter = new ServletResponseWriter().setFeedbackOnEmptyResponse(true);

    @Override
    public final void init() throws ServletException
    {
        String timeoutString = getServletConfig().getInitParameter(REQUEST_TIMEOUT_PROPERTY);
        if (timeoutString != null)
        {
            timeout = Long.parseLong(timeoutString);
        }
        if (logger.isInfoEnabled())
        {
            logger.info("Default request timeout for GET methods is: " + timeout);
        }

        String feedbackString = getServletConfig().getInitParameter(FEEDBACK_PROPERTY);
        if (feedbackString != null)
        {
            feedback = Boolean.valueOf(feedbackString);
        }
        if (logger.isInfoEnabled())
        {
            logger.info("feedback is set to: " + feedback);
        }

        String ct = getServletConfig().getInitParameter(DEFAULT_CONTENT_TYPE_PROPERTY);
        if (ct != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Using default content type configured on the servlet (" + DEFAULT_CONTENT_TYPE_PROPERTY + ") = " + ct);
            }
            defaultContentType = ct;
        }
        if (logger.isInfoEnabled())
        {
            logger.info("Default content type is: " + defaultContentType);
        }

        payloadParameterName = getServletConfig().getInitParameter(PAYLOAD_PARAMETER_NAME);
        if (payloadParameterName == null)
        {
            payloadParameterName = DEFAULT_PAYLOAD_PARAMETER_NAME;
        }
        if (logger.isInfoEnabled())
        {
            logger.info("Using payload param name: " + payloadParameterName);
        }

        muleContext = setupMuleContext();
        setupResponseTransformer();
        doInit();
    }

    protected MuleContext setupMuleContext() throws ServletException
    {
        MuleContext context = (MuleContext) getServletContext().getAttribute(MuleProperties.MULE_CONTEXT_PROPERTY);
        if (context == null)
        {
            throw new ServletException("Property " + MuleProperties.MULE_CONTEXT_PROPERTY + " not set on ServletContext");
        }
        return context;
    }

    protected void setupResponseTransformer() throws ServletException
    {
        responseTransformer.setMuleContext(muleContext);

        try
        {
            responseTransformer.initialise();
        }
        catch (InitialisationException e)
        {
            throw new ServletException(e);
        }
    }

    protected void doInit() throws ServletException
    {
        // template method
    }

    protected void writeResponse(HttpServletResponse servletResponse, MuleMessage message) throws Exception
    {
        if (message == null)
        {
            servletResponseWriter.writeEmptyResponse(servletResponse, null);
        }
        else
        {
            servletResponseWriter.writeResponse(servletResponse, message, null);
        }
    }

    protected void handleException(Throwable exception, String message, HttpServletResponse response)
    {
        logger.error("message: " + exception.getMessage(), exception);
        int code = Integer.valueOf(ExceptionHelper.getErrorMapping("http", exception.getClass(),muleContext));
        response.setStatus(code);
        try
        {
            String errorMessage = message + ": " + exception.getMessage();
            if (exception instanceof MessagingException && ((MessagingException) exception).getEvent() != null)
            {
                MessagingException me = (MessagingException) exception;
                writeErrorResponseFromMessage(response, me.getEvent().getMessage(), code, errorMessage);
            }
            else
            {
                response.sendError(code, errorMessage);
            }
        }
        catch (Exception e)
        {
            logger.error("Failed to sendError on response: " + e.getMessage(), e);
        }
    }

    protected void writeErrorResponseFromMessage(HttpServletResponse servletResponse, MuleMessage message, int errorCode, String errorMessage) throws Exception
    {
        servletResponseWriter.writeErrorResponse(servletResponse, message, errorCode, errorMessage, null);
    }

}
