/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.InitialisationCallback;
import org.mule.impl.ManagementContextAware;
import org.mule.impl.OptimizedRequestContext;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.providers.AbstractConnector;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.nested.NestedRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.routing.response.ResponseRouterCollection;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPointResolverSet;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.util.concurrent.WaitableBoolean;
import org.mule.util.object.ObjectFactory;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

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

    protected UMOManagementContext managementContext;
    
    protected UMOEntryPointResolverSet entryPointResolverSet;
    
    /**
     * The initial states that the component can be started in
     */
    public static final String INITIAL_STATE_STOPPED = "stopped";
    public static final String INITIAL_STATE_STARTED = "started";
    public static final String INITIAL_STATE_PAUSED = "paused";

    /**
     * The exception strategy used by the component.
     */
    protected ExceptionListener exceptionListener;

    /**
     * Factory which creates an instance of the actual service object.
     */
    protected ObjectFactory serviceFactory;

    /**
     * The component's name
     */
    protected String name;

    protected UMOInboundRouterCollection inboundRouter;

    protected UMOOutboundRouterCollection outboundRouter;

    protected UMONestedRouterCollection nestedRouter;

    protected UMOResponseRouterCollection responseRouter;

    /**
     * Determines the initial state of this component when the model starts. Can be
     * 'stopped' or 'started' (default)
     */
    protected String initialState = INITIAL_STATE_STARTED;

    protected List initialisationCallbacks = new ArrayList();

    /**
     * The properties for the Mule UMO.
     * 
     * @deprecated MULE-1933 Properties for the underlying service should be set on the ServiceFactory instead.
     */
    protected Map properties = new HashMap();
    
    /** For Spring only */
    public AbstractComponent()
    {
        // nop
    }
    
    /**
     * Initialise the component. The component will first create a Mule UMO from the
     * UMODescriptor and then initialise a pool based on the attributes in the
     * UMODescriptor.
     * 
     * @throws org.mule.umo.lifecycle.InitialisationException if the component fails
     *             to initialise
     * @see org.mule.umo.UMODescriptor
     */
    public final synchronized void initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            throw new InitialisationException(
                CoreMessages.objectAlreadyInitialised("Component '" + name + "'"), this);
        }

        if (inboundRouter == null)
        {
            // Create Default routes that route to the default inbound and
            // outbound endpoints
            inboundRouter = new InboundRouterCollection();
            // TODO MULE-2102 This should be configured in the default template.
            inboundRouter.addRouter(new InboundPassThroughRouter());
        }
        if (outboundRouter == null)
        {
            outboundRouter = new OutboundRouterCollection();
            // TODO MULE-2102 This should be configured in the default template.
            outboundRouter.addRouter(new OutboundPassThroughRouter());
        }
        if (responseRouter == null)
        {
            responseRouter = new ResponseRouterCollection();
        }
        if (nestedRouter == null)
        {
            nestedRouter = new NestedRouterCollection();
        }

        if (exceptionListener == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            exceptionListener = new DefaultComponentExceptionStrategy(this);
            ((ManagementContextAware) exceptionListener).setManagementContext(managementContext);
            ((Initialisable) exceptionListener).initialise();
        }
        
        doInitialise();

        // initialise statistics
        stats = createStatistics();

        stats.setEnabled(managementContext.getStatistics().isEnabled());
        managementContext.getStatistics().add(stats);
        stats.setOutboundRouterStat(outboundRouter.getStatistics());
        stats.setInboundRouterStat(inboundRouter.getStatistics());
        
        initialised.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_INITIALISED);

    }

    protected ComponentStatistics createStatistics()
    {
        return new ComponentStatistics(name);
    }

    protected void fireComponentNotification(int action)
    {
        managementContext.fireNotification(new ComponentNotification(this, action));
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
                name + ".component").size() > 0)
            {
                try
                {
                    stopping.whenFalse(null);
                }
                catch (InterruptedException e)
                {
                    // we can ignore this
                    // TODO MULE-863: Why?
                }
            }

            doStop();
            stopped.set(true);
            initialised.set(false);
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
            // TODO MULE-863: If this is an error, do something!
            logger.error("Failed to stop component: " + name, e);
        }
        doDispose();
        fireComponentNotification(ComponentNotification.COMPONENT_DISPOSED);
        managementContext.getStatistics().remove(stats);
    }

    public ComponentStatistics getStatistics()
    {
        return stats;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        if (stopping.get() || stopped.get())
        {
            throw new ComponentException(
                CoreMessages.componentIsStopped(name), 
                event.getMessage(), this);
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
            logger.debug("Component: " + name + " has received asynchronous event on: "
                         + event.getEndpoint().getEndpointURI());
        }

        doDispatch(event);
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        if (stopping.get() || stopped.get())
        {
            throw new ComponentException(
                CoreMessages.componentIsStopped(name), 
                event.getMessage(), this);
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
            logger.debug("Component: " + name + " has received synchronous event on: "
                         + event.getEndpoint().getEndpointURI());
        }
        event = OptimizedRequestContext.unsafeSetEvent(event);
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
            logger.debug("Component: " + name
                         + " is paused. Blocking call until resume is called");
        }
        paused.whenFalse(null);
    }

    /**
     * @return the Mule descriptor name which is associated with the component
     */
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getName();
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
            if (((DefaultComponentExceptionStrategy) exceptionListener).getComponent() == null)
            {
                ((DefaultComponentExceptionStrategy) exceptionListener).setComponent(this);
            }
        }
        exceptionListener.exceptionThrown(e);
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

    protected void registerListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint) it.next();
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
                throw new ModelException(
                    CoreMessages.failedtoRegisterOnEndpoint(name, endpoint.getEndpointURI()), e);
            }
        }
    }

    protected void unregisterListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint) it.next();
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
                throw new ModelException(
                    CoreMessages.failedToUnregister(name, endpoint.getEndpointURI()), e);
            }
        }
    }

    protected void startListeners() throws UMOException
    {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
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
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
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
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
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
                        MessageFactory.createStaticMessage("Failed to connect listener "
                                    + receiver + " for endpoint " + endpoint.getName()),
                        e);
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
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
                endpoint);
            if (receiver != null)
            {
                try
                {
                    receiver.disconnect();
                }
                catch (Exception e)
                {
                    throw new ModelException(
                        MessageFactory.createStaticMessage("Failed to disconnect listener "
                                    + receiver + " for endpoint " + endpoint.getName()),
                        e);
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
        endpoints.addAll(inboundRouter.getEndpoints());

        // Add response endpoints
        if (responseRouter != null
            && responseRouter.getEndpoints() != null)
        {
            endpoints.addAll(responseRouter.getEndpoints());
        }
        return endpoints;
    }
    
    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }
    
    protected MuleProxy createComponentProxy(Object pojoService) throws UMOException
    {
        MuleProxy proxy = new DefaultMuleProxy(pojoService, this, managementContext);
        proxy.setStatistics(getStatistics());
        return proxy;
    }

    protected Object getOrCreateService() throws UMOException
    {
        if (serviceFactory == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Component " + name + " has not been initialized properly, no serviceFactory."), this);
        }
        
        Object component;
        try
        {
            component = serviceFactory.getOrCreate();
        }
        catch (Exception e)
        {
            throw new LifecycleException(MessageFactory.createStaticMessage("Unable to create instance of POJO service"), e, this);
        }

        // Call any custom initialisers
        fireInitialisationCallbacks(component);

        return component;
    }

    public void fireInitialisationCallbacks(Object component) throws InitialisationException
    {
        InitialisationCallback callback;
        for (Iterator iterator = initialisationCallbacks.iterator(); iterator.hasNext();)
        {
            callback = (InitialisationCallback) iterator.next();
            callback.initialise(component);
        }
    }

    public void addInitialisationCallback(InitialisationCallback callback)
    {
        initialisationCallbacks.add(callback);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public UMOModel getModel()
    {
        return model;
    }

    public void setModel(UMOModel model)
    {
        this.model = model;
    }

    public ObjectFactory getServiceFactory()
    {
        return serviceFactory;
    }

    public void setServiceFactory(ObjectFactory serviceFactory)
    {
        this.serviceFactory = serviceFactory;
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    public UMOInboundRouterCollection getInboundRouter()
    {
        return inboundRouter;
    }

    public void setInboundRouter(UMOInboundRouterCollection inboundRouter)
    {
        this.inboundRouter = inboundRouter;
    }

    public UMONestedRouterCollection getNestedRouter()
    {
        return nestedRouter;
    }

    public void setNestedRouter(UMONestedRouterCollection nestedRouter)
    {
        this.nestedRouter = nestedRouter;
    }

    public UMOOutboundRouterCollection getOutboundRouter()
    {
        return outboundRouter;
    }

    public void setOutboundRouter(UMOOutboundRouterCollection outboundRouter)
    {
        this.outboundRouter = outboundRouter;
    }

    public UMOResponseRouterCollection getResponseRouter()
    {
        return responseRouter;
    }

    public void setResponseRouter(UMOResponseRouterCollection responseRouter)
    {
        this.responseRouter = responseRouter;
    }

    public String getInitialState()
    {
        return initialState;
    }

    public void setInitialState(String initialState)
    {
        this.initialState = initialState;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    /**
     * A descriptor can have a custom entrypoint resolver for its own object.
     * By default this is null. When set this resolver will override the resolver on the model
     *
     * @return Null is a resolver set has not been set otherwise the resolver to use
     *         on this component
     */
    public UMOEntryPointResolverSet getEntryPointResolverSet()
    {
        return entryPointResolverSet;
    }

    /**
     * A descriptor can have a custom entrypoint resolver for its own object.
     * By default this is null. When set this resolver will override the resolver on the model
     *
     * @param resolverSet theresolver set to use when resolving entry points
     *                    on this component
     */
    public void setEntryPointResolverSet(UMOEntryPointResolverSet resolverSet)
    {
        this.entryPointResolverSet = entryPointResolverSet;
    }
}
