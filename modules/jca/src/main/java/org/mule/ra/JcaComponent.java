/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.ManagementContextAware;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.model.UMOEntryPoint;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>JcaComponent</code> Is the type of component used in Mule when embedded
 * inside an app server using JCA. In the future we might want to use one of the
 * existing models.
 */
public class JcaComponent implements UMOComponent, ManagementContextAware
{
	/**
     * Serial version
     */
    private static final long serialVersionUID = -1510441245219710451L;

    private transient final MuleDescriptor descriptor;
    private transient UMOEntryPoint entryPoint;
    private Object component;
    private ComponentStatistics stats;

    /**
     * Determines if the component has been initialised
     */
    private final AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * Determines if the component has been started
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    protected String registryId;

    protected UMOManagementContext managementContext;



    public JcaComponent(MuleDescriptor descriptor)
    {
        if (descriptor == null)
        {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }

        this.descriptor = descriptor;
    }

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public UMODescriptor getDescriptor()
    {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        try
        {
            // Invoke method
            entryPoint.invoke(component, RequestContext.getEventContext());
        }
        catch (Exception e)
        {
            throw new MuleException(new Message(Messages.FAILED_TO_INVOKE_X, "UMO Component: "
                                                                             + descriptor.getName()), e);
        }
    }

    /**
     * This is the synchronous call method and not supported by components managed in
     * a JCA container
     * 
     * @param event
     * @return
     * @throws UMOException
     */
    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        throw new UnsupportedOperationException("sendEvent()");
    }

    public void pause() throws UMOException
    {
        // nothing to do
    }

    public void resume() throws UMOException
    {
        // nothing to do
    }

    public boolean isPaused()
    {
        return false;
    }

    public void start() throws UMOException
    {
        started.set(true);
    }

    public void stop() throws UMOException
    {
        started.set(false);
    }

    public void dispose()
    {
        descriptor.getManagementContext().getStatistics().remove(stats);
    }

    public synchronized void initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALISED,
                "Component '" + descriptor.getName() + "'"), this);
        }
        descriptor.initialise();
        try
        {
            entryPoint = managementContext.getRegistry().lookupModel(JcaModel.JCA_MODEL_TYPE).getEntryPointResolver().resolveEntryPoint(
                descriptor);
        }
        catch (UMOException e)
        {
            throw new InitialisationException(e, this);
        }

        // initialise statistics
        stats = new ComponentStatistics(descriptor.getName(), -1);

        stats.setEnabled(managementContext.getStatistics().isEnabled());
        managementContext.getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());

        component = descriptor.getImplementation();

        initialised.set(true);
        managementContext.fireNotification(
            new ComponentNotification(descriptor, ComponentNotification.COMPONENT_INITIALISED));
    }

    protected Object getDelegateComponent() throws InitialisationException
    {
        Object impl = descriptor.getImplementation();
        Object component;

        try
        {
            if (impl instanceof ContainerKeyPair)
            {
                component = RegistryContext.getRegistry().getContainerContext().getComponent(impl);

                if (descriptor.isSingleton())
                {
                    descriptor.setImplementation(component);
                }
            }
            else
            {
                component = impl;
            }
        }
        catch (ObjectNotFoundException e)
        {
            throw new InitialisationException(e, this);
        }

        // Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);
        return component;
    }

    public boolean isStarted()
    {
        return started.get();
    }

    /**
     * Gets the underlying instance form this component Where the Component
     * implmentation provides pooling this is no 1-2-1 mapping between UMOComponent
     * and instance, so this method will return the object in initial state. <p/> If
     * the underlying component is Container managed in Spring or another IoC
     * container then the object instance in the IoC container will be returned
     * 
     * @return the underlying instance form this component
     */
    public Object getInstance() throws UMOException
    {
        return component;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        //TODO LM: registryId = RegistryContext.getRegistry().registerMuleObject(managementContext.getModel(), this).getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#deregister()
     */
    public void deregister() throws DeregistrationException
    {
        RegistryContext.getRegistry().deregisterComponent(registryId);
        registryId = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#getRegistryId()
     */
    public String getRegistryId()
    {
        return registryId;
    }

}
