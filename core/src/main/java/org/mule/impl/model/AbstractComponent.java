/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.providers.AbstractConnector;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.concurrent.WaitableBoolean;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A base implementation for all UMOComponents in Mule
 */
public abstract class AbstractComponent implements UMOComponent
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * The Mule descriptor associated with the component
     */
    protected MuleDescriptor descriptor = null;

    protected ComponentStatistics stats = null;

    /**
     * Determines if the component has been stopped
     */
    protected AtomicBoolean stopped = new AtomicBoolean(true);

    /**
     * Determines whether stop has been called and is still in progress
     */
    protected WaitableBoolean stopping = new WaitableBoolean(false);

    /**
     * determines if the proxy pool has been initialised
     */
    protected AtomicBoolean poolInitialised = new AtomicBoolean(false);

    /**
     * The exception strategy used by the component, this is provided by the
     * UMODescriptor
     */
    protected ExceptionListener exceptionListener = null;

    /**
     * Determines if the component has been initilised
     */
    protected AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * The model in which this component is registered
     */
    protected UMOModel model;

    /**
     * Determines if the component has been paused
     */
    protected WaitableBoolean paused = new WaitableBoolean(false);

    protected String registryId = null;

    protected UMOManagementContext managementContext;

    /**
     * Default constructor
     */
    public AbstractComponent(MuleDescriptor descriptor, UMOModel model)
    {
        if (descriptor == null)
        {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }
        this.descriptor = descriptor;
        this.model = model;
        this.managementContext = descriptor.getManagementContext();
    }

    /**
     * Initialise the component. The component will first create a Mule UMO from the
     * UMODescriptor and then initialise a pool based on the attributes in the
     * UMODescriptor.
     * 
     * @throws org.mule.umo.lifecycle.InitialisationException if the component fails
     *             to initialise
     * @see org.mule.umo.UMODescriptor
     * @param managementContext
     */
    public final synchronized void initialise(UMOManagementContext managementContext) throws InitialisationException
    {
        if (initialised.get())
        {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALISED,
                "Component '" + descriptor.getName() + "'"), this);
        }

        this.managementContext = managementContext;

        try
        {
            register();
        } catch (RegistrationException re)
        {
            logger.info("Unable to register component");
        }
        
        try
        {
            descriptor.register();
        }
        catch (UMOException e)
        {
            logger.error("Unable to register descriptor " + 
                    descriptor.getName() + " with the registry");
        }

        descriptor.initialise(managementContext);

        this.exceptionListener = descriptor.getExceptionListener();

        doInitialise();

        // initialise statistics
        stats = createStatistics();

        stats.setEnabled(managementContext.getStatistics().isEnabled());
        managementContext.getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());
        
        initialised.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_INITIALISED);

    }

    protected ComponentStatistics createStatistics()
    {
        return new ComponentStatistics(getName(), descriptor.getThreadingProfile().getMaxThreadsActive());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        registryId = 
            managementContext.getRegistry().registerMuleObject(model, this).getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#deregister()
     */
    public void deregister() throws DeregistrationException
    {
        managementContext.getRegistry().deregisterComponent(registryId);
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

    protected void fireComponentNotification(int action)
    {
        managementContext.fireNotification(new ComponentNotification(descriptor, action));
    }

    public void forceStop() throws UMOException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPING);
            doForceStop();
            stopped.set(true);
            stopping.set(false);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPED);
        }
    }

    public void stop() throws UMOException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPING);

            // Unregister Listeners for the component
            unregisterListeners();
            if (managementContext.getQueueManager().getQueueSession().getQueue(
                descriptor.getName() + ".component").size() > 0)
            {
                try
                {
                    stopping.whenFalse(null);
                }
                catch (InterruptedException e)
                {
                    // we can ignore this
                }
            }

            doStop();
            stopped.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPED);
        }
    }

    public void start() throws UMOException
    {
        start(false);
    }

    /**
     * Starts a Mule Component.
     * 
     * @param startPaused - Start component in a "paused" state (messages are
     *            received but not processed).
     */
    void start(boolean startPaused) throws UMOException
    {

        // Create the receivers for the component but do not start them yet.
        registerListeners();

        // We connect the receivers _before_ starting the component because there may
        // be
        // some initialization required for the component which needs to have them
        // connected.
        // For example, the org.mule.providers.soap.glue.GlueMessageReceiver adds
        // InitialisationCallbacks within its doConnect() method (see MULE-804).
        connectListeners();

        // Start (and pause) the component.
        if (stopped.get())
        {
            stopped.set(false);
            paused.set(false);
            doStart();
        }
        fireComponentNotification(ComponentNotification.COMPONENT_STARTED);
        if (startPaused)
        {
            pause();
        }

        // We start the receivers _after_ starting the component because if a message
        // gets routed to the component before it is started,
        // org.mule.impl.model.AbstractComponent.dispatchEvent() will throw a
        // ComponentException with message COMPONENT_X_IS_STOPPED (see MULE-526).
        startListeners();
    }

    /**
     * Pauses event processing for a single Mule Component. Unlike stop(), a paused
     * component will still consume messages from the underlying transport, but those
     * messages will be queued until the component is resumed.
     */
    public final void pause() throws UMOException
    {

        doPause();
        paused.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_PAUSED);
    }

    /**
     * Resumes a single Mule Component that has been paused. If the component is not
     * paused nothing is executed.
     */
    public final void resume() throws UMOException
    {
        doResume();
        paused.set(false);
        fireComponentNotification(ComponentNotification.COMPONENT_RESUMED);
    }

    /**
     * Determines if the component is in a paused state
     * 
     * @return True if the component is in a paused state, false otherwise
     */
    public boolean isPaused()
    {
        return paused.get();
    }

    /**
     * Custom components can execute code necessary to put the component in a paused
     * state here. If a developer overloads this method the doResume() method MUST
     * also be overloaded to avoid inconsistent state in the component
     * 
     * @throws UMOException
     */
    protected void doPause() throws UMOException
    {
        // template method
    }

    /**
     * Custom components can execute code necessary to resume a component once it has
     * been paused If a developer overloads this method the doPause() method MUST
     * also be overloaded to avoid inconsistent state in the component
     * 
     * @throws UMOException
     */
    protected void doResume() throws UMOException
    {
        // template method
    }

    public final void dispose()
    {
        try
        {
            if (!stopped.get())
            {
                stop();
            }
        }
        catch (UMOException e)
        {
            logger.error("Failed to stop component: " + descriptor.getName(), e);
        }
        doDispose();
        fireComponentNotification(ComponentNotification.COMPONENT_DISPOSED);
        managementContext.getStatistics().remove(stats);
    }

    public ComponentStatistics getStatistics()
    {
        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOSession#getDescriptor()
     */
    public UMODescriptor getDescriptor()
    {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        if (stopping.get() || stopped.get())
        {
            throw new ComponentException(new Message(Messages.COMPONENT_X_IS_STOPPED,
                getDescriptor().getName()), event.getMessage(), this);
        }

        try
        {
            waitIfPaused(event);
        }
        catch (InterruptedException e)
        {
            throw new ComponentException(event.getMessage(), this, e);
        }

        // Dispatching event to an inbound endpoint
        // in the MuleSession#dispatchEvent
        UMOImmutableEndpoint endpoint = event.getEndpoint();

        if (!endpoint.canReceive())
        {
            try
            {
                endpoint.dispatch(event);
            }
            catch (Exception e)
            {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }

            return;
        }

        // Dispatching event to the component
        if (stats.isEnabled())
        {
            stats.incReceivedEventASync();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Component: " + descriptor.getName() + " has received asynchronous event on: "
                         + event.getEndpoint().getEndpointURI());
        }

        doDispatch(event);
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        if (stopping.get() || stopped.get())
        {
            throw new ComponentException(new Message(Messages.COMPONENT_X_IS_STOPPED,
                getDescriptor().getName()), event.getMessage(), this);
        }

        try
        {
            waitIfPaused(event);
        }
        catch (InterruptedException e)
        {
            throw new ComponentException(event.getMessage(), this, e);
        }

        if (stats.isEnabled())
        {
            stats.incReceivedEventSync();
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Component: " + descriptor.getName() + " has received synchronous event on: "
                         + event.getEndpoint().getEndpointURI());
        }
        RequestContext.setEvent(event);
        return doSend(event);
    }

    /**
     * Called before an event is sent or dispatched to a component, it will block
     * until resume() is called. Users can override this method if they want to
     * handle pausing differently e.g. implement a store and forward policy
     * 
     * @param event the current event being passed to the component
     * @throws InterruptedException if the thread is interrupted
     */
    protected void waitIfPaused(UMOEvent event) throws InterruptedException
    {
        if (logger.isDebugEnabled() && paused.get())
        {
            logger.debug("Component: " + descriptor.getName()
                         + " is paused. Blocking call until resume is called");
        }
        paused.whenFalse(null);
    }

    /**
     * @return the Mule descriptor name which is associated with the component
     */
    public String getName()
    {
        return descriptor.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return descriptor.getName();
    }

    public boolean isStopped()
    {
        return stopped.get();
    }

    public boolean isStopping()
    {
        return stopping.get();
    }

    protected void handleException(Exception e)
    {
        if (exceptionListener instanceof DefaultComponentExceptionStrategy)
        {
            if (((DefaultComponentExceptionStrategy)exceptionListener).getComponent() == null)
            {
                ((DefaultComponentExceptionStrategy)exceptionListener).setComponent(this);
            }
        }
        exceptionListener.exceptionThrown(e);
    }

    /**
     * Provides a consistent mechanism for custom models to create components.
     * 
     * @return
     * @throws UMOException
     */
    protected Object lookupComponent() throws UMOException
    {
        return ComponentFactory.createComponent(getDescriptor());
    }

    protected void doForceStop() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doInitialise() throws InitialisationException
    {
        // template method
    }

    public boolean isStarted()
    {
        return !stopped.get();
    }

    protected abstract UMOMessage doSend(UMOEvent event) throws UMOException;

    protected abstract void doDispatch(UMOEvent event) throws UMOException;

    public Object getInstance() throws UMOException
    {
        return lookupComponent();
    }

    protected void registerListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint)it.next();
            try
            {
                endpoint.getConnector().registerListener(this, endpoint);
            }
            catch (UMOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new ModelException(new Message(Messages.FAILED_TO_REGISTER_X_ON_ENDPOINT_X,
                    getDescriptor().getName(), endpoint.getEndpointURI()), e);
            }
        }
    }

    protected void unregisterListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint)it.next();
            try
            {
                endpoint.getConnector().unregisterListener(this, endpoint);
            }
            catch (UMOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new ModelException(new Message(Messages.FAILED_TO_UNREGISTER_X_ON_ENDPOINT_X,
                    getDescriptor().getName(), endpoint.getEndpointURI()), e);
            }
        }
    }

    protected void startListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint)it.next();
            UMOMessageReceiver receiver = ((AbstractConnector)endpoint.getConnector()).getReceiver(this,
                endpoint);
            if (receiver != null && endpoint.getConnector().isStarted()
                && endpoint.getInitialState().equals(UMOEndpoint.INITIAL_STATE_STARTED))
            {
                receiver.start();
            }
        }
    }

    protected void stopListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint)it.next();
            UMOMessageReceiver receiver = ((AbstractConnector)endpoint.getConnector()).getReceiver(this,
                endpoint);
            if (receiver != null)
            {
                receiver.stop();
            }
        }
    }

    protected void connectListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint)it.next();
            UMOMessageReceiver receiver = ((AbstractConnector)endpoint.getConnector()).getReceiver(this,
                endpoint);
            if (receiver != null)
            {
                try
                {
                    receiver.connect();
                }
                catch (Exception e)
                {
                    throw new ModelException(
                        Message.createStaticMessage("Failed to connect listener "
                                    + receiver + " for endpoint " + endpoint.getName()), e);
                }
            }
        }
    }

    protected void disconnectListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint)it.next();
            UMOMessageReceiver receiver = ((AbstractConnector)endpoint.getConnector()).getReceiver(this,
                endpoint);
            if (receiver != null)
            {
                try
                {
                    receiver.disconnect();
                }
                catch (Exception e)
                {
                    throw new ModelException(Message.createStaticMessage("Failed to disconnect listener "
                                    + receiver + " for endpoint " + endpoint.getName()), e);
                }
            }
        }
    }

    /**
     * Returns a list of all incoming endpoints on a component.
     */
    protected List getIncomingEndpoints()
    {
        List endpoints = new ArrayList();

        // Add inbound endpoints
        endpoints.addAll(getDescriptor().getInboundRouter().getEndpoints());

        // Add response endpoints
        if (getDescriptor().getResponseRouter() != null
            && getDescriptor().getResponseRouter().getEndpoints() != null)
        {
            endpoints.addAll(getDescriptor().getResponseRouter().getEndpoints());
        }
        return endpoints;
    }

}
