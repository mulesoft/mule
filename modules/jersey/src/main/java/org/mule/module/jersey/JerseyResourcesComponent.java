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
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.component.AbstractComponent;
import org.mule.module.jersey.exception.FallbackErrorMapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.hk2.utilities.Binder;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.jackson1.Jackson1Feature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.ServerRuntime;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;

/**
 * Wraps a set of components which can get invoked by Jersey. This component will
 * map the MuleMessage format to the internal Jersey format. Jersey will then select
 * the appropriate component to invoke based on the request parameters/URI.
 */
public class JerseyResourcesComponent extends AbstractComponent
{

    protected static final String JERSEY_RESPONSE = "jersey_response";

    /**
     * Default dummy security context.
     */
    private static final SecurityContext DEFAULT_SECURITY_CONTEXT = new SecurityContext()
    {

        @Override
        public boolean isUserInRole(final String role)
        {
            return false;
        }

        @Override
        public boolean isSecure()
        {
            return false;
        }

        @Override
        public Principal getUserPrincipal()
        {
            return null;
        }

        @Override
        public String getAuthenticationScheme()
        {
            return null;
        }
    };

    private List<JavaComponent> components;
    private ScheduledExecutorService backgroundScheduler;
    private List<ExceptionMapper<?>> exceptionMappers = new ArrayList<>();
    private List<ContextResolver<?>> contextResolvers = new ArrayList<>();
    private Set<String> packages = new HashSet<>();
    private Map<String, Object> properties = new HashMap<>();

    private ApplicationHandler application;
    private ResourceConfig resourceConfig;

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();

        final Set<Class<?>> resources = new HashSet<>();

        if (components == null)
        {
            throw new IllegalStateException("There must be at least one component in the Jersey resources.");
        }

        initializeResources(resources);
        initializeOtherResources(exceptionMappers, resources);
        initializeOtherResources(contextResolvers, resources);

        application = createApplication(resources);
        try
        {
            ServerRuntime serverRuntime = ClassUtils.getFieldValue(application, "runtime", false);
            backgroundScheduler = ClassUtils.getFieldValue(serverRuntime, "backgroundScheduler", false);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
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

    protected ApplicationHandler createApplication(final Set<Class<?>> resources)
    {
        if (!properties.containsKey(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED))
        {
            properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        }

        resourceConfig = new ResourceConfig();

        for (String pkg : packages)
        {
            resourceConfig.packages(pkg);
        }

        resourceConfig.addProperties(properties)
                .registerClasses(resources)
                .register(Jackson1Feature.class);

        if (!resourceConfig.isRegistered(ResponseErrorMapper.class))
        {
            resourceConfig.register(new FallbackErrorMapper());
        }

        return new ApplicationHandler(resourceConfig, getComponentProvider());
    }

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        ContainerRequest req = buildRequest(event);
        req.setSecurityContext(DEFAULT_SECURITY_CONTEXT);

        MuleMessage message = event.getMessage();
        req.setEntityStream(getInputStream(message));
        copyProperties(message, req);

        return execute(req, event);
    }

    private Object execute(final ContainerRequest request, MuleEvent event)
    {
        final MuleResponseWriter writer = new MuleResponseWriter(event, request.getMethod(), backgroundScheduler);

        request.setWriter(writer);
        application.handle(request);

        return new OutputHandler()
        {
            @Override
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                writer.getOutputStream().setDelegate(out);
            }
        };
    }

    protected void copyProperties(MuleMessage message, ContainerRequest request)
    {
        for (Object prop : message.getInboundPropertyNames())
        {
            if (prop.equals(HttpConnector.HTTP_COOKIES_PROPERTY))
            {
                org.apache.commons.httpclient.Cookie[] apacheCookies = message.getInboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
                for (org.apache.commons.httpclient.Cookie apacheCookie : apacheCookies)
                {
                    Cookie cookie = new Cookie(apacheCookie.getName(), apacheCookie.getValue());
                    request.header(HttpConstants.HEADER_COOKIE, cookie);
                }
            }
            else
            {
                Object property = message.getInboundProperty(prop.toString());
                if (property != null)
                {
                    request.header(prop.toString(), property.toString());
                }
            }
        }
    }

    private InputStream getInputStream(MuleMessage message) throws TransformerException
    {
        return message.getPayload(InputStream.class);
    }

    protected Binder getComponentProvider()
    {
        return new MuleComponentBinder(muleContext, components);
    }

    private ContainerRequest buildRequest(MuleEvent event) throws URISyntaxException
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

        String scheme;
        if ("servlet".equals(endpointUri.getScheme()))
        {
            scheme = "http";
        }
        else
        {
            scheme = endpointUri.getScheme();
        }

        URI baseUri = getBaseUri(scheme, host, contextPath);
        URI completeUri = getCompleteUri(endpointUri, scheme, host, path, query);

        if (logger.isDebugEnabled())
        {
            logger.debug("Base URI: " + baseUri);
            logger.debug("Complete URI: " + completeUri);
        }

        return new ContainerRequest(baseUri, completeUri, method, null, new MapPropertiesDelegate());
    }

    private URI getCompleteUri(URI endpointUri,
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

    private URI getBaseUri(String scheme, String host, String contextPath) throws URISyntaxException
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
        List<JavaComponent> javaComponents = new ArrayList<>();
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

    protected ResourceConfig getResourceConfig()
    {
        return resourceConfig;
    }

    public void setExceptionMappers(List<ExceptionMapper<?>> exceptionMappers)
    {
        this.exceptionMappers.addAll(exceptionMappers);
    }

    public void setContextResolvers(List<ContextResolver<?>> contextResolvers)
    {
        this.contextResolvers.addAll(contextResolvers);
    }

    public void setPackages(Set<String> packages)
    {
        this.packages = packages;
    }

    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }
}
