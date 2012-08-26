/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.config.ExceptionHelper;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
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

    private MuleMessageToHttpResponse responseTransformer = new MuleMessageToHttpResponse();

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
            writeEmptyResponse(servletResponse);
        }
        else
        {
            writeResponseFromMessage(servletResponse, message);
        }
        servletResponse.flushBuffer();
    }

    protected void writeEmptyResponse(HttpServletResponse servletResponse) throws IOException
    {
        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
        if (feedback)
        {
            servletResponse.setStatus(HttpServletResponse.SC_OK);
            servletResponse.getWriter().write("Action was processed successfully. There was no result");
        }
    }

    protected void writeResponseFromMessage(HttpServletResponse servletResponse, MuleMessage message) throws Exception
    {
        HttpResponse httpResponse = convertToHttpResponse(message);
        setHttpHeadersOnServletResponse(httpResponse, servletResponse);

        if (!servletResponse.isCommitted())
        {
            servletResponse.setStatus(httpResponse.getStatusCode());
        }

        if (httpResponse.hasBody())
        {
            OutputHandler outputHandler = httpResponse.getBody();
            outputHandler.write(RequestContext.getEvent(), servletResponse.getOutputStream());
        }
    }

    protected HttpResponse convertToHttpResponse(MuleMessage message) throws TransformerException
    {
        if (message.getPayload() instanceof HttpResponse)
        {
            return (HttpResponse) message.getPayload();

        }
        else
        {
            return (HttpResponse) responseTransformer.transform(message);
        }
    }

    protected HttpServletResponse setHttpHeadersOnServletResponse(HttpResponse httpResponse, HttpServletResponse servletResponse)
    {
        // Remove any Transfer-Encoding headers that were set (e.g. by MuleMessageToHttpResponse)
        // earlier. Mule's default HTTP transformer is used in both cases: when the reply
        // MuleMessage is generated for our standalone HTTP server and for the servlet case. The
        // servlet container should be able to figure out the Transfer-Encoding itself and some
        // get confused by an existing header.
        httpResponse.removeHeaders(HttpConstants.HEADER_TRANSFER_ENCODING);

        Header[] headers = httpResponse.getHeaders();
        for (Header header : headers)
        {
            servletResponse.addHeader(header.getName(), header.getValue());
        }

        ensureContentTypeHeaderIsSet(servletResponse, httpResponse);

        return servletResponse;
    }

    protected void ensureContentTypeHeaderIsSet(HttpServletResponse servletResponse, HttpResponse httpResponse)
    {
        Header contentTypeHeader = httpResponse.getFirstHeader(HttpConstants.HEADER_CONTENT_TYPE);
        String contentType = defaultContentType;
        if (contentTypeHeaderIsValid(contentTypeHeader))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Using Content-Type from message header = " + contentTypeHeader.getValue());
            }
            contentType = contentTypeHeader.getValue();
        }
        servletResponse.setContentType(contentType);
    }

    protected boolean contentTypeHeaderIsValid(Header header)
    {
        return (header != null) && (header.getValue() != null);
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
        HttpResponse httpResponse = convertToHttpResponse(message);
        setHttpHeadersOnServletResponse(httpResponse, servletResponse);
        servletResponse.sendError(errorCode, errorMessage);
    }

}
