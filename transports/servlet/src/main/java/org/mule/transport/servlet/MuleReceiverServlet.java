/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.DefaultMuleMessage;
import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.endpoint.DynamicURIInboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.servlet.i18n.ServletMessages;
import org.mule.util.PropertiesUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives Http requests via a Servlet and routes them to listeners with servlet://
 * endpoints
 * <p/>
 * There needs to be a ServletConnector configured on the Mule Server, this connector
 * must have the servletUrl property set that matches the Url for the container that this
 * Servlet is hosted in, i.e. something like http://192.168.10.21:8888
 */

public class MuleReceiverServlet extends AbstractReceiverServlet
{
    /** Serial version */
    private static final long serialVersionUID = 6631307373079767439L;

    protected ServletConnector connector = null;

    protected void doInit(ServletConfig servletConfig) throws ServletException
    {
        String servletConnectorName = servletConfig.getInitParameter(SERVLET_CONNECTOR_NAME_PROPERTY);
        if (servletConnectorName == null)
        {
            connector = (ServletConnector) TransportFactory.getConnectorByProtocol("servlet");
            if (connector == null)
            {
                connector = new ServletConnector();
                try
                {
                    RegistryContext.getRegistry().registerConnector(connector);
                }
                catch (MuleException e)
                {
                    throw new ServletException("Failed to register the AjaxServletConnector", e);
                }
                //throw new ServletException(ServletMessages.noConnectorForProtocolServlet().toString());
            }
        }
        else
        {
            connector = (ServletConnector) RegistryContext.getRegistry().lookupConnector(servletConnectorName);
            if (connector == null)
            {
                throw new ServletException(ServletMessages.noServletConnectorFound(servletConnectorName).toString());
            }
        }
    }

    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "HEAD");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
            else
            {
                response.setStatus(HttpConstants.SC_OK);
            }
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "GET");
            writeResponse(response, responseMessage);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void setupRequestMessage(HttpServletRequest request,
                                       MuleMessage requestMessage,
                                       MessageReceiver receiver)
    {

        EndpointURI uri = receiver.getEndpointURI();
        String reqUri = request.getRequestURI().toString();
        requestMessage.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, reqUri);
        
        String queryString = request.getQueryString();
        if (queryString != null) 
        {
            reqUri += "?"+queryString;
        }

        requestMessage.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, reqUri);
        
        String path;
        if ("servlet".equals(uri.getScheme())) 
        {
            path = HttpConnector.normalizeUrl(request.getContextPath());
            if ("/".equals(path))
            {
                path = HttpConnector.normalizeUrl(request.getServletPath());
            } 
            else 
            {
                path = path + HttpConnector.normalizeUrl(request.getServletPath());
            }
            
            String pathPart2 = uri.getAddress();

            if (!path.endsWith("/"))
            {
                // "/foo" + "bar"
                path = path + HttpConnector.normalizeUrl(pathPart2);
            }
            else if (pathPart2.startsWith("/"))
            {
                // "/foo/" + "/bar"
                path = path + pathPart2.substring(1);
            }
            else
            {
                // "/foo/" + "bar"
                path = path + pathPart2;
            }
        } 
        else 
        {
            // The Jetty transport has normal paths
            path = HttpConnector.normalizeUrl(uri.getPath());
        }
        
        requestMessage.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, path);
        
        // Call this to keep API compatability
        setupRequestMessage(request, requestMessage);
    }

    protected void setupRequestMessage(HttpServletRequest request, MuleMessage requestMessage)
    {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "POST");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected MuleMessage doMethod(HttpServletRequest request, String method)
        throws MuleException
    {
        MessageReceiver receiver = getReceiverForURI(request);
        
        MuleMessage requestMessage = new DefaultMuleMessage(new HttpRequestMessageAdapter(request));
        requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        
        setupRequestMessage(request, requestMessage, receiver);
        
        return routeMessage(receiver, requestMessage, request);
    }

    protected MuleMessage routeMessage(MessageReceiver receiver, MuleMessage requestMessage, HttpServletRequest request)
        throws MuleException
    {
        return receiver.routeMessage(requestMessage, true);
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "OPTIONS");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "PUT");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
        }

        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "DELETE");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void doTrace(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "TRACE");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected void doConnect(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = doMethod(request, "CONNECT");
            if (responseMessage != null)
            {
                writeResponse(response, responseMessage);
            }
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    protected MessageReceiver getReceiverForURI(HttpServletRequest httpServletRequest)
            throws EndpointException
    {
        String uri = getReceiverName(httpServletRequest);
        if (uri == null)
        {
            throw new EndpointException(
                    HttpMessages.unableToGetEndpointUri(httpServletRequest.getRequestURI()));
        }

        MessageReceiver receiver = (MessageReceiver) getReceivers().get(uri);

        if (receiver == null)
        {
            receiver = (AbstractMessageReceiver) getReceivers().get(uri);
            
            // Lets see if the uri matches up with the last part of
            // any of the receiver keys.
            if (receiver == null)
            {
                receiver = HttpMessageReceiver.findReceiverByStem(connector.getReceivers(), uri);
            }

            if (receiver == null)
            {
                throw new NoReceiverForEndpointException("No receiver found for endpointUri: " + uri);
            }
        }
        InboundEndpoint endpoint = receiver.getEndpoint();
        
        // Ensure that this receiver is using a dynamic (mutable) endpoint
        if (!(endpoint instanceof DynamicURIInboundEndpoint)) 
        {
            endpoint = new DynamicURIInboundEndpoint(receiver.getEndpoint());
            receiver.setEndpoint(endpoint);
        }
        
        // Tell the dynamic endpoint about our new URL
        //Note we don't use the servlet: prefix since we need to be dealing with the raw endpoint here
        EndpointURI epURI = new MuleEndpointURI(getRequestUrl(httpServletRequest));
        
        try
        {
            epURI.initialise();
            epURI.getParams().setProperty("servlet.endpoint", "true");
            ((DynamicURIInboundEndpoint) endpoint).setEndpointURI(epURI);
        }
        catch (InitialisationException e)
        {
            throw new EndpointException(e);
        }
        return receiver;
    }

    protected String getRequestUrl(HttpServletRequest httpServletRequest)
    {
        StringBuffer url = new StringBuffer();

        url.append(httpServletRequest.getScheme());
        url.append("://");
        url.append(httpServletRequest.getServerName());
        url.append(":");
        url.append(httpServletRequest.getServerPort());
        url.append(httpServletRequest.getServletPath());
        url.append(httpServletRequest.getPathInfo());
        if (httpServletRequest.getQueryString() != null)
        {
            url.append("?");
            url.append(httpServletRequest.getQueryString());
        }
        return url.toString();
    }

    protected String getReceiverName(HttpServletRequest httpServletRequest)
    {
        String name = httpServletRequest.getPathInfo();
        if (name == null)
        {
            name = httpServletRequest.getServletPath();
            if (name == null)
            {
                name = httpServletRequest.getParameter("endpoint");
                if (name == null)
                {
                    Properties params = PropertiesUtils.getPropertiesFromQueryString(httpServletRequest.getQueryString());
                    name = params.getProperty("endpoint");
                    if (name == null)
                    {
                        return null;
                    }
                }
            }
        }

        if (name.startsWith("/"))
        {
            name = name.substring(1);
        }
        return name;
    }

    protected Map getReceivers()
    {
        return connector.getReceivers();
    }
}
