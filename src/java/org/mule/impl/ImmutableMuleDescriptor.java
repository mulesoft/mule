/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.umo.UMOException;
import org.mule.umo.UMOImmutableDescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassHelper;

import java.beans.ExceptionListener;
import java.util.*;

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
     * Implementation type can be prepended to the implementation string to
     * control how the implementation is loaded. Local mean that the
     * implementation is loaded from the object references i.e. local:myRef
     * would get the implementation from the ObjectReference called myRef. Other
     * implementations may include jndi:myRef
     */
    public static final String IMPLEMENTATION_TYPE_LOCAL = "local:";
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
     * The implementationReference used to create the Object UMO instance Can
     * either be a string such as a container reference or classname or can be
     * an instance of the implementation
     */
    protected Object implementationReference = null;

    /**
     * The transformer for the default receive endpoint
     */
    protected UMOTransformer inboundTransformer = null;

    /**
     * The descriptor name
     */
    protected String name;

    /**
     * The transformer for the default send Endpoint
     */
    protected UMOTransformer outboundTransformer = null;

    /**
     * The properties for the Mule UMO. 
     */
    protected Map properties = new HashMap();

    /**
     * The transformer for the response
     */
    protected UMOTransformer responseTransformer = null;

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

    protected UMOEndpoint inboundEndpoint;
    protected UMOEndpoint outboundEndpoint;

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
     * in a container. if the value is false the component will not be pooled by
     * Mule
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
        properties = (HashMap) descriptor.getProperties();
        name = descriptor.getName();

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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("name=" + name);
        buffer.append(", outbound endpoint=" + outboundEndpoint);
        buffer.append(", send transformer=" + outboundTransformer);
        buffer.append(", inbound endpointUri=" + inboundEndpoint);
        buffer.append(", receive transformer=" + inboundTransformer);
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
        return containerManaged;
    }

    public Class getImplementationClass() throws UMOException
    {
        // check for other types of references
        String impl = null;
        Class implClass = null;
        if (implementationReference instanceof String) {
            impl = implementationReference.toString();
            if (impl.startsWith(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL)) {
                impl = impl.substring(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL.length());
                Object obj = properties.get(impl);
                if (obj != null) {
                    implClass = obj.getClass();
                    // If its a string it must be a container reference or a
                    // classname
                    if (implClass.equals(String.class)) {
                        implClass = getImplementationForReference(impl);
                    }
                } else {
                    throw new MuleException(new Message(Messages.NO_COMPONENT_FOR_LOCAL_REFERENCE, impl));
                }
            } else {
                implClass = getImplementationForReference(impl);
            }
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
        Class clazz = null;
        try {
            clazz = ClassHelper.loadClass(reference, getClass());
        } catch (Exception e) {
            Object object = MuleManager.getInstance().getContainerContext().getComponent(reference);
            clazz = object.getClass();
        }
        return clazz;
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
}
