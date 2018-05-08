/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.events;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.Model;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.ObjectFilter;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transformer.TransformerException;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.QueueProfile;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.module.spring.i18n.SpringMessages;
import org.mule.object.SingletonObjectFactory;
import org.mule.routing.filters.WildcardFilter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.util.ClassUtils;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ResolvableType;

/**
 * <code>MuleEventMulticaster</code> is an implementation of a Spring
 * ApplicationEventMulticaster. This implementation allows Mule event to be sent and
 * received through the Spring ApplicationContext. This allows any Spring bean to
 * receive and send events from any transport that Mule supports such as Jms, Http,
 * Tcp, Pop3, Smtp, File, etc. All a bean needs to do to receive and send events is
 * to implement MuleEventListener. Beans can also have subscriptions to certain
 * events by implementing MuleSubscriptionEventListener, where the bean can provide a
 * list of endpoints on which to receive events i.e. <code>
 * &lt;bean id="myListener" class="com.foo.MyListener"&gt;
 * &lt;property name="subscriptions"&gt;
 * &lt;list&gt;
 * &lt;value&gt;jms://customer.support&lt;/value&gt;
 * &lt;value&gt;pop3://support:123456@mail.mycompany.com&lt;/value&gt;
 * &lt;/list&gt;
 * &lt;/property&gt;
 * &lt;/bean&gt;
 * </code>
 * <p/> Endpoints are specified as a Mule Url which is used to register a listener
 * for the subscription In the previous version of the MuleEventMulticaster it was
 * possible to specify wildcard endpoints. This is still possible but you need to
 * tell the multicaster which specific endpoints to listen on and then your
 * subscription listeners can use wildcards. To register the specific endpoints on
 * the MuleEvent Multicaster you use the <i>subscriptions</i> property. <p/> <code>
 * &lt;bean id="applicationEventMulticaster" class="org.mule.module.spring.events.MuleEventMulticaster"&gt;
 * &lt;property name="subscriptions"&gt;
 * &lt;list&gt;
 * &lt;value&gt;jms://orders.queue&lt;/value&gt;
 * &lt;value&gt;jms://another.orders.queue&lt;/value&gt;
 * &lt;/list&gt;
 * &lt;/property&gt;
 * &lt;/bean&gt;
 * <p/>
 * &lt;bean id="myListener" class="com.foo.MyListener"&gt;
 * &lt;property name="subscriptions"&gt;
 * &lt;list&gt;
 * &lt;value&gt;jms://*.orders.*.&lt;/value&gt;
 * &lt;/list&gt;
 * &lt;/property&gt;
 * &lt;/bean&gt;
 * <p/>
 * </code>
 *
 * @see MuleEventListener
 * @see MuleSubscriptionEventListener
 * @see ApplicationEventMulticaster
 *
 * <b>Deprecated from 3.6.0.</b>
 */
@Deprecated
public class MuleEventMulticaster
    implements ApplicationEventMulticaster, ApplicationContextAware, MuleContextAware, Callable, Initialisable
{
    public static final String EVENT_MULTICASTER_DESCRIPTOR_NAME = "muleEventMulticasterDescriptor";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleEventMulticaster.class);

    /**
     * The set of listeners for this Multicaster
     */
    protected final Set listeners = new CopyOnWriteArraySet();

    /**
     * Determines whether events will be processed asynchronously
     */
    protected boolean asynchronous = false;

    /**
     * An ExecutorService for handling asynchronous events
     */
    protected ExecutorService asyncPool = null;

    /**
     * A list of endpoints the eventMulticaster will receive events on Note that if
     * this eventMulticaster has a Mule Descriptor associated with it, these
     * endpoints are ignored and the ones on the Mule Descriptor are used. These are
     * here for convenience, the event multicaster will use these to create a default
     * MuleDescriptor for itself at runtime
     */
    protected String[] subscriptions = null;

    /**
     * The Spring application context
     */
    protected ApplicationContext applicationContext;

    /**
     * The mule instance component for the Multicaster
     */
    protected Service service;

    /**
     * The filter used to match subscriptions
     */
    protected Class<? extends Filter> subscriptionFilter = WildcardFilter.class;

    /**
     * Used to store parsed endpoints
     */
    protected ExceptionListener exceptionListener = new LoggingExceptionListener();

    protected MuleContext muleContext;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (asynchronous)
        {
            if (asyncPool == null)
            {
                asyncPool = muleContext.getDefaultThreadingProfile().createPool("spring-events");
            }
        }
        else
        {
            if (asyncPool != null)
            {
                asyncPool.shutdown();
                asyncPool = null;
            }
        }
    }

    /**
     * Adds a listener to the the Multicaster. If asynchronous is set to true, an
     * <code>AsynchronousMessageListener</code> is used to wrap the listener. This
     * listener will be initialised with a threadpool. The configuration for the
     * threadpool can be set on this multicaster or inherited from the MuleManager
     * configuration, which is good for most cases.
     *
     * @param listener the ApplicationListener to register with this Multicaster
     * @see AsynchronousEventListener
     * @see ThreadingProfile
     */
    @Override
    public void addApplicationListener(ApplicationListener listener)
    {
        Object listenerToAdd = listener;

        if (asynchronous)
        {
            listenerToAdd = new AsynchronousEventListener(asyncPool, listener);
        }

        listeners.add(listenerToAdd);
    }

    /**
     * Removes a listener from the multicaster
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeApplicationListener(ApplicationListener listener)
    {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            ApplicationListener applicationListener = (ApplicationListener) iterator.next();
            if (applicationListener instanceof AsynchronousEventListener)
            {
                if (((AsynchronousEventListener) applicationListener).getListener().equals(listener))
                {
                    listeners.remove(applicationListener);
                    return;
                }
            }
            else
            {
                if (applicationListener.equals(listener))
                {
                    listeners.remove(applicationListener);
                    return;
                }
            }
        }
        listeners.remove(listener);
    }

    @Override
    public void addApplicationListenerBean(String s)
    {
        Object listener = applicationContext.getBean(s);
        if (listener instanceof ApplicationListener)
        {
            addApplicationListener((ApplicationListener)listener);
        }
        else
        {
            throw new IllegalArgumentException(SpringMessages.beanNotInstanceOfApplicationListener(s)
                .getMessage());
        }
    }

    @Override
    public void removeApplicationListenerBean(String s)
    {
        Object listener = applicationContext.getBean(s);
        if (listener instanceof ApplicationListener)
        {
            removeApplicationListener((ApplicationListener)listener);
        }
        else
        {
            throw new IllegalArgumentException(SpringMessages.beanNotInstanceOfApplicationListener(s)
                .getMessage());
        }
    }

    /**
     * Removes all the listeners from the multicaster
     */
    @Override
    public void removeAllListeners()
    {
        listeners.clear();
    }

    /**
     * Method is used to dispatch events to listeners registered with the
     * EventManager or dispatches events to Mule depending on the type and state of
     * the event received. If the event is not a Mule event it will be dispatched to
     * any listeners registered that are NOT MuleEventListeners. If the event is a
     * Mule event and there is no source event attached to it, it is assumed that the
     * event was dispatched by an object in the context using context.publishEvent()
     * and will be dispatched by Mule. If the event does have a source event attached
     * to it, it is assumed that the event was dispatched by Mule and will be
     * delivered to any listeners subscribed to the event.
     *
     * @param e the application event received by the context
     */
    @Override
    public void multicastEvent(ApplicationEvent e)
    {
        MuleApplicationEvent muleEvent = null;
        // if the context gets refreshed we need to reinitialise
        if (e instanceof ContextRefreshedEvent)
        {
            try
            {
                registerMulticasterComponent();
            }
            catch (MuleException ex)
            {
                throw new MuleRuntimeException(SpringMessages.failedToReinitMule(), ex);
            }
        }
        else if (e instanceof ContextClosedEvent)
        {
            if (!muleContext.isDisposing() && !muleContext.isDisposed())
            {
                muleContext.dispose();
            }
            return;
        }
        else if (e instanceof MuleApplicationEvent)
        {
            muleEvent = (MuleApplicationEvent) e;
            // If there is no Mule event the event didn't originate from Mule
            // so its an outbound event
            if (muleEvent.getMuleEventContext() == null)
            {
                try
                {
                    dispatchEvent(muleEvent);
                }
                catch (ApplicationEventException e1)
                {
                    exceptionListener.exceptionThrown(e1);
                }
                return;
            }
        }

        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            ApplicationListener listener = (ApplicationListener) iterator.next();
            if (muleEvent != null)
            {
                // As the asynchronous listener wraps the real listener we need
                // to check the type of the wrapped listener, but invoke the Async
                // listener
                if (listener instanceof AsynchronousEventListener)
                {
                    AsynchronousEventListener asyncListener = (AsynchronousEventListener) listener;
                    if (asyncListener.getListener() instanceof MuleSubscriptionEventListener)
                    {
                        if (isSubscriptionMatch(muleEvent.getEndpoint(),
                            ((MuleSubscriptionEventListener) asyncListener.getListener()).getSubscriptions()))
                        {
                            asyncListener.onApplicationEvent(muleEvent);
                        }
                    }
                    else if (asyncListener.getListener() instanceof MuleEventListener)
                    {
                        asyncListener.onApplicationEvent(muleEvent);
                    }
                    else if (!(asyncListener.getListener() instanceof MuleEventListener))
                    {
                        asyncListener.onApplicationEvent(e);
                    }
                    // Synchronous MuleEvent listener Checks
                }
                else if (listener instanceof MuleSubscriptionEventListener)
                {
                    if (isSubscriptionMatch(muleEvent.getEndpoint(),
                        ((MuleSubscriptionEventListener) listener).getSubscriptions()))
                    {
                        listener.onApplicationEvent(muleEvent);
                    }
                }
                else if (listener instanceof MuleEventListener)
                {
                    listener.onApplicationEvent(muleEvent);
                }
            }
            else if (listener instanceof AsynchronousEventListener
                     && !(((AsynchronousEventListener) listener).getListener() instanceof MuleEventListener))
            {
                listener.onApplicationEvent(e);
            }
            else if (!(listener instanceof MuleEventListener))
            {
                listener.onApplicationEvent(e);
            }
            else
            {
                // Finally only propagate the Application event if the
                // ApplicationEvent interface is explicitly implemented
                for (int i = 0; i < listener.getClass().getInterfaces().length; i++)
                {
                    if (listener.getClass().getInterfaces()[i].equals(ApplicationListener.class))
                    {
                        listener.onApplicationEvent(e);
                        break;
                    }
                }

            }
        }
    }

    /***
     * Implemented only for compliance with Spring 4.2+
     * Please use the {@link #multicastEvent(ApplicationEvent) multicastEvent} method.
     * @param event
     * @param eventType
     */
    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        multicastEvent(event);
    }

    /**
     * Matches a subscription to the current event endpointUri
     *
     * @param endpoint endpoint
     * @param subscriptions subscriptions
     * @return true if there's a match
     */
    private boolean isSubscriptionMatch(String endpoint, String[] subscriptions)
    {
        for (int i = 0; i < subscriptions.length; i++)
        {
            String subscription = subscriptions[i];

            ObjectFilter filter = createFilter(subscription);
            if (filter.accept(endpoint))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether events will be processed asynchronously
     *
     * @return tru if asynchronous. The default is false
     */
    public boolean isAsynchronous()
    {
        return asynchronous;
    }

    /**
     * Determines whether events will be processed asynchronously
     *
     * @param asynchronous true if asynchronous
     */
    public void setAsynchronous(boolean asynchronous)
    {
        this.asynchronous = asynchronous;
    }

    /**
     * This is the callback method used by Mule to give Mule events to this
     * Multicaster
     *
     * @param context the context received by Mule
     */
    @Override
    public Object onCall(MuleEventContext context) throws TransformerException, MalformedEndpointException
    {
        multicastEvent(new MuleApplicationEvent(context.getMessage().getPayload(), context, applicationContext));
        context.setStopFurtherProcessing(true);
        return null;
    }

    /**
     * Will dispatch an application event through Mule
     *
     * @param applicationEvent the Spring event to be dispatched
     * @throws ApplicationEventException if the event cannot be dispatched i.e. if
     *             the underlying transport throws an exception
     */
    protected void dispatchEvent(MuleApplicationEvent applicationEvent) throws ApplicationEventException
    {
        OutboundEndpoint endpoint;
        try
        {
            endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
                applicationEvent.getEndpoint());
        }
        catch (MuleException e)
        {
            throw new ApplicationEventException("Failed to get endpoint for endpointUri: "
                                                + applicationEvent.getEndpoint(), e);
        }
        if (endpoint != null)
        {
            try
            {
                DefaultMuleMessage message = new DefaultMuleMessage(applicationEvent.getSource(),
                    applicationEvent.getProperties(), muleContext);
                // has dispatch been triggered using beanFactory.publish()
                // without a current event
                if (applicationEvent.getMuleEventContext() != null)
                {
                    // tell mule not to try and route this event itself
                    applicationEvent.getMuleEventContext().setStopFurtherProcessing(true);
                    applicationEvent.getMuleEventContext().dispatchEvent(message, endpoint);
                }
                else
                {
                    DefaultMuleEvent event = new DefaultMuleEvent(message, endpoint.getExchangePattern(),
                        service);
                    RequestContext.setEvent(event);
                    // transform if necessary
                    if (endpoint.getTransformers() != null)
                    {
                        message = new DefaultMuleMessage(applicationEvent.getSource(),
                            applicationEvent.getProperties(), muleContext);
                        message.applyTransformers(event, endpoint.getTransformers());
                    }
                    endpoint.process(new DefaultMuleEvent(message, endpoint.getExchangePattern(), service));
                }
            }
            catch (Exception e1)
            {
                throw new ApplicationEventException("Failed to dispatch event: " + e1.getMessage(), e1);
            }
        }
        else
        {
            throw new ApplicationEventException("Failed endpoint using name: "
                                                + applicationEvent.getEndpoint());
        }
    }

    /**
     * Set the current Spring application context
     *
     * @param applicationContext application context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    protected void registerMulticasterComponent() throws MuleException
    {
        // A descriptor hasn't been explicitly configured, so create a default
        if (service == null)
        {
            service = getDefaultService();
            setSubscriptionsOnService(service);
            muleContext.getRegistry().registerService(service);
        }
    }

    protected void setSubscriptionsOnService(Service service) throws MuleException
    {
        List<String> endpoints = new ArrayList<String>();
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            ApplicationListener listener = (ApplicationListener) iterator.next();
            if (listener instanceof AsynchronousEventListener)
            {
                listener = ((AsynchronousEventListener) listener).getListener();
            }
            if (listener instanceof MuleSubscriptionEventListener)
            {
                String[] subscriptions = ((MuleSubscriptionEventListener) listener).getSubscriptions();
                for (int i = 0; i < subscriptions.length; i++)
                {
                    if (subscriptions[i].indexOf("*") == -1 && MuleEndpointURI.isMuleUri(subscriptions[i]))
                    {
                        boolean isSoap = registerAsSoap(subscriptions[i], listener);

                        if (!isSoap)
                        {
                            endpoints.add(subscriptions[i]);
                        }
                    }
                }
            }
        }
        if (endpoints.size() > 0)
        {
            for (String endpoint : endpoints)
            {
                InboundEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(endpoint);

                // check whether the endpoint has already been set on the
                // MuleEventMulticaster
                if (((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoint(ep.getName()) == null)
                {
                    ((ServiceCompositeMessageSource) service.getMessageSource()).addSource(ep);
                }
            }
        }
    }

    private boolean registerAsSoap(String endpoint, Object listener) throws MuleException
    {
        if (endpoint.startsWith("soap") || endpoint.startsWith("axis") || endpoint.startsWith("cxf"))
        {
            EndpointURI ep = new MuleEndpointURI(endpoint, muleContext);

            // get the service name from the URI path
            String serviceName = null;
            if (ep.getPath() != null)
            {
                String path = ep.getPath();
                if (path.endsWith("/"))
                {
                    path = path.substring(0, path.length() - 1);
                }
                int i = path.lastIndexOf("/");
                if (i > -1)
                {
                    serviceName = path.substring(i + 1);
                }
            }
            else
            {
                serviceName = service.getName();
            }
            // now strip off the service name
            String newEndpoint = endpoint;
            int i = newEndpoint.indexOf(serviceName);
            newEndpoint = newEndpoint.substring(0, i - 1);
            SedaService s = new SedaService(muleContext);
            s.setName(serviceName);
            s.setModel(muleContext.getRegistry().lookupSystemModel());

            QueueProfile queueProfile = QueueProfile.newInstancePersistingToDefaultMemoryQueueStore(muleContext);
            s.setQueueProfile(queueProfile);

            ((CompositeMessageSource) s.getMessageSource()).addSource(
                muleContext.getEndpointFactory().getInboundEndpoint(newEndpoint));
            final DefaultJavaComponent component = new DefaultJavaComponent(new SingletonObjectFactory(listener));
            component.setMuleContext(muleContext);
            s.setComponent(component);
            muleContext.getRegistry().registerService(s);
            return true;
        }
        else
        {
            return false;
        }
    }

    protected Service getDefaultService() throws MuleException
    {
        // When the the beanFactory is refreshed all the beans get
        // reloaded so we need to unregister the service from Mule
        Model model = muleContext.getRegistry().lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
        if (model == null)
        {
            model = new SedaModel();
            model.setName(MuleProperties.OBJECT_SYSTEM_MODEL);
            muleContext.getRegistry().registerModel(model);
        }
        Service service = muleContext.getRegistry().lookupService(EVENT_MULTICASTER_DESCRIPTOR_NAME);
        if (service != null)
        {
            muleContext.getRegistry().unregisterService(service.getName());
        }
        service = new SedaService(muleContext);
        service.setName(EVENT_MULTICASTER_DESCRIPTOR_NAME);
        service.setModel(model);
        if (subscriptions == null)
        {
            logger.info("No receive endpoints have been set, using default '*'");
            ((CompositeMessageSource) service.getMessageSource()).addSource(
                muleContext.getEndpointFactory().getInboundEndpoint("vm://*"));
        }
        else
        {
            // Set multiple inbound subscriptions on the descriptor
            ServiceCompositeMessageSource messageRouter = (ServiceCompositeMessageSource) service.getMessageSource();

            for (int i = 0; i < subscriptions.length; i++)
            {
                String subscription = subscriptions[i];

                EndpointFactory endpointFactory = muleContext.getEndpointFactory();
                EndpointBuilder endpointBuilder = endpointFactory.getEndpointBuilder(subscription);
                endpointBuilder.setExchangePattern(MessageExchangePattern.fromSyncFlag(!asynchronous));
                InboundEndpoint endpoint = endpointFactory.getInboundEndpoint(endpointBuilder);

                messageRouter.addSource(endpoint);
            }
        }
        DefaultJavaComponent component = new DefaultJavaComponent(new SingletonObjectFactory(this));
        component.setMuleContext(muleContext);
        service.setComponent(component);
        return service;
    }

    protected ObjectFilter createFilter(String pattern)
    {
        try
        {
            if (getSubscriptionFilter() == null)
            {
                setSubscriptionFilter(WildcardFilter.class);
            }
            return (ObjectFilter) ClassUtils.instanciateClass(getSubscriptionFilter(), pattern);
        }
        catch (Exception e)
        {
            exceptionListener.exceptionThrown(e);
            return new WildcardFilter(pattern);
        }
    }

    /**
     * the type of filter used to filter subscriptions
     *
     * @return the class of the filter to use. The default is WildcardFilter
     * @see WildcardFilter
     */
    public Class<? extends Filter> getSubscriptionFilter()
    {
        return subscriptionFilter;
    }

    /**
     * sets the type of filter used to filter subscriptions
     *
     * @param subscriptionFilter the class of the filter to use.
     */
    public void setSubscriptionFilter(Class<? extends Filter> subscriptionFilter)
    {
        this.subscriptionFilter = subscriptionFilter;
    }

    /**
     * A list of endpoints the eventMulticaster will receive events on Note that if
     * this eventMulticaster has a Mule Descriptor associated with it, these
     * endpoints are ignored and the ones on the Mule Descriptor are used. These are
     * here for convenience, the event multicaster will use these to create a default
     * MuleDescriptor for itself at runtime
     *
     * @return endpoints List being listened on
     */
    public String[] getSubscriptions()
    {
        return subscriptions;
    }

    /**
     * A list of endpoints the eventMulticaster will receive events on Note that if
     * this eventMulticaster has a Mule Descriptor associated with it, these
     * endpoints are ignored and the ones on the Mule Descriptor are used. These are
     * here for convenience, the event multicaster will use these to create a default
     * MuleDescriptor for itself at runtime
     *
     * @param subscriptions a list of endpoints to listen on
     */
    public void setSubscriptions(String[] subscriptions)
    {
        this.subscriptions = subscriptions;
    }

    protected void setExceptionListener(ExceptionListener listener)
    {
        if (listener != null)
        {
            this.exceptionListener = listener;
        }
        else
        {
            throw new IllegalArgumentException("exceptionListener may not be null");
        }
    }

    private static class LoggingExceptionListener implements ExceptionListener
    {
        public LoggingExceptionListener()
        {
            super();
        }

        @Override
        public void exceptionThrown(Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}
