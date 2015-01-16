/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.rmi;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractJndiConnector;
import org.mule.transport.rmi.i18n.RmiMessages;
import org.mule.util.ArrayUtils;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.security.Policy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.collections.MapUtils;

/**
 * <code>RmiConnector</code> can bind or send to a given RMI port on a given host.
 */
public class RmiConnector extends AbstractJndiConnector
{

    public static final String RMI = "rmi";
    public static final int DEFAULT_RMI_muleRegistry_PORT = 1099;
    public static final String PROPERTY_RMI_SECURITY_POLICY = "securityPolicy";
    public static final String PROPERTY_RMI_SERVER_CODEBASE = "serverCodebase";
    public static final String PROPERTY_SERVER_CLASS_NAME = "serverClassName";

    /**
     * The property name that explicitly defines which argument types should be
     * passed to a remote object method invocation. This is a comma-separate list for
     * fully qualified classnames. If this property is not set on an outbound
     * endpoint, the argument types will be determined automatically from the payload
     * of the current message
     */
    public static final String PROPERTY_SERVICE_METHOD_PARAM_TYPES = "methodArgumentTypes";

    /**
     * The property name for a list of objects used to call a Remote object via an
     * RMI or EJB MessageReceiver
     */
    public static final String PROPERTY_SERVICE_METHOD_PARAMS_LIST = "methodArgumentsList";

    private String securityPolicy = null;

    private String serverCodebase = null;

    private String serverClassName = null;

    protected long pollingFrequency = 1000L;

    private SecurityManager securityManager = new SecurityManager();
    
    public RmiConnector(MuleContext context)
    {
        super(context);
    }

    /**
     * As RMI requires a security manager, the connector has built int functionality
     * to change the security manager and policy.
     * This method is used to do that change, and may be called from other components
     * that require the correct setup even before the connector is initialized
     */
    public void initSecurity()
    {
        if (securityPolicy != null)
        {
            System.setProperty("java.security.policy", securityPolicy);
            Policy.getPolicy().refresh();
        }
        if (securityManager != null)
        {
            System.setSecurityManager(securityManager);
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        initSecurity();
        initJndiContext();
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method
    }

    public String getProtocol()
    {
        return RMI;
    }

    public String getSecurityPolicy()
    {
        return securityPolicy;
    }

    public void setSecurityPolicy(String path)
    {
        // verify securityPolicy existence
        if (path != null)
        {
            URL url = IOUtils.getResourceAsUrl(path, RmiConnector.class);
            if (url == null)
            {
                throw new IllegalArgumentException(
                    "Error on initialization, RMI security policy does not exist");
            }
            this.securityPolicy = url.toString();
        }
    }

    public String getServerCodebase()
    {
        return (this.serverCodebase);
    }

    public void setServerCodebase(String serverCodebase)
    {
        this.serverCodebase = serverCodebase;
    }

    public String getServerClassName()
    {
        return (this.serverClassName);
    }

    public void setServerClassName(String serverClassName)
    {
        this.serverClassName = serverClassName;
    }

    public SecurityManager getSecurityManager()
    {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager)
    {
        this.securityManager = securityManager;
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        final Object[] args = new Object[]{new Long(pollingFrequency)};
        return getServiceDescriptor().createMessageReceiver(this, flowConstruct, endpoint, args);
    }

    /**
     * Helper method for Dispatchers and Receives to extract the correct method from
     * a Remote object
     *
     * @param remoteObject The remote object on which to invoke the method
     * @param event The current event being processed
     * @throws org.mule.api.MuleException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public Method getMethodObject(Remote remoteObject, MuleEvent event, OutboundEndpoint outboundEndpoint)
        throws MuleException, NoSuchMethodException, ClassNotFoundException
    {
        EndpointURI endpointUri = outboundEndpoint.getEndpointURI();
        String methodName = MapUtils.getString(endpointUri.getParams(), MuleProperties.MULE_METHOD_PROPERTY,
            null);

        if (null == methodName)
        {
            methodName = (String)event.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY, PropertyScope.INVOCATION);

            if (null == methodName)
            {
                throw new MessagingException(RmiMessages.messageParamServiceMethodNotSet(), event);
            }
        }

        Class[] argTypes = getArgTypes(event.getMessage().getInvocationProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES), event);

        try
        {
            return remoteObject.getClass().getMethod(methodName, argTypes);
        }
        catch (NoSuchMethodException e)
        {
            throw new NoSuchMethodException(
                CoreMessages.methodWithParamsNotFoundOnObject(methodName, ArrayUtils.toString(argTypes),
                    remoteObject.getClass()).toString());
        }
        catch (SecurityException e)
        {
            throw e;
        }
    }

    protected Class[] stringsToClasses(Collection strings) throws ClassNotFoundException
    {
        Class[] classes = new Class[strings.size()];
        int index = 0;
        Iterator string = strings.iterator();
        while (string.hasNext())
        {
            classes[index++] = ClassUtils.loadClass((String) string.next(), getClass());
        }
        return classes;
    }

    protected Object getRemoteRef(ImmutableEndpoint endpoint)
        throws IOException, NotBoundException, NamingException, InitialisationException
    {

        EndpointURI endpointUri = endpoint.getEndpointURI();

        String serviceName = endpointUri.getPath();
        try
        {
            // Test if we can find the object locally
            return getJndiContext().lookup(serviceName);
        }
        catch (NamingException e)
        {
            // Strip path seperator
        }

        try
        {
            serviceName = serviceName.substring(1);
            return getJndiContext().lookup(serviceName);
        }
        catch (NamingException e)
        {
            // Try with full host and path
        }

        int port = endpointUri.getPort();
        if (port < 1)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("RMI port not set on URI: " + endpointUri + ". Using default port: "
                            + RmiConnector.DEFAULT_RMI_muleRegistry_PORT);
            }
            port = RmiConnector.DEFAULT_RMI_muleRegistry_PORT;
        }

        InetAddress inetAddress = InetAddress.getByName(endpointUri.getHost());

        return getJndiContext(inetAddress.getHostAddress() + ":" + port).lookup(serviceName);
    }

    public Remote getRemoteObject(ImmutableEndpoint endpoint)
        throws IOException, NotBoundException, NamingException, InitialisationException
    {
        return (Remote)getRemoteRef(endpoint);
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    protected Class[] getArgTypes(Object args, MuleEvent fromEvent) 
        throws ClassNotFoundException, TransformerException
    {
        Class<?>[] argTypes = null;

        if (args instanceof List)
        {
            // MULE-1794 this used to take the first list entry as a string, splitting it
            // as for String below.
            argTypes = stringsToClasses((List) args);
        }
        else if (args instanceof String)
        {
            argTypes = stringsToClasses(Arrays.asList(((String) args).split(",")));
        }
        else
        {
            argTypes = ClassUtils.getClassTypes(fromEvent.getMessage().getPayload());
        }

        return argTypes;
    }
}
