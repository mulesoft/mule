/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.servlet;

import org.mule.RegistryContext;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpMessageReceiver;
import org.mule.providers.http.i18n.HttpMessages;
import org.mule.providers.service.TransportFactory;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.NoReceiverForEndpointException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.PropertiesUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives Http requests via a Servlet and routes the to listeners with servlet://
 * endpoints
 *
 * There needs to be a ServletConnector configured on the Mule Server, this connector
 * must have the servletUrl property set that matches the Url for the container that this
 * Servlet is hosted in, i.e. something like http://192.168.10.21:8888
 */

public class MuleReceiverServlet extends AbstractReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6631307373079767439L;

    protected ServletConnector connector = null;

    protected void doInit(ServletConfig servletConfig) throws ServletException
    {
        String servletConnectorName = servletConfig.getInitParameter(SERVLET_CONNECTOR_NAME_PROPERTY);
        if(servletConnectorName==null)
        {
            connector = (ServletConnector) TransportFactory.getConnectorByProtocol("servlet");
            if (connector == null)
            {
                throw new ServletException(HttpMessages.noConnectorForProtocolServlet().toString());
            }
        }
        else
        {
            connector = (ServletConnector) RegistryContext.getRegistry().lookupConnector(servletConnectorName);
            if (connector == null)
            {
                throw new ServletException(
                    HttpMessages.noServletConnectorFound(servletConnectorName).toString());
            }
        }


    }

    protected void doHead(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage = null;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "HEAD");
            setupRequestMessage(request, requestMessage);
            receiver.routeMessage(requestMessage, true);
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
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
            writeResponse(response, responseMessage);
        }
        catch (Exception e)
        {
            handleException(e, e.getMessage(), response);
        }
    }

    private void setupRequestMessage(HttpServletRequest request, UMOMessage requestMessage)
    {
        requestMessage.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, request.getRequestURI());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
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

    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "OPTIONS");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
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
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "PUT");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
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
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "DELETE");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
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
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "TRACE");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
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
            UMOMessageReceiver receiver = getReceiverForURI(request);
            UMOMessage responseMessage;
            UMOMessage requestMessage = new MuleMessage(new HttpRequestMessageAdapter(request));
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "CONNECT");
            setupRequestMessage(request, requestMessage);
            responseMessage = receiver.routeMessage(requestMessage, true);
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

    protected UMOMessageReceiver getReceiverForURI(HttpServletRequest httpServletRequest)
        throws EndpointException
    {
        String uri = getReceiverName(httpServletRequest);
        if (uri == null)
        {
            throw new EndpointException(
                HttpMessages.unableToGetEndpointUri(httpServletRequest.getRequestURI()));
        }

        UMOMessageReceiver receiver = (UMOMessageReceiver)getReceivers().get(uri);

        if (receiver == null)
        {
            // Nothing found lets try stripping the path and only use the last
            // path element
            int i = uri.lastIndexOf("/");
            if (i > -1)
            {
                String tempUri = uri.substring(i + 1);
                receiver = (AbstractMessageReceiver)getReceivers().get(tempUri);
            }

            // Lets see if the uri matches up with the last part of
            // any of the receiver keys.
            if (receiver == null) 
            {
                receiver = HttpMessageReceiver.findReceiverByStem(connector.getReceivers(), uri);
            }
            
            // This is some bizarre piece of code so the XFire Servlet code works.
            // We should remove this at some point (see XFireWsdlCallTestCase for a failure
            // if this code is removed).
            if (receiver == null)
            {
                Map receivers = getReceivers();
                Iterator iter = receivers.keySet().iterator();
                while (iter.hasNext())
                {
                    String key = iter.next().toString();
                    i = key.lastIndexOf("/");
                    if (i > -1)
                    {
                        String key2 = key.substring(i+1);
                        if (key2.equals(uri))
                        {
                            receiver = (AbstractMessageReceiver)receivers.get(key);
                            break;
                        }
                    }
                }
            }
            
            if (receiver == null)
            {
                throw new NoReceiverForEndpointException("No receiver found for endpointUri: " + uri);
            }
        }
        //TODO DF: Endpoint mutability
        ((UMOEndpoint) receiver.getEndpoint()).setEndpointURI(new MuleEndpointURI(getRequestUrl(httpServletRequest)));
        return receiver;
    }

    protected String getRequestUrl(HttpServletRequest httpServletRequest)
    {
        StringBuffer url = new StringBuffer();
        url.append(connector.getProtocol().toLowerCase());
        url.append(":");
        url.append(httpServletRequest.getScheme());
        url.append("://");
        url.append(httpServletRequest.getServerName());
        url.append(":");
        url.append(httpServletRequest.getServerPort());
        url.append("/");
        url.append(getReceiverName(httpServletRequest));
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
