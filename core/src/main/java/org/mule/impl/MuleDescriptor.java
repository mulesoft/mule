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

import org.mule.config.MuleConfiguration;
import org.mule.config.ThreadingProfile;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.beans.ExceptionListener;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleDescriptor</code> describes all the properties for a Mule UMO. New
 * Mule UMOs can be initialised as needed from their descriptor.
 *
 */

public class MuleDescriptor extends ImmutableMuleDescriptor implements UMODescriptor, ManagementContextAware
{
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
                FileInputStream is = new FileInputStream(FileUtils.newFile(delegate));
                Properties dProps = new Properties();
                dProps.load(is);
                properties.putAll(dProps);
            }
            catch (Exception e)
            {
                // TODO MULE-863: Sufficient?  Correct level?
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
        setImplementationInstance(instance);
    }

    public void setInboundRouter(UMOInboundRouterCollection router)
    {
        this.inboundRouter = router;
    }

    public void setOutboundRouter(UMOOutboundRouterCollection router)
    {
        outboundRouter = router;
    }

    public void setNestedRouter(UMONestedRouterCollection router)
    {
    	nestedRouter = router;
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
     * @see org.mule.umo.routing.UMOResponseRouterCollection
     */
    public void setResponseRouter(UMOResponseRouterCollection router)
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        super.register();
    }


    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }


    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }


    //@java.lang.Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if(StringUtils.isNotEmpty(modelName))
        {
            UMOModel model = managementContext.getRegistry().lookupModel(modelName);
            if(model!=null)
            {
                try
                {
                    model.registerComponent(this);
                }
                catch (UMOException e)
                {
                    throw new InitialisationException(e, this);
                }
            }
        }
    }
}
