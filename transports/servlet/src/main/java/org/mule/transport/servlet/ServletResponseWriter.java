/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Writes a servlet response based on a mule message or basic attributes of a response.
 */
public class ServletResponseWriter
{

    private transient Log logger = LogFactory.getLog(getClass());
    private MuleMessageToHttpResponse responseTransformer = new MuleMessageToHttpResponse();
    private String defaultContentType = HttpConstants.DEFAULT_CONTENT_TYPE;
    private boolean feedbackOnEmptyResponse;

    /**
     * Writes a servlet response based on a mule message.
     *
     * @param servletResponse response object
     * @param message must be a not null message which content will be sent as response
     * @param httpHeaders headers to be set in the response object. Can be null.
     * @throws Exception
     */
    public void writeResponse(HttpServletResponse servletResponse, MuleMessage message, Map<String, String> httpHeaders) throws Exception
    {
        addHeaders(servletResponse, httpHeaders);
        writeResponseFromMessage(servletResponse, message);
        servletResponse.flushBuffer();
    }

    /**
     * Writes an empty {@link javax.servlet.http.HttpServletResponse}.
     *
     * @param servletResponse response object
     * @param httpHeaders headers to be set in the response object. Can be null.
     * @throws IOException
     */
    public void writeEmptyResponse(HttpServletResponse servletResponse, Map<String, String> httpHeaders) throws IOException
    {
        addHeaders(servletResponse, httpHeaders);
        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
        if (feedbackOnEmptyResponse)
        {
            servletResponse.setStatus(HttpServletResponse.SC_OK);
            servletResponse.getWriter().write("Action was processed successfully. There was no result");
        }
        servletResponse.flushBuffer();
    }

    /**
     * Writes a servlet response with an error code based on a mule message.
     *
     * @param servletResponse response object
     * @param message message with the content of the response
     * @param errorCode http error code to be sent back to the client
     * @param errorMessage http error message
     * @param httpHeaders headers to be set in the response object. Can be null.
     * @throws Exception
     */
    public void writeErrorResponse(HttpServletResponse servletResponse, MuleMessage message, int errorCode, String errorMessage, Map<String, String> httpHeaders) throws Exception
    {
        addHeaders(servletResponse, httpHeaders);
        HttpResponse httpResponse = convertToHttpResponse(message);
        setHttpHeadersOnServletResponse(httpResponse, servletResponse);
        servletResponse.sendError(errorCode, errorMessage);
    }

    /**
     * Writes a servlet response with an error code with html code wrapping the error message.
     *
     * @param servletResponse response object
     * @param errorCode http error code to be sent back to the client
     * @param errorMessage http error message
     * @param httpHeaders headers to be set in the response object. Can be null.
     * @throws Exception
     */
    public void writeErrorResponse(HttpServletResponse servletResponse, int errorCode, String errorMessage, Map<String, String> httpHeaders) throws Exception
    {
        addHeaders(servletResponse, httpHeaders);
        servletResponse.sendError(errorCode, errorMessage);
    }

    /**
     * Writes a servlet response with an error code without html code wrapping the error message.
     *
     * @param servletResponse response object
     * @param errorCode http error code to be sent back to the client
     * @param errorMessage http error message
     * @param httpHeaders headers to be set in the response object. Can be null.
     * @throws Exception
     */
    public void writeNonHtmlErrorResponse(HttpServletResponse servletResponse, int errorCode, String errorMessage, Map<String, String> httpHeaders) throws Exception
    {
        addHeaders(servletResponse, httpHeaders);
        servletResponse.setStatus(errorCode);
        servletResponse.getOutputStream().write(errorMessage.getBytes());
        servletResponse.flushBuffer();
    }

    private void writeResponseFromMessage(HttpServletResponse servletResponse, MuleMessage message) throws Exception
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

    private HttpResponse convertToHttpResponse(MuleMessage message) throws TransformerException
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

    private void addHeaders(HttpServletResponse servletResponse, Map<String, String> httpHeaders)
    {
        if (httpHeaders != null)
        {
            for (String httpHeaderKey : httpHeaders.keySet())
            {
                servletResponse.addHeader(httpHeaderKey, httpHeaders.get(httpHeaderKey));
            }
        }
    }

    private HttpServletResponse setHttpHeadersOnServletResponse(HttpResponse httpResponse, HttpServletResponse servletResponse)
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

    private void ensureContentTypeHeaderIsSet(HttpServletResponse servletResponse, HttpResponse httpResponse)
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

    private boolean contentTypeHeaderIsValid(Header header)
    {
        return (header != null) && (header.getValue() != null);
    }

    /**
     * @param feedbackOnEmptyResponse if set to true, when an empty response is sent back to the client a message will be add to the response body.
     */
    public ServletResponseWriter setFeedbackOnEmptyResponse(boolean feedbackOnEmptyResponse)
    {
        this.feedbackOnEmptyResponse = feedbackOnEmptyResponse;
        return this;
    }
}
