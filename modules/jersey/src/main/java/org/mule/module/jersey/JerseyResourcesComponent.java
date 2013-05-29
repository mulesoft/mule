/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.component.JavaComponent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.component.AbstractComponent;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.WebApplicationFactory;

/**
 * Wraps a set of components which can get invoked by Jersey. This component will
 * map the MuleMessage format to the internal Jersey format. Jersey will then select
 * the appropriate component to invoke based on the request parameters/URI.
 */
public class JerseyResourcesComponent extends AbstractComponent
{
    public static String JERSEY_RESPONSE = "jersey_response";

    private List<JavaComponent> components;

    private WebApplication application;

    private List<ExceptionMapper<?>> exceptionMappers = new ArrayList<ExceptionMapper<?>>();

    private List<ContextResolver<?>> contextResolvers = new ArrayList<ContextResolver<?>>();

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();

        final Set<Class<?>> resources = new HashSet<Class<?>>();

        if (components == null)
        {
            throw new IllegalStateException("There must be at least one component in the Jersey resources.");
        }

        initializeResources(resources);
        initializeOtherResources(exceptionMappers, resources);
        initializeOtherResources(contextResolvers, resources);

        DefaultResourceConfig resourceConfig = createConfiguration(resources);

        application = WebApplicationFactory.createWebApplication();
        application.initiate(resourceConfig, getComponentProvider());
    }

    protected void initializeResources(Set<Class<?>> resources) throws InitialisationException
    {
        // Initialize the Jersey resources using the components
        for (JavaComponent component : components)
        {
            Class<?> c;
            try
            {
                c = component.getObjectType();
                resources.add(c);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    protected void initializeOtherResources(List<?> newResources, final Set<Class<?>> resources)
    {
        for (Object resource : newResources)
        {
            resources.add(resource.getClass());
        }
    }

    protected DefaultResourceConfig createConfiguration(final Set<Class<?>> resources)
    {
        return new DefaultResourceConfig(resources);
    }

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        MuleMessage message = event.getMessage();

        String path = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        String contextPath = message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
        String query = null;
        int queryIdx = path.indexOf('?');
        if (queryIdx != -1)
        {
            query = path.substring(queryIdx + 1);
            path = path.substring(0, queryIdx);
        }

        URI endpointUri = event.getMessageSourceURI();
        String host = message.getInboundProperty("Host", endpointUri.getHost());
        String method = message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);
        InBoundHeaders headers = new InBoundHeaders();
        for (Object prop : message.getInboundPropertyNames())
        {
            if (prop.equals(HttpConnector.HTTP_COOKIES_PROPERTY))
            {
                org.apache.commons.httpclient.Cookie[] apacheCookies = message
                        .getInboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
                for (org.apache.commons.httpclient.Cookie apacheCookie : apacheCookies)
                {
                    Cookie cookie = new Cookie(apacheCookie.getName(), apacheCookie.getValue());
                    headers.addObject(HttpConstants.HEADER_COOKIE, cookie);
                }
            } else
            {
                Object property = message.getInboundProperty(prop.toString());
                if (property != null)
                {
                    headers.add(prop.toString(), property.toString());
                }
            }
        }

        String scheme;
        if ("servlet".equals(endpointUri.getScheme()))
        {
            scheme = "http";
        }
        else
        {
            scheme = endpointUri.getScheme();
        }

        URI baseUri = getBaseUri(endpointUri, scheme, host, contextPath);
        URI completeUri = getCompleteUri(endpointUri, scheme, host, path, query);
        ContainerRequest req = new ContainerRequest(application, method, baseUri, completeUri, headers,
            getInputStream(message));

        if (logger.isDebugEnabled())
        {
            logger.debug("Base URI: " + baseUri);
            logger.debug("Complete URI: " + completeUri);
        }

        MuleResponseWriter writer = new MuleResponseWriter(message);
        ContainerResponse res = new ContainerResponse(application, req, writer);

        application.handleRequest(req, res);

        return writer.getResponse();
    }

    protected static InputStream getInputStream(MuleMessage message) throws TransformerException
    {
        return message.getPayload(InputStream.class);
    }

    protected IoCComponentProviderFactory getComponentProvider()
    {
        return new MuleComponentProviderFactory(muleContext, components);
    }

    protected static URI getCompleteUri(URI endpointUri,
                                        String scheme,
                                        String host,
                                        String path,
                                        String query) throws URISyntaxException
    {
        String uri = scheme + "://" + host + path;
        if (query != null)
        {
            uri += "?" + query;
        }

        return new URI(uri);
    }

    protected static URI getBaseUri(URI endpointUri, String scheme, String host, String contextPath)
        throws URISyntaxException
    {
        if (!contextPath.endsWith("/"))
        {
            contextPath += "/";
        }

        return new URI(scheme + "://" + host + contextPath);
    }

    public List<JavaComponent> getComponents()
    {
        return components;
    }

    public void setComponents(List<JavaComponent> components)
    {
        this.components = components;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        List<JavaComponent> javaComponents = new ArrayList<JavaComponent>();
        for (MessageProcessor mp : messageProcessors)
        {
            if (mp instanceof JavaComponent)
            {
                javaComponents.add((JavaComponent) mp);
            }
            else
            {
                throw new IllegalStateException("Only JavaComponents are allowed as MessageProcessors. Type "
                                                + mp.getClass().getName() + " is not allowed.");
            }
        }
        setComponents(javaComponents);
    }

    public void setExceptionMappers(List<ExceptionMapper<?>> exceptionMappers)
    {
        this.exceptionMappers.addAll(exceptionMappers);
    }

    public void setContextResolvers(List<ContextResolver<?>> contextResolvers)
    {
        this.contextResolvers.addAll(contextResolvers);
    }

}
