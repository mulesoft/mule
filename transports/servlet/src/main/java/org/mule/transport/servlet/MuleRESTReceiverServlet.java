/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <code>MuleRESTReceiverServlet</code> is used for sending and receiving events from
 * the Mule server via a servlet container. The servlet uses the REST style of request
 * processing. <p/>
 * GET METHOD will do a request from an endpoint if an endpoint parameter is set otherwise 
 * it behaves the same way as POST. You can either specify the endpoint URL REST-style, 
 * e.g., to read from jms://orders.queue <p/>
 * http://www.mycompany.com/rest/jms/orders/queue <p/> or a logical Mule endpoint name, 
 * e.g., this would get the first email message received by the orderEmailInbox endpoint. <p/>
 * http://www.mycompany.com/rest/ordersEmailInbox <p/> 
 * POST METHOD Do a synchronous call and return a result 
 * http://www.clientapplication.com/service/clientquery?custId=1234 <p/> 
 * PUT METHOD Do an asynchronous call without returning a result (other than an http
 * status code) http://www.clientapplication.com/service/orders?payload=<order>more
 * beer</order> <p/> 
 * DELETE METHOD Same as GET only without returning a result
 */

public class MuleRESTReceiverServlet extends MuleReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2395763805839859649L;

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws ServletException, IOException
    {
        try
        {
            InboundEndpoint endpoint = getEndpointForURI(httpServletRequest);
            if (endpoint != null)
            {
                String timeoutString = httpServletRequest.getParameter("timeout");
                long to = timeout;

                if (timeoutString != null)
                {
                    to = Long.parseLong(timeoutString);
                }

                if (logger.isDebugEnabled())
                {
                    logger.debug("Making request using endpoint: " + endpoint.toString() + " timeout is: "
                                 + to);
                }

                MuleMessage returnMessage = endpoint.request(to);
                writeResponse(httpServletResponse, returnMessage);
            }
            else
            {
                MessageReceiver receiver = getReceiverForURI(httpServletRequest);
            
                httpServletRequest.setAttribute(PAYLOAD_PARAMETER_NAME, payloadParameterName);
                
                MuleMessage message = receiver.createMuleMessage(httpServletRequest);
                MuleEvent event = receiver.routeMessage(message);
                MuleMessage returnMessage = !receiver.getEndpoint().getExchangePattern().hasResponse() || event == null ? null : event.getMessage();
                writeResponse(httpServletResponse, returnMessage);
            }
        }
        catch (Exception e)
        {
            handleException(e, "Failed to route event through Servlet Receiver", httpServletResponse);
        }
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws ServletException, IOException
    {
        try
        {
            MessageReceiver receiver = getReceiverForURI(httpServletRequest);

            httpServletRequest.setAttribute(PAYLOAD_PARAMETER_NAME, payloadParameterName);

            MuleMessage message = receiver.createMuleMessage(httpServletRequest, 
                receiver.getEndpoint().getEncoding());
            
            MuleEvent event = receiver.routeMessage(message);
            MuleMessage returnMessage = !receiver.getEndpoint().getExchangePattern().hasResponse() || event == null ? null : event.getMessage();
            writeResponse(httpServletResponse, returnMessage);
        }
        catch (Exception e)
        {
            handleException(e, "Failed to Post event to Mule", httpServletResponse);
        }
    }

    @Override
    protected void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws ServletException, IOException
    {
        try
        {
            MessageReceiver receiver = getReceiverForURI(httpServletRequest);

            httpServletRequest.setAttribute(PAYLOAD_PARAMETER_NAME, payloadParameterName);

            MuleMessage message = receiver.createMuleMessage(httpServletRequest, 
                receiver.getEndpoint().getEncoding());
            receiver.routeMessage(message);

            httpServletResponse.setStatus(HttpServletResponse.SC_CREATED);
            if (feedback)
            {
                httpServletResponse.getWriter().write(
                    "Item was created at endpointUri: " + receiver.getEndpointURI());
            }
        }
        catch (Exception e)
        {
            handleException(e, "Failed to Post event to Mule" + e.getMessage(), httpServletResponse);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws ServletException, IOException
    {
        try
        {
            InboundEndpoint endpoint = getEndpointForURI(httpServletRequest);
            String timeoutString = httpServletRequest.getParameter("timeout");
            long to = timeout;

            if (timeoutString != null)
            {
                to = new Long(timeoutString).longValue();
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Making request using endpoint: " + endpoint.toString() + " timeout is: " + to);
            }

            MuleMessage returnMessage = endpoint.request(to);
            if (returnMessage != null)
            {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            }
            else
            {
                httpServletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        }
        catch (Exception e)
        {
            handleException(e, "Failed to Delete mule event via receive using uri: "
                               + httpServletRequest.getPathInfo(), httpServletResponse);
        }
    }

    protected InboundEndpoint getEndpointForURI(HttpServletRequest httpServletRequest)
        throws MuleException
    {
        String endpointName = httpServletRequest.getParameter("endpoint");
        if (endpointName == null)
        {
            // Let's try stripping the path and only use the last path element
            String uri = httpServletRequest.getPathInfo();
            int i = uri.lastIndexOf("/");
            if (i > -1)
            {
                endpointName = uri.substring(i + 1);
            }
        }

        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(endpointName);
        if (endpoint == null)
        {
            // if we dont find an endpoint for the given name, lets check the
            // servlet receivers
            MessageReceiver receiver = getReceivers().get(endpointName);
            
            if (receiver != null)
            {
                endpoint = receiver.getEndpoint();
            }
        }
        return endpoint;
    }
}
