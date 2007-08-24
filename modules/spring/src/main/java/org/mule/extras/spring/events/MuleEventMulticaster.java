/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

import org.mule.MuleException;
import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.extras.spring.i18n.SpringMessages;
import org.mule.impl.ManagementContextAware;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.seda.SedaModel;
import org.mule.providers.AbstractConnector;
import org.mule.routing.filters.ObjectFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.object.SimpleObjectFactory;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArraySet;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * <code>MuleEventMulticaster</code> is an implementation of a Spring
 * ApplicationeventMulticaster. This implementation allows Mule event to be sent and
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
 * the Event Multicaster you use the <i>subscriptions</i> property. <p/> <code>
 * &lt;bean id="applicationEventMulticaster" class="org.mule.extras.spring.events.MuleEventMulticaster"&gt;
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
 * @deprecated Does this class still make any sense for Mule 2.x?
 */
public class MuleEventMulticaster implements ApplicationEventMulticaster, ApplicationContextAware, ManagementContextAware
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
     * Any logical endpointUri mappings to register with mule. These allow for
     * friendly names to be used in place of urls i.e. email-orders ->
     * smtp://orders:password@restaurant.com
     */
    protected Map endpointMappings = null;

    /**
     * A list of endpoints the eventMulticaster will receive events on Note that if
     * this eventMulticaster has a Mule Descriptor associated with it, these
     * endpoints are ignored and the ones on the Mule Descriptor are used. These are
     * here for convenience, the event multicaster will use these to create a default
     * MuleDescriptor for itself at runtime
     */
    protected String[] subscriptions = null;

    /**
     * The Spring acpplication context
     */
    protected ApplicationContext applicationContext;

    /**
     * The Mule descriptor that belongs to this component instnace in Mule
     */
    protected UMODescriptor descriptor;

    /**
     * The mule instance compoennt for the Multicaster
     */
    protected UMOComponent component;

    /**
     * The filter used to match subscriptions
     */
    protected Class subscriptionFilter = WildcardFilter.class;

    /**
     * Used to store parsed endpoints
     */
    protected ExceptionListener exceptionListener = new LoggingExceptionListener();

    protected UMOManagementContext managementContext;


    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
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
    public void removeApplicationListener(ApplicationListener listener)
    {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            ApplicationListener applicationListener = (ApplicationListener)iterator.next();
            if (applicationListener instanceof AsynchronousEventListener)
            {
                if (((AsynchronousEventListener)applicationListener).getListener().equals(listener))
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

    /**
     * Removes all the listeners from the multicaster
     */
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
    public void multicastEvent(ApplicationEvent e)
    {
        MuleApplicationEvent muleEvent = null;
        // if the context gets refreshed we need to reinitialise
        if (e instanceof ContextRefreshedEvent)
        {
            // If the manager is being initialised from another context
            // don't try and initialise Mule
            if (managementContext!=null && !managementContext.isInitialised())
            {
                try
                {
                    registerMulticasterDescriptor();
                }
                catch (UMOException ex)
                {
                    throw new MuleRuntimeException(SpringMessages.failedToReinitMule(), ex);
                }
            }
            else
            {
                initMule();
            }
        }
        else if (e instanceof ContextClosedEvent)
        {
            managementContext.dispose();
            return;
        }
        else if (e instanceof MuleApplicationEvent)
        {
            muleEvent = (MuleApplicationEvent)e;
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
            ApplicationListener listener = (ApplicationListener)iterator.next();
            if (muleEvent != null)
            {
                // As the asynchronous listener wraps the real listener we need to
                // check the
                // type of the wrapped listener, but invoke the Async listener
                if (listener instanceof AsynchronousEventListener)
                {
                    AsynchronousEventListener asyncListener = (AsynchronousEventListener)listener;
                    if (asyncListener.getListener() instanceof MuleSubscriptionEventListener)
                    {
                        if (isSubscriptionMatch(muleEvent.getEndpoint(),
                            ((MuleSubscriptionEventListener)asyncListener.getListener()).getSubscriptions()))
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
                    // Synchronous Event listener Checks
                }
                else if (listener instanceof MuleSubscriptionEventListener)
                {
                    if (isSubscriptionMatch(muleEvent.getEndpoint(),
                        ((MuleSubscriptionEventListener)listener).getSubscriptions()))
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
                     && !(((AsynchronousEventListener)listener).getListener() instanceof MuleEventListener))
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

            // Subscriptions can be full Mule Urls or resource specific such as
            // my.queue
            // if it is a MuleEndpointURI we need to extract the Resource
            // specific part
            // if (MuleEndpointURI.isMuleUri(subscription)) {
            // UMOEndpointURI ep = (UMOEndpointURI) endpointsCache.get(subscription);
            // if (ep == null) {
            // try {
            // ep = new MuleEndpointURI(subscription);
            // } catch (MalformedEndpointException e) {
            // throw new IllegalArgumentException(e.getMessage());
            // }
            // endpointsCache.put(subscription, ep);
            // }
            // subscription = ep.getAddress();
            // }

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
     * @param asynchronous true if aysnchronous
     */
    public void setAsynchronous(boolean asynchronous)
    {
        this.asynchronous = asynchronous;
        if (asynchronous)
        {
            if (asyncPool == null)
            {
                asyncPool = RegistryContext.getConfiguration().getDefaultThreadingProfile().createPool(
                    "spring-events");
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
     * This is the callback method used by Mule to give Mule events to this
     * Multicaster
     * 
     * @param context the context received by Mule
     */
    public void onMuleEvent(UMOEventContext context) throws TransformerException, MalformedEndpointException
    {
        multicastEvent(new MuleApplicationEvent(context.getTransformedMessage(), context, applicationContext));
        context.setStopFurtherProcessing(true);
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
        UMOEndpoint endpoint;
        try
        {
            endpoint = managementContext.getRegistry().getOrCreateEndpointForUri(applicationEvent.getEndpoint(),
                UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        catch (UMOException e)
        {
            throw new ApplicationEventException("Failed to get endpoint for endpointUri: "
                                                + applicationEvent.getEndpoint(), e);
        }
        if (endpoint != null)
        {
            try
            {
                // if (applicationEvent.getEndpoint() != null) {
                // endpoint.setEndpointURI(applicationEvent.getEndpoint());
                // }

                MuleMessage message = new MuleMessage(applicationEvent.getSource(),
                    applicationEvent.getProperties());
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
                    UMOSession session = new MuleSession(message,
                        ((AbstractConnector)endpoint.getConnector()).getSessionHandler(), component);
                    RequestContext.setEvent(new MuleEvent(message, endpoint, session, false));
                    // transform if necessary
                    if (endpoint.getTransformer() != null)
                    {
                        message = new MuleMessage(endpoint.getTransformer().transform(
                            applicationEvent.getSource()), applicationEvent.getProperties());
                    }
                    endpoint.dispatch(new MuleEvent(message, endpoint, session, false));
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    protected void initMule()
    {
        try
        {
            // endpointsCache.clearErrors();
            // See if there has been a discriptor explicitly configured
            if (applicationContext.containsBean(EVENT_MULTICASTER_DESCRIPTOR_NAME))
            {
                descriptor = (UMODescriptor)applicationContext.getBean(EVENT_MULTICASTER_DESCRIPTOR_NAME);
            }
            // If the mule manager has been initialised in the contain
            // there is not need to do anything here
            if (applicationContext.containsBean("muleManager"))
            {
                // Register the multicaster descriptor
                registerMulticasterDescriptor();
                return;
            }

            if (!managementContext.isStarted())
            {
                RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(!asynchronous);
                // register any endpointUri mappings
                registerEndpointMappings();
            }
            // tell mule to load component definitions from spring
            //SpringContainerContext containerContext = new SpringContainerContext();
            //containerContext.setBeanFactory(applicationContext);

            // see if there are any UMOConnectors to register
            registerConnectors();

            // Next see if there are any UMOTransformers to register
            registerTransformers();

            registerGlobalEndpoints();

            // Register the multicaster descriptor
            registerMulticasterDescriptor();

            if (!managementContext.isStarted())
            {
               managementContext.start();
            }
        }
        catch (UMOException e)
        {
            throw new MuleRuntimeException(SpringMessages.failedToReinitMule(), e);
        }
    }

    protected void registerMulticasterDescriptor() throws UMOException
    {
        // A discriptor hasn't been explicitly configured, so create a default
        if (descriptor == null)
        {
            descriptor = getDefaultDescriptor();
            setSubscriptionsOnDescriptor((MuleDescriptor)descriptor);
            descriptor.setModelName(MuleProperties.OBJECT_SYSTEM_MODEL);
            managementContext.getRegistry().registerService(descriptor);
            UMOModel model = managementContext.getRegistry().lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
            component = model.getComponent(descriptor.getName());
        }
    }

    protected void setSubscriptionsOnDescriptor(MuleDescriptor descriptor) throws UMOException
    {
        String[] subscriptions;
        List endpoints = new ArrayList();
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            ApplicationListener listener = (ApplicationListener)iterator.next();
            if (listener instanceof AsynchronousEventListener)
            {
                listener = ((AsynchronousEventListener)listener).getListener();
            }
            if (listener instanceof MuleSubscriptionEventListener)
            {
                subscriptions = ((MuleSubscriptionEventListener)listener).getSubscriptions();
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
            String endpoint;
            for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
            {
                endpoint = (String) iterator.next();
                MuleEndpoint ep = new MuleEndpoint(endpoint, true);

                // check whether the endpoint has already been set on the MuleEventMulticastor
                if (descriptor.getInboundRouter().getEndpoint(ep.getName()) == null)
                {
                    descriptor.getInboundRouter().addEndpoint(ep);
                }
            }
        }
    }

    private boolean registerAsSoap(String endpoint, Object listener) throws UMOException
    {
        throw new MuleException("Need to reimplement this method without using the QuickConfigurationBuilder.");
//        if (endpoint.startsWith("glue") || endpoint.startsWith("soap") || endpoint.startsWith("axis") || endpoint.startsWith("xfire"))
//        {
//            UMOEndpointURI ep = new MuleEndpointURI(endpoint);
//            QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
//
//            // get the service name from the URI path
//            String serviceName = null;
//            if (ep.getPath() != null)
//            {
//                String path = ep.getPath();
//                if (path.endsWith("/"))
//                {
//                    path = path.substring(0, path.length() - 1);
//                }
//                int i = path.lastIndexOf("/");
//                if (i > -1)
//                {
//                    serviceName = path.substring(i + 1);
//                }
//            }
//            else
//            {
//                serviceName = descriptor.getName();
//            }
//            // now strip off the service name
//            String newEndpoint = endpoint;
//            int i = newEndpoint.indexOf(serviceName);
//            newEndpoint = newEndpoint.substring(0, i - 1);
//            builder.registerComponentInstance(listener, serviceName, new MuleEndpointURI(newEndpoint));
//            return true;
//        }
//        else
//        {
//            return false;
//        }
    }

    protected void registerEndpointMappings() throws UMOException
    {
        // register any endpointUri mappings
        if (endpointMappings != null)
        {
            Map.Entry entry;
            for (Iterator iterator = endpointMappings.entrySet().iterator(); iterator.hasNext();)
            {
                entry = (Map.Entry)iterator.next();
                managementContext.getRegistry().registerEndpoint(new MuleEndpoint((String)entry.getKey(), true));
            }
        }
    }

    protected void registerConnectors() throws UMOException
    {
        if (!managementContext.isInitialised())
        {
            // Next see if there are any UMOConnectors to register
            Map connectors = applicationContext.getBeansOfType(UMOConnector.class, true, true);
            if (connectors.size() > 0)
            {
                Map.Entry entry;
                UMOConnector c;
                for (Iterator iterator = connectors.entrySet().iterator(); iterator.hasNext();)
                {
                    entry = (Map.Entry)iterator.next();
                    c = (UMOConnector)entry.getValue();
                    if (c.getName() == null)
                    {
                        c.setName(entry.getKey().toString());
                    }
                    managementContext.getRegistry().registerConnector(c);
                }
            }
        }
    }

    protected void registerGlobalEndpoints() throws UMOException
    {
        if (!managementContext.isInitialised())
        {
            // Next see if there are any UMOConnectors to register
            Map endpoints = applicationContext.getBeansOfType(UMOEndpoint.class, true, true);
            if (endpoints.size() > 0)
            {
                Map.Entry entry;
                UMOEndpoint endpoint;
                for (Iterator iterator = endpoints.entrySet().iterator(); iterator.hasNext();)
                {
                    entry = (Map.Entry)iterator.next();
                    endpoint = (UMOEndpoint)entry.getValue();
                    if (endpoint.getName() == null)
                    {
                        endpoint.setName(entry.getKey().toString());
                    }
                    managementContext.getRegistry().registerEndpoint(endpoint);
                }
            }
        }
    }

    protected void registerTransformers() throws UMOException
    {
        if (!managementContext.isInitialised())
        {
            // Next see if there are any UMOConnectors to register
            Map transformers = applicationContext.getBeansOfType(UMOTransformer.class, true, true);
            if (transformers.size() > 0)
            {
                Map.Entry entry;
                UMOTransformer t;
                for (Iterator iterator = transformers.entrySet().iterator(); iterator.hasNext();)
                {
                    entry = (Map.Entry)iterator.next();
                    t = (UMOTransformer)entry.getValue();
                    if (t.getName() == null)
                    {
                        t.setName(entry.getKey().toString());
                    }
                    managementContext.getRegistry().registerTransformer(t);
                }
            }
        }
    }

    protected UMODescriptor getDefaultDescriptor() throws UMOException
    {
        // When the the beanFactory is refreshed all the beans get
        // reloaded so we need to unregister the component from Mule
        UMOModel model = managementContext.getRegistry().lookupModel(MuleProperties.OBJECT_SYSTEM_MODEL);
        if(model==null)
        {
            model = new SedaModel();
            model.setName(MuleProperties.OBJECT_SYSTEM_MODEL);
            managementContext.getRegistry().registerModel(model);
        }
        UMODescriptor descriptor = managementContext.getRegistry().lookupService(EVENT_MULTICASTER_DESCRIPTOR_NAME);
        if (descriptor != null)
        {
            model.unregisterComponent(descriptor);
        }
        descriptor = new MuleDescriptor(EVENT_MULTICASTER_DESCRIPTOR_NAME);
        if (subscriptions == null)
        {
            logger.info("No receive endpoints have been set, using default '*'");
            descriptor.getInboundRouter().addEndpoint(new MuleEndpoint("vm://*", true));
        }
        else
        {
            // Set multiple inbound subscriptions on the descriptor
            UMOInboundRouterCollection messageRouter = descriptor.getInboundRouter();

            for (int i = 0; i < subscriptions.length; i++)
            {
                String subscription = subscriptions[i];
                UMOEndpointURI endpointUri = new MuleEndpointURI(subscription);
                UMOEndpoint endpoint = managementContext.getRegistry().getOrCreateEndpointForUri(endpointUri,
                    UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
                if (!asynchronous)
                {
                    endpoint.setSynchronous(true);
                }
                messageRouter.addEndpoint(endpoint);
            }
        }
        // TODO RM: Need to put the actual instance here (not just the name reference), 
        // please review whether the following is correct?
        descriptor.setServiceFactory(new SimpleObjectFactory(MuleEventMulticaster.class));
        return descriptor;
    }

    protected ObjectFilter createFilter(String pattern)
    {
        try
        {
            if (getSubscriptionFilter() == null)
            {
                setSubscriptionFilter(WildcardFilter.class);
            }
            ObjectFilter filter = (ObjectFilter)ClassUtils.instanciateClass(getSubscriptionFilter(),
                new Object[] {pattern});
            return filter;
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
    public Class getSubscriptionFilter()
    {
        return subscriptionFilter;
    }

    /**
     * sets the type of filter used to filter subscriptions
     * 
     * @param subscriptionFilter the class of the filter to use.
     */
    public void setSubscriptionFilter(Class subscriptionFilter)
    {
        this.subscriptionFilter = subscriptionFilter;
    }

    /**
     * Any logical endpointUri mappings to register with mule. These allow for
     * friendly names to be used in place of urls i.e. email-orders ->
     * smtp://orders:password@restaurant.com
     * 
     * @return endpointMappings a map of logical names and endpoiut url strings
     */
    public Map getEndpointMappings()
    {
        return endpointMappings;
    }

    /**
     * Any logical endpointUri mappings to register with mule. These allow for
     * friendly names to be used in place of urls i.e. email-orders ->
     * smtp://orders:password@restaurant.com
     * 
     * @param endpointMappings a map of logical names and endpoiut url strings
     */
    public void setEndpointMappings(Map endpointMappings)
    {
        this.endpointMappings = endpointMappings;
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
     * @param subscriptions a list of enpoints to listen on
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

    private class LoggingExceptionListener implements ExceptionListener
    {
        public void exceptionThrown(Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}
