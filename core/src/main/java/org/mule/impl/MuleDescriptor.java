/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.config.MuleConfiguration;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.impl.container.DescriptorContainerKeyPair;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.transformer.UMOTransformer;

import java.beans.ExceptionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <code>MuleDescriptor</code> describes all the properties for a Mule UMO. New
 * Mule UMOs can be initialised as needed from their descriptor.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleDescriptor extends ImmutableMuleDescriptor implements UMODescriptor
{
    public static final String DEFAULT_INSTANCE_REF_NAME = "_instanceRef";
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(MuleDescriptor.class);

    public MuleDescriptor(String name)
    {
        super();
        this.name = name;
    }

    public MuleDescriptor(MuleDescriptor descriptor)
    {
        super(descriptor);
    }

    /**
     * Default constructor. Initalises common properties for the MuleConfiguration
     * object
     * 
     * @see MuleConfiguration
     */
    public MuleDescriptor()
    {
        super();
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setExceptionListener(org.mule.umo.UMOExceptionStrategy)
     */
    public void setExceptionListener(ExceptionListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Exception Strategy cannot be null");
        }
        this.exceptionListener = listener;
        logger.debug("Using exception strategy: " + listener.getClass().getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setName(java.lang.String)
     */
    public void setName(String newName)
    {
        if (newName == null)
        {
            throw new IllegalArgumentException("Name cannot be null");
        }
        name = newName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.HasTransformer#setOutboundTransformer(org.mule.umo.transformer.UMOTransformer)
     */
    public void setOutboundTransformer(UMOTransformer transformer)
    {
        outboundTransformer = transformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setResponseTransformer(UMOTransformer)
     */
    public void setResponseTransformer(UMOTransformer transformer)
    {
        responseTransformer = transformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#getPropertiesForURI(java.util.Properties)
     */
    public void setProperties(Map props)
    {
        properties = props;
        String delegate = (String)properties.get(MULE_PROPERTY_DOT_PROPERTIES);
        if (delegate != null)
        {
            try
            {
                FileInputStream is = new FileInputStream(new File(delegate));
                Properties dProps = new Properties();
                dProps.load(is);
                properties.putAll(dProps);
            }
            catch (Exception e)
            {
                logger.warn(MULE_PROPERTY_DOT_PROPERTIES + " was set  to " + delegate
                            + " but the file could not be read, exception is: " + e.getMessage());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setVersion(long)
     */
    public void setVersion(String ver)
    {
        version = ver;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setInboundEndpoint(org.mule.impl.UMOEndpoint)
     */
    public void setInboundEndpoint(UMOEndpoint endpoint) throws MuleException
    {
        inboundEndpoint = endpoint;
        if (inboundEndpoint != null)
        {
            inboundEndpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
            if (inboundEndpoint.getTransformer() != null)
            {
                inboundTransformer = inboundEndpoint.getTransformer();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setOutboundEndpoint(org.mule.impl.UMO
     *      ProviderDescriptor)
     */
    public void setOutboundEndpoint(UMOEndpoint endpoint) throws MuleException
    {
        outboundEndpoint = endpoint;
        if (outboundEndpoint != null)
        {
            outboundEndpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
            if (outboundEndpoint.getTransformer() != null)
            {
                outboundTransformer = outboundEndpoint.getTransformer();
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.HasTransformer#setInboundTransformer(org.mule.umo.transformer.UMOTransformer)
     */
    public void setInboundTransformer(UMOTransformer transformer)
    {
        inboundTransformer = transformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#addinteceptor(org.mule.umo.UMOInterceptor)
     */
    public void addInterceptor(UMOInterceptor inteceptor)
    {
        if (inteceptor != null)
        {
            intecerptorList.add(inteceptor);
        }
    }

    public void setInterceptors(List inteceptorList)
    {
        this.intecerptorList = inteceptorList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setPoolingProfile(UMOPoolingProfile)
     */
    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMODescriptor#setImplementation(java.lang.String)
     */
    public void setImplementation(Object reference)
    {
        if (reference == null)
        {
            throw new IllegalArgumentException("ImplementationReference cannot be null");
        }
        implementationReference = reference;
    }

    public void setImplementationInstance(Object instance)
    {
        if (name == null)
        {
            throw new NullPointerException("UMODescriptor.name");
        }
        properties.put(DEFAULT_INSTANCE_REF_NAME, instance);
        setImplementation(new DescriptorContainerKeyPair(name, DEFAULT_INSTANCE_REF_NAME));
    }

    public void setInboundRouter(UMOInboundMessageRouter routerList)
    {
        this.inboundRouter = routerList;
    }

    public void setOutboundRouter(UMOOutboundMessageRouter routerList)
    {
        outboundRouter = routerList;
    }

    public void setContainerManaged(boolean value)
    {
        containerManaged = value;
    }

    public void addInitialisationCallback(InitialisationCallback callback)
    {
        initialisationCallbacks.add(callback);
    }

    /**
     * Response Routers control how events are returned in a request/response call.
     * It cn be use to aggregate response events before returning, thus acting as a
     * Join in a forked process. This can be used to make request/response calls a
     * lot more efficient as independent tasks can be forked, execute concurrently
     * and then join before the request completes
     * 
     * @param router the response router for this component
     * @see org.mule.umo.routing.UMOResponseMessageRouter
     */
    public void setResponseRouter(UMOResponseMessageRouter router)
    {
        this.responseRouter = router;
    }

    /**
     * Determines if only a single instance of this component is created. This is
     * useful when a component hands off event processing to another engine such as
     * Rules processing or Bpel and the processing engine allocates and manages its
     * own threads.
     * 
     * @param singleton true if this component is a singleton
     */
    public void setSingleton(boolean singleton)
    {
        this.singleton = singleton;
    }

    /**
     * Sets the initial state of this component
     * 
     * @param state the initial state of this component
     */
    public void setInitialState(String state)
    {
        this.initialState = state;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    /**
     * Sets the name of the contaier where the object for this descriptor resides. If
     * this value is 'none' the 'implementaiton' attributed is expected to be a fully
     * qualified class name that will be instanciated.
     * 
     * @param containerName the container name, or null if it is not known - in which
     *            case each container will be queried for the component
     *            implementation.
     */
    public void setContainer(String containerName)
    {
        this.container = containerName;
    }
}
