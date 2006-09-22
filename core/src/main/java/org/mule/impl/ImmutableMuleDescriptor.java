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

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.impl.container.DescriptorContainerContext;
import org.mule.impl.container.DescriptorContainerKeyPair;
import org.mule.impl.container.MuleContainerContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.umo.UMOException;
import org.mule.umo.UMOImmutableDescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.transformer.UMOTransformer;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

/**
 * <code>MuleDescriptor</code> describes all the properties for a Mule UMO.
 * New Mule UMOs can be initialised as needed from their descriptor.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ImmutableMuleDescriptor implements UMOImmutableDescriptor
{
    /**
     * The initial states that the component can be started in
     */
    public static final String INITIAL_STATE_STOPPED = "stopped";
    public static final String INITIAL_STATE_STARTED = "started";
    public static final String INITIAL_STATE_PAUSED = "paused";
    /**
     * Property that allows for a property file to be used to load properties
     * instead of listing them directly in the mule-configuration file
     */
    protected static final String MULE_PROPERTY_DOT_PROPERTIES = "org.mule.dotProperties";

    /**
     * holds the exception stategy for this UMO
     */
    protected ExceptionListener exceptionListener = null;

    /**
     * The implementationReference used to create the Object UMO instance.  Can
     * either be a string such as a container reference or classname or can be
     * an instance of the implementation.
     */
    protected Object implementationReference = null;

    /**
     * The descriptor name
     */
    protected String name;

    /**
     * The properties for the Mule UMO.
     */
    protected Map properties = new HashMap();

    /**
     * The descriptors version
     */
    protected String version = "1.0";

    /**
     * A list of UMOinteceptors that will be executed when the Mule UMO executed
     */
    protected List intecerptorList = new CopyOnWriteArrayList();

    protected UMOInboundMessageRouter inboundRouter = null;

    protected UMOOutboundMessageRouter outboundRouter = null;

    protected UMOResponseMessageRouter responseRouter = null;

    /**
     * The default receive endpoint.
     * @deprecated Please use <code>inboundRouter</code> instead.
     * @see MULE-506
     */
    protected UMOEndpoint inboundEndpoint;

    /**
     * The transformer for the default receive endpoint.
     * @deprecated Please use <code>inboundRouter</code> instead.
     * @see MULE-506
     */
    protected UMOTransformer inboundTransformer = null;

    /**
     * The default send endpoint.
     * @deprecated Please use <code>outboundRouter</code> instead.
     * @see MULE-506
     */
    protected UMOEndpoint outboundEndpoint;

    /**
     * The transformer for the default send Endpoint
     * @deprecated Please use <code>outboundRouter</code> instead.
     * @see MULE-506
     */
    protected UMOTransformer outboundTransformer = null;

    /**
     * The transformer for the response
     * @deprecated Please use <code>responseRouter</code> instead.
     * @see MULE-506
     */
    protected UMOTransformer responseTransformer = null;

    /**
     * The threading profile to use for this component. If this is not set a
     * default will be provided by the server
     */
    protected ThreadingProfile threadingProfile;

    /**
     * the pooling configuration used when initialising the component described
     * by this descriptor.
     */
    protected PoolingProfile poolingProfile;

    /**
     * The queuing profile for events received for this component
     */
    protected QueueProfile queueProfile;

    /**
     * Determines whether the component described by this descriptor is hosted
     * in a container.  If the value is false the component will not be pooled by
     * Mule.
     * @deprecated Use <code>container</code> instead.
     * @see MULE-812
     */
    protected boolean containerManaged = true;

    /**
     * Determines the initial state of this component when the model
     * starts. Can be 'stopped' or 'started' (default)
     */
    protected String initialState = INITIAL_STATE_STARTED;

    /**
     * Determines if this component is a singleton
     */
    protected boolean singleton = false;

    protected List initialisationCallbacks = new ArrayList();

    protected String encoding = null;

    /**
     * The name of the container that the component implementation resides in
     * If null, the container is not known, if 'none' the component is instanciated from
     * its implementation class name.
     */
    protected String container = null;

    /**
     * Default constructor. Initalises common properties for the
     * MuleConfiguration object
     *
     * @see org.mule.config.MuleConfiguration
     */
    public ImmutableMuleDescriptor(ImmutableMuleDescriptor descriptor)
    {
        inboundRouter = descriptor.getInboundRouter();
        outboundRouter = descriptor.getOutboundRouter();
        responseRouter = descriptor.getResponseRouter();
        inboundTransformer = descriptor.getInboundTransformer();
        outboundTransformer = descriptor.getOutboundTransformer();
        responseTransformer = descriptor.getResponseTransformer();
        implementationReference = descriptor.getImplementation();
        version = descriptor.getVersion();
        intecerptorList = descriptor.getInterceptors();
        properties = descriptor.getProperties();
        name = descriptor.getName();
        encoding = descriptor.getEncoding();

        threadingProfile = descriptor.getThreadingProfile();
        poolingProfile = descriptor.getPoolingProfile();
        queueProfile = descriptor.getQueueProfile();
        exceptionListener = descriptor.getExceptionListener();
        initialState = descriptor.getInitialState();
        singleton = descriptor.isSingleton();
        containerManaged = descriptor.isContainerManaged();
    }

    /**
     * Default constructor used by mutable versions of this class to provide
     * defaults for certain properties
     */
    protected ImmutableMuleDescriptor()
    {
        inboundRouter = new InboundMessageRouter();
        inboundRouter.addRouter(new InboundPassThroughRouter());
    }

     public void initialise() throws InitialisationException
    {
        MuleConfiguration config = MuleManager.getConfiguration();
        if (threadingProfile == null) {
            threadingProfile = config.getComponentThreadingProfile();
        }
        if (poolingProfile == null) {
            poolingProfile = config.getPoolingProfile();
        }
        if (queueProfile == null) {
            queueProfile = config.getQueueProfile();
        }

        if (exceptionListener == null) {
            exceptionListener = MuleManager.getInstance().getModel().getExceptionListener();
        } else if (exceptionListener instanceof Initialisable) {
            ((Initialisable) exceptionListener).initialise();
        }

        if (inboundEndpoint != null) {
            if (inboundTransformer != null) {
                inboundEndpoint.setTransformer(inboundTransformer);
            }
            ((MuleEndpoint) inboundEndpoint).initialise();
            // If the transformer was set on the endpoint uri, it will only
            // be initialised when the endpoint is initialised, hence we make
            // this call here to ensure a consistent state
            if (inboundTransformer == null) {
                inboundTransformer = inboundEndpoint.getTransformer();
            }
        }

        if (outboundEndpoint != null) {
            if (outboundTransformer != null) {
                outboundEndpoint.setTransformer(outboundTransformer);
            }
            ((MuleEndpoint) outboundEndpoint).initialise();
            // If the transformer was set on the endpoint uri, it will only
            // be initialised when the endpoint is initialised, hence we make
            // this call here to ensure a consistent state
            if (outboundTransformer == null) {
                outboundTransformer = outboundEndpoint.getTransformer();
            }
        }

        if (exceptionListener instanceof Initialisable) {
            ((Initialisable) exceptionListener).initialise();
        }

        MuleEndpoint endpoint;
        if (inboundRouter == null) {
            // Create Default routes that route to the default inbound and
            // outbound endpoints
            inboundRouter = new InboundMessageRouter();
            inboundRouter.addRouter(new InboundPassThroughRouter());
        } else {
            if (inboundRouter.getCatchAllStrategy() != null
                    && inboundRouter.getCatchAllStrategy().getEndpoint() != null) {
                ((MuleEndpoint) inboundRouter.getCatchAllStrategy().getEndpoint()).initialise();
            }
            for (Iterator iterator = inboundRouter.getEndpoints().iterator(); iterator.hasNext();) {
                endpoint = (MuleEndpoint) iterator.next();
                endpoint.initialise();
            }
        }

        if (responseRouter != null) {
            for (Iterator iterator = responseRouter.getEndpoints().iterator(); iterator.hasNext();) {
                endpoint = (MuleEndpoint) iterator.next();
                endpoint.initialise();
            }
        }

        if (outboundRouter == null) {
            outboundRouter = new OutboundMessageRouter();
            outboundRouter.addRouter(new OutboundPassThroughRouter(this));
        } else {
            if (outboundRouter.getCatchAllStrategy() != null
                    && outboundRouter.getCatchAllStrategy().getEndpoint() != null) {
                ((MuleEndpoint) outboundRouter.getCatchAllStrategy().getEndpoint()).initialise();
            }
            UMOOutboundRouter router = null;
            for (Iterator iterator = outboundRouter.getRouters().iterator(); iterator.hasNext();) {
                router = (UMOOutboundRouter) iterator.next();
                for (Iterator iterator1 = router.getEndpoints().iterator(); iterator1.hasNext();) {
                    endpoint = (MuleEndpoint) iterator1.next();
                    endpoint.initialise();
                }
            }
        }
        // Is a reference of an implementation object?
        if(implementationReference instanceof String) {
            if (DescriptorContainerContext.DESCRIPTOR_CONTAINER_NAME.equals(container)) {
                implementationReference = new DescriptorContainerKeyPair(name, implementationReference);
            } else {
                implementationReference = new ContainerKeyPair(container, implementationReference);
            }
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMODescriptor#getExceptionListener()
     */
    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.transformers.HasTransformer#getInboundTransformer()
     */
    public UMOTransformer getInboundTransformer()
    {
        return inboundTransformer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMODescriptor#getName()
     */
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.impl.MuleDescriptor#getOutboundTransformer()
     */
    public UMOTransformer getOutboundTransformer()
    {
        return outboundTransformer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.impl.MuleDescriptor#getResponseTransformer()
     */
    public UMOTransformer getResponseTransformer()
    {
        return responseTransformer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMODescriptor#getParams() Not HashMap is used instead
     *      of Map due to a Spring quirk where the property is not found if
     *      specified as a map
     */
    public Map getProperties()
    {
        return properties;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMODescriptor#getVersion()
     */
    public String getVersion()
    {
        return version;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMODescriptor#getinteceptorList()
     */
    public List getInterceptors()
    {
        return intecerptorList;
    }

    public String getEncoding()
    {
        return encoding;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("name=").append(name);
        buffer.append(", outbound endpoint=").append(outboundEndpoint);
        buffer.append(", send transformer=").append(outboundTransformer);
        buffer.append(", inbound endpointUri=").append(inboundEndpoint);
        buffer.append(", receive transformer=").append(inboundTransformer);
        buffer.append(", encoding=").append(encoding);
        return buffer.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMODescriptor#getImplementation()
     */
    public Object getImplementation()
    {
        return implementationReference;
    }

    public UMOInboundMessageRouter getInboundRouter()
    {
        return inboundRouter;
    }

    public UMOOutboundMessageRouter getOutboundRouter()
    {
        return outboundRouter;
    }

    /**
     * The threading profile used but the UMO when managing a component. can be
     * used to allocate more or less resources to this particular umo component
     */
    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }

    public PoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public boolean isContainerManaged()
    {
        return !MuleContainerContext.MULE_CONTAINER_NAME.equalsIgnoreCase(container);
    }

    public Class getImplementationClass() throws UMOException
    {
        // check for other types of references
        Class implClass = null;
        if (implementationReference instanceof String || implementationReference instanceof ContainerKeyPair) {
            Object object = MuleManager.getInstance().getContainerContext().getComponent(implementationReference);
            implClass = object.getClass();
        } else {
            implClass = implementationReference.getClass();
        }

        return implClass;
    }

    /**
     * A helper method that will resolved a component for a given reference id.
     * For example, for a component declared in a Spring Application context the
     * id would be the bean id, in Pico the id would be a fully qualified class
     * name.
     *
     * @param reference the reference to use when resolving the component
     * @return the Implementation of the component
     */
    protected Class getImplementationForReference(String reference) throws ContainerException
    {
            Object object = MuleManager.getInstance().getContainerContext().getComponent(reference);
        return object.getClass();
        }

    public void fireInitialisationCallbacks(Object component) throws InitialisationException
    {
        InitialisationCallback callback;
        for (Iterator iterator = initialisationCallbacks.iterator(); iterator.hasNext();) {
            callback = (InitialisationCallback) iterator.next();
            callback.initialise(component);
        }
    }

    /**
     * The inbound Provider to use when receiveing an event. This may get
     * overidden by the configured behaviour of the inbound router on this
     * component
     *
     * @return the inbound endpoint or null if one is not set
     * @see org.mule.umo.endpoint.UMOEndpoint
     */
    public UMOEndpoint getInboundEndpoint()
    {
        return inboundEndpoint;
    }

    /**
     * The outbound Provider to use when sending an event. This may get
     * overidden by the configured behaviour of the outbound router on this
     * component
     *
     * @return the outbound endpoint or null if one is not set
     * @see org.mule.umo.endpoint.UMOEndpoint
     */
    public UMOEndpoint getOutboundEndpoint()
    {
        return outboundEndpoint;
    }

    public UMOResponseMessageRouter getResponseRouter()
    {
        return responseRouter;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public String getInitialState() {
        return initialState;
    }

    /**
     * Returns the name of the contaier where the object for this descriptor resides. If this value
     * is 'none' the 'implementaiton' attributed is expected to be a fully qualified class name that
     * will be instanciated.
     *
     * @return the container name, or null if it is not known - in which case each container will be queried
     *         for the component implementation.
     */
    public String getContainer() {
        return container;
}
}
