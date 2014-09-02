/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.component.AbstractComponent;
import org.mule.transport.http.HttpConnector;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.WebApplicationFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wraps a set of components which can get invoked by Jersey. This component will
 * map the MuleMessage format to the internal Jersey format. Jersey will then select
 * the appropriate component to invoke based on the request parameters/URI.
 */
public class JerseyResourcesComponent extends AbstractComponent
{
    public static String JERSEY_RESPONSE = "jersey_response";

    protected final Log logger = LogFactory.getLog(this.getClass());

    private List<JavaComponent> components;

    private WebApplication application;

    private List<ExceptionMapper<?>> exceptionMappers = new ArrayList<ExceptionMapper<?>>();

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
        initializeExceptionMappers(resources);

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

    protected void initializeExceptionMappers(final Set<Class<?>> resources)
    {
        // Initialise Exception Mappers
        for (ExceptionMapper<?> exceptionMapper : exceptionMappers)
        {
            resources.add(exceptionMapper.getClass());
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

        String path = (String) message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        String contextPath = (String) message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
        String query = null;
        int queryIdx = path.indexOf('?');
        if (queryIdx != -1)
        {
            query = path.substring(queryIdx + 1);
            path = path.substring(0, queryIdx);
        }

        EndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        String host = message.getInboundProperty("Host", endpointUri.getHost());
        String method = message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);
        InBoundHeaders headers = new InBoundHeaders();
        for (Object prop : message.getInboundPropertyNames())
        {
            Object property = message.getInboundProperty(prop.toString());
            if (property != null)
            {
                headers.add(prop.toString(), property.toString());
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
        final MuleContainerResponse res = new MuleContainerResponse(application, req, writer);

        /* This will process the request in the Jersey application, but only the headers will be written
         * to the response, as we are providing a custom implementation of ContainerResponse. Streaming of the
         * payload will be executed inside the output handler that is returned. */
        application.handleRequest(req, res);

        return new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                res.writeToStream(out);
            }
        };
    }

    protected static InputStream getInputStream(MuleMessage message) throws TransformerException
    {
        return message.getPayload(InputStream.class);
    }

    protected IoCComponentProviderFactory getComponentProvider()
    {
        return new MuleComponentProviderFactory(muleContext, components);
    }

    protected static URI getCompleteUri(EndpointURI endpointUri,
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

    protected static URI getBaseUri(EndpointURI endpointUri, String scheme, String host, String contextPath)
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

    public void setExceptionMapper(ExceptionMapper<?> exceptionMapper)
    {
        exceptionMappers.add(exceptionMapper);
    }
}
