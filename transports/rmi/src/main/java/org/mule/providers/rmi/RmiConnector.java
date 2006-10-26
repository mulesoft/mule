/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.collections.MapUtils;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractJndiConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.ArrayUtils;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;

/**
 * <code>RmiConnector</code> can bind or send to a given RMI port on a given host.
 */
public class RmiConnector extends AbstractJndiConnector
{
    // Messages
    public static final int MSG_PARAM_SERVICE_METHOD_NOT_SET = 1;
    public static final int MSG_PROPERTY_SERVICE_METHOD_PARAM_TYPES_NOT_SET = 2;
    public static final int NO_RMI_SERVICECLASS_SET = 10;
    public static final int RMI_SERVICECLASS_INVOCATION_FAILED = 11;

    public static final int DEFAULT_RMI_REGISTRY_PORT = 1099;

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

    private SecurityManager securityManager = new RMISecurityManager();

    public String getProtocol()
    {
        return "rmi";
    }

    /**
     * @return Returns the securityPolicy.
     */
    public String getSecurityPolicy()
    {
        return securityPolicy;
    }

    /**
     * @param path The securityPolicy to set.
     */
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

    /**
     * Method getServerCodebase
     *
     * @return
     */
    public String getServerCodebase()
    {
        return (this.serverCodebase);
    }

    /**
     * Method setServerCodebase
     *
     * @param serverCodebase
     */
    public void setServerCodebase(String serverCodebase)
    {
        this.serverCodebase = serverCodebase;
    }

    /**
     * Method getServerClassName
     *
     * @return
     */
    public String getServerClassName()
    {
        return (this.serverClassName);
    }

    /**
     * Method setServerClassName
     *
     * @param serverClassName
     */
    public void setServerClassName(String serverClassName)
    {
        this.serverClassName = serverClassName;
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();

        if (securityPolicy != null)
        {
            System.setProperty("java.security.policy", securityPolicy);
        }

        // Set security manager
        if (securityManager != null)
        {
            System.setSecurityManager(securityManager);
        }
        initJndiContext();
    }

    public SecurityManager getSecurityManager()
    {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager)
    {
        this.securityManager = securityManager;
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        final Object[] args = new Object[]{new Long(pollingFrequency)};
        return getServiceDescriptor().createMessageReceiver(this, component, endpoint, args);
    }

    /**
     * Helper method for Dispatchers and Receives to extract the correct method from
     * a Remote object
     *
     * @param remoteObject The remote object on which to invoke the method
     * @param event The current event being processed
     * @return
     * @throws org.mule.umo.UMOException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public Method getMethodObject(Remote remoteObject, UMOEvent event)
        throws UMOException, NoSuchMethodException, ClassNotFoundException
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();

        String methodName = MapUtils.getString(endpointUri.getParams(), MuleProperties.MULE_METHOD_PROPERTY,
            null);

        if (null == methodName)
        {
            methodName = (String)event.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY);

            if (null == methodName)
            {
                throw new DispatchException(new org.mule.config.i18n.Message("rmi",
                    RmiConnector.MSG_PARAM_SERVICE_METHOD_NOT_SET), event.getMessage(), event.getEndpoint());
            }
        }

        Class[] argTypes;

        // Parse method args

        Object args = event.getMessage().getProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES);

        String argumentString = null;

        if (args instanceof List)
        {
            List arguments = (List) args;
            argumentString = (String) arguments.get(0);
        }
        else if(args instanceof String)
        {
            argumentString = (String)args;
        }

        if (null != argumentString)
        {
            String[] split = argumentString.split(",");

            argTypes = new Class[split.length];
            for (int i = 0; i < split.length; i++)
            {
                argTypes[i] = ClassUtils.loadClass(split[i].trim(), getClass());

            }
        }
        else
        {
            argTypes = ClassUtils.getClassTypes(event.getTransformedMessage());
        }

        try
        {
            return remoteObject.getClass().getMethod(methodName, argTypes);
        }
        catch (NoSuchMethodException e)
        {
            throw new NoSuchMethodException(new Message(Messages.METHOD_X_WITH_PARAMS_X_NOT_FOUND_ON_X,
                methodName, ArrayUtils.toString(argTypes), remoteObject.getClass().getName()).toString());
        }
        catch (SecurityException e)
        {
            throw e;
        }
    }

    protected Object getRemoteRef(UMOImmutableEndpoint endpoint)
        throws IOException, NotBoundException, NamingException, InitialisationException
    {

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();

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
                            + RmiConnector.DEFAULT_RMI_REGISTRY_PORT);
            }
            port = RmiConnector.DEFAULT_RMI_REGISTRY_PORT;
        }

        InetAddress inetAddress = InetAddress.getByName(endpointUri.getHost());

        return getJndiContext(inetAddress.getHostAddress() + ":" + port).lookup(serviceName);
    }

    public Remote getRemoteObject(UMOImmutableEndpoint endpoint)
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

}
