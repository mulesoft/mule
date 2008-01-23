/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.component.ComponentAware;
import org.mule.api.component.ComponentException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.Model;
import org.mule.api.model.ModelException;
import org.mule.api.model.MuleProxy;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.NestedRouterCollection;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.routing.nested.DefaultNestedRouterCollection;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.response.DefaultResponseRouterCollection;
import org.mule.transport.AbstractConnector;
import org.mule.util.concurrent.WaitableBoolean;
import org.mule.util.object.ObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Collection;
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
public abstract class AbstractComponent implements Component
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
    protected Model model;

    /**
     * Determines if the component has been paused
     */
    protected WaitableBoolean paused = new WaitableBoolean(false);

    protected MuleContext muleContext;

    protected EntryPointResolverSet entryPointResolverSet;

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
     * By default a singleton object factory with the {@link PassThroughComponent} is used
     */
    protected ObjectFactory serviceFactory = new SingletonObjectFactory(PassThroughComponent.class);

    /**
     * The component's name
     */
    protected String name;

    protected InboundRouterCollection inboundRouter = new DefaultInboundRouterCollection();

    protected OutboundRouterCollection outboundRouter = new DefaultOutboundRouterCollection();

    protected NestedRouterCollection nestedRouter = new DefaultNestedRouterCollection();

    protected ResponseRouterCollection responseRouter = new DefaultResponseRouterCollection();

    /**
     * Determines the initial state of this component when the model starts. Can be
     * 'stopped' or 'started' (default)
     */
    protected String initialState = INITIAL_STATE_STARTED;

    protected List initialisationCallbacks = new ArrayList();

    /**
     * Indicates whether a component has passed its initial startup state.
     */
    private AtomicBoolean beyondInitialState = new AtomicBoolean(false);

    /**
     * The properties for the Mule UMO.
     *
     * @deprecated MULE-1933 Properties for the underlying service should be set on the ServiceFactory instead.
     */
    protected Map properties = new HashMap();

    /**
     * For Spring only
     */
    public AbstractComponent()
    {
        // nop
    }

    /**
     * Initialise the component. The component will first create a Mule UMO from the
     * UMODescriptor and then initialise a pool based on the attributes in the
     * UMODescriptor.
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *          if the component fails
     *          to initialise
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
            inboundRouter = new DefaultInboundRouterCollection();
            // TODO MULE-2102 This should be configured in the default template.
            inboundRouter.addRouter(new InboundPassThroughRouter());
        }
        if (outboundRouter == null)
        {
            outboundRouter = new DefaultOutboundRouterCollection();
            // TODO MULE-2102 This should be configured in the default template.
            outboundRouter.addRouter(new OutboundPassThroughRouter());
        }
        if (responseRouter == null)
        {
            responseRouter = new DefaultResponseRouterCollection();
        }
        if (nestedRouter == null)
        {
            nestedRouter = new DefaultNestedRouterCollection();
        }

        if (exceptionListener == null)
        {
            // TODO MULE-2102 This should be configured in the default template.
            exceptionListener = new DefaultComponentExceptionStrategy(this);
            ((MuleContextAware) exceptionListener).setMuleContext(muleContext);
            ((Initialisable) exceptionListener).initialise();
        }

        serviceFactory.initialise();

        doInitialise();

        // initialise statistics
        stats = createStatistics();

        stats.setEnabled(muleContext.getStatistics().isEnabled());
        muleContext.getStatistics().add(stats);
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
        muleContext.fireNotification(new ComponentNotification(this, action));
    }

    public void forceStop() throws MuleException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping Component");
            stopping.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPING);
            doForceStop();
            stopped.set(true);
            stopping.set(false);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPED);
        }
    }

    public void stop() throws MuleException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping Component");
            stopping.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPING);

            // Unregister Listeners for the component
            unregisterListeners();

            doStop();
            stopped.set(true);
            initialised.set(false);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPED);
            logger.info("Mule Component " + name + " has been stopped successfully");
        }
    }

    public void start() throws MuleException
    {
        if (isStarted())
        {
            logger.info("Component is already started: " + name);
            return;
        }

        if (!beyondInitialState.get() && initialState.equals(AbstractComponent.INITIAL_STATE_STOPPED))
        {
            logger.info("Component " + name + " has not been started (initial state = 'stopped')");
        }
        else if (!beyondInitialState.get() && initialState.equals(AbstractComponent.INITIAL_STATE_PAUSED))
        {
            start(/*startPaused*/true);
            logger.info("Component " + name + " has been started and paused (initial state = 'paused')");
        }
        else
        {
            start(/*startPaused*/false);
            logger.info("Component " + name + " has been started successfully");
        }

        beyondInitialState.set(true);
    }

    /**
     * Starts a Mule Component.
     *
     * @param startPaused - Start component in a "paused" state (messages are
     *                    received but not processed).
     */
    protected void start(boolean startPaused) throws MuleException
    {
        // Create the receivers for the component but do not start them yet.
        registerListeners();

        // We connect the receivers _before_ starting the component because there may
        // be
        // some initialization required for the component which needs to have them
        // connected.
        // For example, the org.mule.transport.soap.glue.GlueMessageReceiver adds
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
        // org.mule.model.AbstractComponent.dispatchEvent() will throw a
        // ComponentException with message COMPONENT_X_IS_STOPPED (see MULE-526).
        startListeners();
    }

    /**
     * Pauses event processing for a single Mule Component. Unlike stop(), a paused
     * component will still consume messages from the underlying transport, but those
     * messages will be queued until the component is resumed.
     */
    public final void pause() throws MuleException
    {
        doPause();
        paused.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_PAUSED);
        logger.info("Mule Component " + name + " has been paused successfully");
    }

    /**
     * Resumes a single Mule Component that has been paused. If the component is not
     * paused nothing is executed.
     */
    public final void resume() throws MuleException
    {
        doResume();
        paused.set(false);
        fireComponentNotification(ComponentNotification.COMPONENT_RESUMED);
        logger.info("Mule Component " + name + " has been resumed successfully");
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
     * @throws MuleException
     */
    protected void doPause() throws MuleException
    {
        // template method
    }

    /**
     * Custom components can execute code necessary to resume a component once it has
     * been paused If a developer overloads this method the doPause() method MUST
     * also be overloaded to avoid inconsistent state in the component
     *
     * @throws MuleException
     */
    protected void doResume() throws MuleException
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
        catch (MuleException e)
        {
            // TODO MULE-863: If this is an error, do something!
            logger.error("Failed to stop component: " + name, e);
        }
        doDispose();
        fireComponentNotification(ComponentNotification.COMPONENT_DISPOSED);
        muleContext.getStatistics().remove(stats);
    }

    public ComponentStatistics getStatistics()
    {
        return stats;
    }

    public void dispatchEvent(MuleEvent event) throws MuleException
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
        // in the DefaultMuleSession#dispatchEvent
        ImmutableEndpoint endpoint = event.getEndpoint();

        if (!endpoint.canRequest())
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

    public MuleMessage sendEvent(MuleEvent event) throws MuleException
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
    protected void waitIfPaused(MuleEvent event) throws InterruptedException
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

    protected void doForceStop() throws MuleException
    {
        // template method
    }

    protected void doStop() throws MuleException
    {
        // template method
    }

    protected void doStart() throws MuleException
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

    protected abstract MuleMessage doSend(MuleEvent event) throws MuleException;

    protected abstract void doDispatch(MuleEvent event) throws MuleException;

    protected void registerListeners() throws MuleException
    {
        Endpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (Endpoint) it.next();
            try
            {
                endpoint.getConnector().registerListener(this, endpoint);
            }
            catch (MuleException e)
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

    protected void unregisterListeners() throws MuleException
    {
        Endpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (Endpoint) it.next();
            try
            {
                endpoint.getConnector().unregisterListener(this, endpoint);
            }
            catch (MuleException e)
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

    protected void startListeners() throws MuleException
    {
        Endpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (Endpoint) it.next();
            MessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
                    endpoint);
            if (receiver != null && endpoint.getConnector().isStarted()
                    && endpoint.getInitialState().equals(Endpoint.INITIAL_STATE_STARTED))
            {
                receiver.start();
            }
        }
    }

    protected void stopListeners() throws MuleException
    {
        Endpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (Endpoint) it.next();
            MessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
                    endpoint);
            if (receiver != null)
            {
                receiver.stop();
            }
        }
    }

    protected void connectListeners() throws MuleException
    {
        Endpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (Endpoint) it.next();
            MessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
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

    protected void disconnectListeners() throws MuleException
    {
        Endpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (Endpoint) it.next();
            MessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
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

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected MuleProxy createComponentProxy(Object pojoService) throws MuleException
    {
        MuleProxy proxy = new DefaultMuleProxy(pojoService, this, muleContext);
        proxy.setStatistics(getStatistics());
        return proxy;
    }

    protected Object getOrCreateService() throws MuleException
    {
        if (serviceFactory == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Component " + name + " has not been initialized properly, no serviceFactory."), this);
        }

        Object component;
        try
        {
            component = serviceFactory.getOrCreate();
            if (component instanceof ComponentAware)
            {
                ((ComponentAware) component).setComponent(this);
            }
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

    public Model getModel()
    {
        return model;
    }

    public void setModel(Model model)
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

    public InboundRouterCollection getInboundRouter()
    {
        return inboundRouter;
    }

    public void setInboundRouter(InboundRouterCollection inboundRouter)
    {
        this.inboundRouter = inboundRouter;
    }

    public NestedRouterCollection getNestedRouter()
    {
        return nestedRouter;
    }

    public void setNestedRouter(NestedRouterCollection nestedRouter)
    {
        this.nestedRouter = nestedRouter;
    }

    public OutboundRouterCollection getOutboundRouter()
    {
        return outboundRouter;
    }

    public void setOutboundRouter(OutboundRouterCollection outboundRouter)
    {
        this.outboundRouter = outboundRouter;
    }

    public ResponseRouterCollection getResponseRouter()
    {
        return responseRouter;
    }

    public void setResponseRouter(ResponseRouterCollection responseRouter)
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
    public EntryPointResolverSet getEntryPointResolverSet()
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
    public void setEntryPointResolverSet(EntryPointResolverSet resolverSet)
    {
        this.entryPointResolverSet = resolverSet;
    }

    /**
     * Allow for incremental addition of resolvers
     *
     * @param entryPointResolvers Resolvers to add
     */
    public void setEntryPointResolvers(Collection entryPointResolvers)
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = new DefaultEntryPointResolverSet();
        }
        for (Iterator resolvers = entryPointResolvers.iterator(); resolvers.hasNext();)
        {
            entryPointResolverSet.addEntryPointResolver((EntryPointResolver) resolvers.next());
        }
    }

}
