/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleLogic;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.Model;
import org.mule.api.model.ModelException;
import org.mule.api.model.MuleProxy;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.NestedRouterCollection;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.api.service.ServiceException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.DefaultMuleProxy;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.notification.ServiceNotification;
import org.mule.management.stats.ServiceStatistics;
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
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A base implementation for all UMOComponents in Mule
 */
public abstract class AbstractService implements Service
{
    
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ServiceStatistics stats = null;

    /**
     * Determines if the service has been stopped
     */
    protected AtomicBoolean stopped = new AtomicBoolean(true);

    /**
     * Determines whether stop has been called and is still in progress
     */
    protected WaitableBoolean stopping = new WaitableBoolean(false);

    /**
     * Determines if the service has been initilised
     */
    protected AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * The model in which this service is registered
     */
    protected Model model;

    /**
     * Determines if the service has been paused
     */
    protected WaitableBoolean paused = new WaitableBoolean(false);

    protected MuleContext muleContext;

    protected EntryPointResolverSet entryPointResolverSet;

    /**
     * The initial states that the service can be started in
     */
    public static final String INITIAL_STATE_STOPPED = "stopped";
    public static final String INITIAL_STATE_STARTED = "started";
    public static final String INITIAL_STATE_PAUSED = "paused";

    /**
     * The exception strategy used by the service.
     */
    protected ExceptionListener exceptionListener;

    /**
     * Factory which creates an instance of the actual service object.
     * By default a singleton object factory with the {@link PassThroughComponent} is used
     */
    protected ObjectFactory componentFactory = new SingletonObjectFactory(PassThroughComponent.class);

    /**
     * The service's name
     */
    protected String name;

    protected InboundRouterCollection inboundRouter = new DefaultInboundRouterCollection();

    protected OutboundRouterCollection outboundRouter = new DefaultOutboundRouterCollection();

    protected NestedRouterCollection nestedRouter = new DefaultNestedRouterCollection();

    protected ResponseRouterCollection responseRouter = new DefaultResponseRouterCollection();

    /**
     * Determines the initial state of this service when the model starts. Can be
     * 'stopped' or 'started' (default)
     */
    protected String initialState = INITIAL_STATE_STARTED;

    protected List initialisationCallbacks = new ArrayList();

    /**
     * Indicates whether a service has passed its initial startup state.
     */
    private AtomicBoolean beyondInitialState = new AtomicBoolean(false);

    /**
     * For Spring only
     */
    public AbstractService()
    {
        // nop
    }

    /**
     * Initialise the service. The service will first create a Mule UMO from the
     * UMODescriptor and then initialise a pool based on the attributes in the
     * UMODescriptor.
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *          if the service fails
     *          to initialise
     */
    public final synchronized LifecycleTransitionResult initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            throw new InitialisationException(
                    CoreMessages.objectAlreadyInitialised("Service '" + name + "'"), this);
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
            exceptionListener = new DefaultServiceExceptionStrategy(this);
            ((MuleContextAware) exceptionListener).setMuleContext(muleContext);
            ((Initialisable) exceptionListener).initialise();
        }

        return LifecycleLogic.initialiseAll(this, componentFactory.initialise(), new LifecycleLogic.Closure()
        {
            public LifecycleTransitionResult doContinue() throws InitialisationException
            {
                doInitialise();

                // initialise statistics
                stats = createStatistics();

                stats.setEnabled(muleContext.getStatistics().isEnabled());
                muleContext.getStatistics().add(stats);
                stats.setOutboundRouterStat(outboundRouter.getStatistics());
                stats.setInboundRouterStat(inboundRouter.getStatistics());

                initialised.set(true);
                fireComponentNotification(ServiceNotification.SERVICE_INITIALISED);

                return LifecycleTransitionResult.OK;
            }});
    }

    protected ServiceStatistics createStatistics()
    {
        return new ServiceStatistics(name);
    }

    protected void fireComponentNotification(int action)
    {
        muleContext.fireNotification(new ServiceNotification(this, action));
    }

    public void forceStop() throws MuleException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping Service");
            stopping.set(true);
            fireComponentNotification(ServiceNotification.SERVICE_STOPPING);
            doForceStop();
            stopped.set(true);
            stopping.set(false);
            fireComponentNotification(ServiceNotification.SERVICE_STOPPED);
        }
    }

    public LifecycleTransitionResult stop() throws MuleException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping Service");
            stopping.set(true);
            fireComponentNotification(ServiceNotification.SERVICE_STOPPING);

            // Unregister Listeners for the service
            unregisterListeners();

            doStop();
            stopped.set(true);
            initialised.set(false);
            fireComponentNotification(ServiceNotification.SERVICE_STOPPED);
            logger.info("Mule Service " + name + " has been stopped successfully");
        }
        return LifecycleTransitionResult.OK;
    }

    public LifecycleTransitionResult start() throws MuleException
    {
        LifecycleTransitionResult status = LifecycleTransitionResult.OK;
        if (isStarted())
        {
            logger.info("Service is already started: " + name);
        }
        else
        {
            if (initialState.equals(AbstractService.INITIAL_STATE_STOPPED))
            {
                System.out.println("stopped");
            }
            if (!beyondInitialState.get() && initialState.equals(AbstractService.INITIAL_STATE_STOPPED))
            {
                logger.info("Service " + name + " has not been started (initial state = 'stopped')");
            }
            else if (!beyondInitialState.get() && initialState.equals(AbstractService.INITIAL_STATE_PAUSED))
            {
                status = start(/*startPaused*/true);
                logger.info("Service " + name + " has been started and paused (initial state = 'paused')");
            }
            else
            {
                status = start(/*startPaused*/false);
                logger.info("Service " + name + " has been started successfully");
            }
            beyondInitialState.set(true);
        }
        return status;
    }

    /**
     * Starts a Mule Service.
     *
     * @param startPaused - Start service in a "paused" state (messages are
     *                    received but not processed).
     */
    protected LifecycleTransitionResult start(boolean startPaused) throws MuleException
    {
        // Create the receivers for the service but do not start them yet.
        registerListeners();

        // We connect the receivers _before_ starting the service because there may
        // be
        // some initialization required for the service which needs to have them
        // connected.
        // For example, the org.mule.transport.soap.glue.GlueMessageReceiver adds
        // InitialisationCallbacks within its doConnect() method (see MULE-804).
        connectListeners();

        // Start (and pause) the service.
        if (stopped.get())
        {
            stopped.set(false);
            paused.set(false);
            doStart();
        }
        fireComponentNotification(ServiceNotification.SERVICE_STARTED);
        if (startPaused)
        {
            pause();
        }

        // We start the receivers _after_ starting the service because if a message
        // gets routed to the service before it is started,
        // org.mule.model.AbstractComponent.dispatchEvent() will throw a
        // ServiceException with message COMPONENT_X_IS_STOPPED (see MULE-526).
        return startListeners();
    }

    /**
     * Pauses event processing for a single Mule Service. Unlike stop(), a paused
     * service will still consume messages from the underlying transport, but those
     * messages will be queued until the service is resumed.
     */
    public final void pause() throws MuleException
    {
        doPause();
        paused.set(true);
        fireComponentNotification(ServiceNotification.SERVICE_PAUSED);
        logger.info("Mule Service " + name + " has been paused successfully");
    }

    /**
     * Resumes a single Mule Service that has been paused. If the service is not
     * paused nothing is executed.
     */
    public final void resume() throws MuleException
    {
        doResume();
        paused.set(false);
        fireComponentNotification(ServiceNotification.SERVICE_RESUMED);
        logger.info("Mule Service " + name + " has been resumed successfully");
    }

    /**
     * Determines if the service is in a paused state
     *
     * @return True if the service is in a paused state, false otherwise
     */
    public boolean isPaused()
    {
        return paused.get();
    }

    /**
     * Custom components can execute code necessary to put the service in a paused
     * state here. If a developer overloads this method the doResume() method MUST
     * also be overloaded to avoid inconsistent state in the service
     *
     * @throws MuleException
     */
    protected void doPause() throws MuleException
    {
        // template method
    }

    /**
     * Custom components can execute code necessary to resume a service once it has
     * been paused If a developer overloads this method the doPause() method MUST
     * also be overloaded to avoid inconsistent state in the service
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
            logger.error("Failed to stop service: " + name, e);
        }
        doDispose();
        fireComponentNotification(ServiceNotification.SERVICE_DISPOSED);
        muleContext.getStatistics().remove(stats);
    }

    public ServiceStatistics getStatistics()
    {
        return stats;
    }

    public void dispatchEvent(MuleEvent event) throws MuleException
    {
        if (stopping.get() || stopped.get())
        {
            throw new ServiceException(
                    CoreMessages.componentIsStopped(name),
                    event.getMessage(), this);
        }

        try
        {
            waitIfPaused(event);
        }
        catch (InterruptedException e)
        {
            throw new ServiceException(event.getMessage(), this, e);
        }

        // Dispatching event to an inbound endpoint
        // in the DefaultMuleSession#dispatchEvent
        ImmutableEndpoint endpoint = event.getEndpoint();

        if (endpoint instanceof OutboundEndpoint)
        {
            try
            {
                ((OutboundEndpoint) endpoint).dispatch(event);
            }
            catch (Exception e)
            {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }

            return;
        }

        // Dispatching event to the service
        if (stats.isEnabled())
        {
            stats.incReceivedEventASync();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Service: " + name + " has received asynchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }

        doDispatch(event);
    }

    public MuleMessage sendEvent(MuleEvent event) throws MuleException
    {
        if (stopping.get() || stopped.get())
        {
            throw new ServiceException(
                    CoreMessages.componentIsStopped(name),
                    event.getMessage(), this);
        }

        try
        {
            waitIfPaused(event);
        }
        catch (InterruptedException e)
        {
            throw new ServiceException(event.getMessage(), this, e);
        }

        if (stats.isEnabled())
        {
            stats.incReceivedEventSync();
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Service: " + name + " has received synchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }
        event = OptimizedRequestContext.unsafeSetEvent(event);
        return doSend(event);
    }

    /**
     * Called before an event is sent or dispatched to a service, it will block
     * until resume() is called. Users can override this method if they want to
     * handle pausing differently e.g. implement a store and forward policy
     *
     * @param event the current event being passed to the service
     * @throws InterruptedException if the thread is interrupted
     */
    protected void waitIfPaused(MuleEvent event) throws InterruptedException
    {
        if (logger.isDebugEnabled() && paused.get())
        {
            logger.debug("Service: " + name
                    + " is paused. Blocking call until resume is called");
        }
        paused.whenFalse(null);
    }

    /**
     * @return the Mule descriptor name which is associated with the service
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
        if (exceptionListener instanceof DefaultServiceExceptionStrategy)
        {
            if (((DefaultServiceExceptionStrategy) exceptionListener).getService() == null)
            {
                ((DefaultServiceExceptionStrategy) exceptionListener).setService(this);
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
        InboundEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (InboundEndpoint) it.next();
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
        InboundEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (InboundEndpoint) it.next();
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

    protected LifecycleTransitionResult startListeners() throws MuleException
    {
        InboundEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (InboundEndpoint) it.next();
            MessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
                    endpoint);
            if (receiver != null && endpoint.getConnector().isStarted()
                    && endpoint.getInitialState().equals(ImmutableEndpoint.INITIAL_STATE_STARTED))
            {
                LifecycleTransitionResult result = receiver.start();
                if (! result.isOk())
                {
                    throw (InitialisationException) new InitialisationException(CoreMessages.nestedRetry(), receiver)
                            .initCause(result.getThrowable());
                }
            }
        }
        return LifecycleTransitionResult.OK;
    }

    // This is not called by anything?!
    protected LifecycleTransitionResult stopListeners() throws MuleException
    {
        InboundEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (InboundEndpoint) it.next();
            MessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(this,
                    endpoint);
            if (receiver != null)
            {
                LifecycleTransitionResult result = receiver.stop();
                if (! result.isOk())
                {
                    throw (InitialisationException) new InitialisationException(CoreMessages.nestedRetry(), receiver)
                            .initCause(result.getThrowable());
                }
            }
        }
        return LifecycleTransitionResult.OK;
    }

    protected void connectListeners() throws MuleException
    {
        InboundEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (InboundEndpoint) it.next();
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
        InboundEndpoint endpoint;
        List endpoints = getIncomingEndpoints();

        for (Iterator it = endpoints.iterator(); it.hasNext();)
        {
            endpoint = (InboundEndpoint) it.next();
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
     * Returns a list of all incoming endpoints on a service.
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

    protected MuleProxy createComponentProxy(Object pojo) throws MuleException
    {
        MuleProxy proxy = new DefaultMuleProxy(pojo, this, muleContext);
        proxy.setStatistics(getStatistics());
        return proxy;
    }

    protected Object getOrCreateService() throws MuleException
    {
        if (componentFactory == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Service " + name + " has not been initialized properly, no serviceFactory."), this);
        }

        Object component;
        try
        {
            component = componentFactory.getInstance();
            if (component instanceof ServiceAware)
            {
                ((ServiceAware) component).setService(this);
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

    public ObjectFactory getComponentFactory()
    {
        return componentFactory;
    }

    public void setComponentFactory(ObjectFactory componentFactory)
    {
        this.componentFactory = componentFactory;
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

    /**
     * A descriptor can have a custom entrypoint resolver for its own object.
     * By default this is null. When set this resolver will override the resolver on the model
     *
     * @return Null is a resolver set has not been set otherwise the resolver to use
     *         on this service
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
     *                    on this service
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
