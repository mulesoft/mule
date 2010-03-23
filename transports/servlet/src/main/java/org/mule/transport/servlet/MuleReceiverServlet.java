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
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.servlet.i18n.ServletMessages;
import org.mule.util.PropertiesUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives Http requests via a Servlet and routes them to listeners with servlet://
 * endpoints
 * <p/>
 */
public class MuleReceiverServlet extends AbstractReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6631307373079767439L;

    protected ServletConnector connector = null;

    @Override
    protected void doInit() throws ServletException
    {
        connector = getOrCreateServletConnector(getServletConfig().getInitParameter(SERVLET_CONNECTOR_NAME_PROPERTY));
    }

    protected ServletConnector getOrCreateServletConnector(String name) throws ServletException
    {
        ServletConnector servletConnector;
        if (name == null)
        {
            servletConnector = (ServletConnector) new TransportFactory(muleContext).getConnectorByProtocol("servlet");
            if (servletConnector == null)
            {
                servletConnector = new ServletConnector();
                servletConnector.setName("_generatedServletConnector");
                try
                {
                    muleContext.getRegistry().registerConnector(servletConnector);
                }
                catch (MuleException e)
                {
                    throw new ServletException("Failed to register the ServletConnector", e);
                }
            }
        }
        else
        {
            servletConnector = (ServletConnector) muleContext.getRegistry().lookupConnector(name);
            if (servletConnector == null)
            {
                throw new ServletException(ServletMessages.noServletConnectorFound(name).toString());
            }
        }

        return servletConnector;
    }

    protected void setupRequestMessage(HttpServletRequest request,
                                       MuleMessage requestMessage,
                                       MessageReceiver receiver)
    {

        EndpointURI uri = receiver.getEndpointURI();
        String reqUri = request.getRequestURI();
        requestMessage.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, reqUri);

        String queryString = request.getQueryString();
        if (queryString != null)
        {
            reqUri += "?" + queryString;
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
        // template method
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            MessageReceiver receiver = getReceiverForURI(request);

            MuleMessage requestMessage = new DefaultMuleMessage(new HttpRequestMessageAdapter(request), muleContext);
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod());

            setupRequestMessage(request, requestMessage, receiver);

            MuleMessage result = routeMessage(receiver, requestMessage, request);
            writeResponse(response, result);
        }
        catch (Exception e)
        {
            handleException(e, ServletMessages.failedToProcessRequest().getMessage(), response);
        }
    }

    protected MuleMessage routeMessage(MessageReceiver receiver, MuleMessage requestMessage, HttpServletRequest request)
            throws MuleException
    {
        return receiver.routeMessage(requestMessage, true);
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

        MessageReceiver receiver = getReceivers().get(uri);

        // Lets see if the uri matches up with the last part of
        // any of the receiver keys.
        if (receiver == null)
        {
            receiver = HttpMessageReceiver.findReceiverByStem(connector.getReceivers(), uri);
        }

        if (receiver == null)
        {
            receiver = matchReceiverByWildcard(uri, receiver);
        }

        if (receiver == null)
        {
            throw new NoReceiverForEndpointException(uri);
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
        EndpointURI epURI = new MuleEndpointURI(getRequestUrl(httpServletRequest), muleContext);

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

    protected MessageReceiver matchReceiverByWildcard(String uri, MessageReceiver receiver)
    {
        // Now match wild cards
        for (Object key : getReceivers().keySet())
        {
            if (new WildcardFilter(key.toString()).accept(uri))
            {
                receiver = connector.getReceivers().get(key);
                break;
            }
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
        
        String pathInfo = httpServletRequest.getPathInfo();
        if (pathInfo != null)
        {
            url.append(pathInfo);
        }
        
        String queryString = httpServletRequest.getQueryString();
        if (queryString != null)
        {
            url.append("?");
            url.append(queryString);
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

    protected Map<Object, MessageReceiver> getReceivers()
    {
        return connector.getReceivers();
    }
}
