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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;

/**
 * <code>JcaComponent</code> Is the type of component used in mul when embedded inside
 * an app server using JCA.
 * If future we might want to use one of the existing models
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JcaComponent implements UMOComponent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7883378699629557289L;

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(JcaComponent.class);

    private MuleDescriptor descriptor;
    private UMOEntryPoint entryPoint;
    private Object component;

    private boolean started = false;

    /**
     * Determines if the component has been initilised
     */
    private AtomicBoolean initialised = new AtomicBoolean(false);

    private ComponentStatistics stats = null;

    public JcaComponent(MuleDescriptor descriptor)
    {
        if (descriptor == null) {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }
        this.descriptor = descriptor;
    }

    public UMODescriptor getDescriptor()
    {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        try {
            // Invoke method
            // TODO is there anything we can/should do with the return result from invoke()?
            entryPoint.invoke(component, RequestContext.getEventContext());
        } catch (Exception e) {
            throw new MuleException(new Message(Messages.FAILED_TO_INVOKE_X, "UMO Component: " + descriptor.getName()),
                                    e);
        }
    }

    /**
     * This is the synchronous call method and not supported by components
     * managed in a JCA container
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
        started = true;
    }

    public void stop() throws UMOException
    {
        started = false;
    }

    public void dispose()
    {
        ((MuleManager) MuleManager.getInstance()).getStatistics().remove(stats);
    }

    public synchronized void initialise() throws InitialisationException, RecoverableException
    {
        if (initialised.get()) {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALISED, "Component '"
                    + descriptor.getName() + "'"), this);
        }
        descriptor.initialise();
        try {
            entryPoint = MuleManager.getInstance().getModel().getEntryPointResolver().resolveEntryPoint(descriptor);
        } catch (ModelException e) {
            throw new InitialisationException(e, this);
        }

        // initialise statistics
        stats = new ComponentStatistics(descriptor.getName(), -1, -1);

        stats.setEnabled(((MuleManager) MuleManager.getInstance()).getStatistics().isEnabled());
        ((MuleManager) MuleManager.getInstance()).getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());

        component = descriptor.getImplementation();

        initialised.set(true);
        MuleManager.getInstance().fireNotification(new ComponentNotification(descriptor, ComponentNotification.COMPONENT_INITIALISED));
    }

    protected Object getDelegateComponent() throws InitialisationException
    {
        Object impl = descriptor.getImplementation();
        Object component = null;

        try {
            if (impl instanceof ContainerKeyPair) {
                component = MuleManager.getInstance().getContainerContext().getComponent(impl);

                if(descriptor.isSingleton()) {
                    descriptor.setImplementation(component);
                }
            } else {
                component = impl;
            }
        } catch (ObjectNotFoundException e) {
            throw new InitialisationException(e, this);
        }

        // Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);
        return component;
    }

    public boolean isStarted() {
        return started; 
    }

    /**
     * Gets the underlying instance form this component
     * Where the Component implmentation provides pooling this is no 1-2-1 mapping
     * between UMOComponent and instance, so this method will return the object in initial state.
     * <p/>
     * If the underlying component is Container managed in Spring or another IoC container then the
     * object instance in the IoC container will be returned
     *
     * @return the underlying instance form this component
     */
    public Object getInstance() throws UMOException {
        return component;
    }
}
