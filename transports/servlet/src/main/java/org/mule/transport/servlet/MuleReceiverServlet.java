/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.DynamicURIInboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
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

    private boolean useCachedHttpServletRequest = false;

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
                servletConnector = new ServletConnector(muleContext);
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
        this.useCachedHttpServletRequest = servletConnector.isUseCachedHttpServletRequest();
        return servletConnector;
    }

    protected void setupRequestMessage(HttpServletRequest request,
                                       MuleMessage requestMessage,
                                       MessageReceiver receiver)
    {

        EndpointURI uri = receiver.getEndpointURI();
        String reqUri = request.getRequestURI();
        requestMessage.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, reqUri, PropertyScope.INBOUND);

        String queryString = request.getQueryString();
        if (queryString != null)
        {
            reqUri += "?" + queryString;
        }

        requestMessage.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, reqUri, PropertyScope.INBOUND);

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

        requestMessage.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, path, PropertyScope.INBOUND);
        requestMessage.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY, receiver.getEndpointURI().getAddress(), PropertyScope.INBOUND);

        // Call this to keep API compatability
        setupRequestMessage(request, requestMessage);
    }


    protected void setupRequestMessage(HttpServletRequest request, MuleMessage requestMessage)
    {
        // template method
    }


    // The service() method cannot be overriden blindly as explained below.
    //
    // Until we use a version of the servlet spec that supports the PATCH method, we
    // have to override service() nevertheless to avoid super's implementation
    // barfing on a PATCH request.
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String method = request.getMethod();
        if (method.equalsIgnoreCase(HttpConstants.METHOD_PATCH))
        {
            doAllMethods(request, response);
        }
        else
        {
            super.service(request, response);
        }
    }

    // We cannot override the service method and maintain MuleRESTServletReceiver
    // functionality. See MULE-4806.

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }
    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        doAllMethods(req, resp);
    }

    protected void doAllMethods(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            if (this.useCachedHttpServletRequest)
            {
                request = new CachedHttpServletRequest(request);
            }
            MessageReceiver receiver = getReceiverForURI(request);

            processHttpRequest(request, response, receiver);
        }
        catch (Exception e)
        {
            handleException(e, ServletMessages.failedToProcessRequest().getMessage(), response);
        }
    }

    protected void processHttpRequest(HttpServletRequest request, HttpServletResponse response, MessageReceiver receiver) throws Exception
    {
        MuleMessage requestMessage = receiver.createMuleMessage(request);
        requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod(), PropertyScope.INBOUND);

        setupRequestMessage(request, requestMessage, receiver);

        MuleEvent event = routeMessage(receiver, requestMessage, request);
        MuleMessage result = event == null ? null : event.getMessage();
        writeResponse(response, result);
    }

    protected MuleEvent routeMessage(MessageReceiver receiver, MuleMessage requestMessage, HttpServletRequest request)
            throws MuleException
    {
        return receiver.routeMessage(requestMessage);
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
            receiver = HttpConnector.findReceiverByStem(connector.getReceivers(), uri);
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
        StringBuilder url = new StringBuilder();

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

    @Override
    protected void handleException(Throwable t, String message, HttpServletResponse response)
    {
        MuleEvent responseEvent = null;
        if (t instanceof MessagingException)
        {
            MuleEvent event = ((MessagingException) t).getEvent();
            responseEvent = event.getFlowConstruct().getExceptionListener().handleException((Exception) t, event);
            if (responseEvent != null && responseEvent.getMessage().getExceptionPayload() != null
                && responseEvent.getMessage().getExceptionPayload().getException() instanceof MessagingException)
            {
                message = responseEvent.getMessage().getExceptionPayload().getException().getMessage();
            }
            if (responseEvent != null && responseEvent.getMessage().getExceptionPayload() == null)
            {
                try
                {
                    writeResponse(response, responseEvent.getMessage());
                    return;
                }
                catch (Exception e)
                {
                    logger.error("Failed to write on response: " + e.getMessage(), e);
                }
            }

        }
        else if (t instanceof Exception)
        {
            muleContext.getExceptionListener().handleException((Exception) t);
        }

        super.handleException(t, message, response);
    }
}
