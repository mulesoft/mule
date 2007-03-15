/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.MuleException;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.impl.internal.notifications.ServerNotificationManager;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.registry.DeregistrationException;
import org.mule.registry.Registration;
import org.mule.registry.RegistrationException;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.registry.impl.MuleRegistration;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.MapUtils;
import org.mule.util.SpiUtils;
import org.mule.util.UUID;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.queue.TransactionalQueueManager;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.collections.list.CursorableLinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * TODO
 */
public class DefaultRegistryFacade2 extends SpringRegistry

{
    /**
     * Service descriptor cache.
     *
     * @deprecated This needs to be redesigned for an OSGi environment where ServiceDescriptors may change.
     */
    // @GuardedBy("this")
    protected static Map sdCache = new HashMap();


    /**
     * Default configuration
     */
    private MuleConfiguration config = new MuleConfiguration();

    /**
     * the unique id for this manager
     */
    private String id = UUID.getUUID();

    /**
     * The transaction Manager to use for global transactions
     */
    private TransactionManager transactionManager = null;

    /**
     * Collection for transformers registered in this component
     */
    private Map applicationProps = new HashMap();

    /**
     * True once the Mule Manager is initialised
     */
    private AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * True while the Mule Manager is initialising
     */
    private AtomicBoolean initialising = new AtomicBoolean(false);

    /**
     * Determines of the MuleManager has been started
     */
    private AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Determines in the manager is in the process of starting
     */
    private AtomicBoolean starting = new AtomicBoolean(false);

    /**
     * Determines in the manager is in the process of stopping.
     */
    private AtomicBoolean stopping = new AtomicBoolean(false);

    /**
     * Determines if the manager has been disposed
     */
    private AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Manages all Server event notificationManager
     */
    private ServerNotificationManager notificationManager = null;

    private MultiContainerContext containerContext = null;

    private static Log logger = LogFactory.getLog(DefaultRegistryFacade2.class);

    protected UMOManagementContext managementContext;

    protected StaticApplicationContext registryContext;

    /**
     * Default Constructor
     */
    public DefaultRegistryFacade2(ApplicationContext context)
    {
        if (config == null)
        {
            config = new MuleConfiguration();
        }

        StaticApplicationContext ctx = new StaticApplicationContext(context);

        ctx.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor()
                {

            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
                    {
                return bean;
            }

            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
                    {
                if (initialised.get() || initialising.get())
                {
                    if(bean instanceof Initialisable)
                    {
                        try
                        {
                            ((Initialisable)bean).initialise();
                        }
                        catch (InitialisationException e)
                        {
                            throw new BeanCreationException("Failed to initialise Bean: " + e.getMessage(), e);
                        }

                    }
                }
                if ((started.get() || starting.get()))
                {

                    if(bean instanceof Startable)
                    {
                        try
                        {
                            ((Startable)bean).start();
                        }
                        catch (UMOException e)
                        {
                            throw new BeanCreationException("Failed to start Bean: " + e.getMessage(), e);
                        }

                    }
                }
                return bean;
            }
        });

        containerContext = new MultiContainerContext();

        SpringContainerContext registryContainer = new SpringContainerContext();
        registryContainer.setName("registry");
        registryContainer.setExternalBeanFactory(ctx);
        containerContext.addContainer(registryContainer);

    }

    /**
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    public synchronized MuleConfiguration getConfiguration()
    {
        return config;
    }

    /**
     * Sets the configuration for the <code>MuleManager</code>.
     *
     * @param config the configuration object
     * @throws IllegalAccessError if the <code>MuleManager</code> has already been
     *                            initialised.
     * @deprecated this will go away soon.
     */
    public synchronized void setConfiguration(MuleConfiguration config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException(
                    new Message(Messages.X_IS_NULL, "MuleConfiguration object").getMessage());
        }

        this.config = config;
    }

    public synchronized void dispose()
    {
        if (disposed.get())
        {
            return;
        }
        try
        {
            if (started.get())
            {
                stop();
            }
        }
        catch (UMOException e)
        {
            logger.error("Failed to stop manager: " + e.getMessage(), e);
        }
        disposed.set(true);

        disposeObjectsOfType(UMOConnector.class, ManagerNotification.MANAGER_DISPOSING_CONNECTORS,
                ManagerNotification.MANAGER_DISPOSED_CONNECTORS);
        disposeObjectsOfType(UMOModel.class, -1, -1);
        disposeObjectsOfType(UMOAgent.class, -1, -1);

        containerContext.dispose();
        containerContext = null;
        // props.clearErrors();
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSED));

        // props = null;
        initialised.set(false);
        if (notificationManager != null)
        {
            notificationManager.dispose();
        }


        config = new MuleConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(Object key)
    {
        return applicationProps.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map getProperties()
    {
        return applicationProps;
    }

    /**
     * {@inheritDoc}
     */
    public TransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    /**
     * {@inheritDoc}
     */
    public UMOConnector lookupConnector(String name)
    {
        try
        {
            return (UMOConnector) lookupObject(name);
        }
        catch (ObjectNotFoundException e)
        {
            logger.debug(e.getMessage());
            return null;
        }

    }

    /**
     * {@inheritDoc}
     */
    public String lookupEndpointIdentifier(String logicalName, String defaultName)
    {

        return defaultName;
    }

    /**
     * {@inheritDoc}
     */
    public UMOEndpoint lookupEndpoint(String logicalName)
    {
        //This will grab a new prototype from the context
        try
        {
            return (UMOEndpoint) lookupObject(logicalName);
        }
        catch (ObjectNotFoundException e)
        {
            logger.debug(e.getMessage());
            return null;
        }

    }


    /**
     * {@inheritDoc}
     */
    public UMOTransformer lookupTransformer(String name)
    {
        try
        {
            return (UMOTransformer) lookupObject(name);
        }
        catch (ObjectNotFoundException e)
        {
            logger.debug(e.getMessage());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerConnector(UMOConnector connector) throws UMOException
    {
        registerSingleton(connector);
    }

    protected void registerSingleton(Object o) throws UMOException
    {
        try
        {
            Map m = BeanUtils.describe(o);
            MutablePropertyValues mpvs = new MutablePropertyValues(m);
            registryContext.registerSingleton((String)m.get("name"), o.getClass(), mpvs);
        }
        catch (Exception e)
        {
            throw new MuleException(e);
        }
    }

    protected void registerPrototype(Object o) throws UMOException
    {
        try
        {
            Map m = BeanUtils.describe(o);
            MutablePropertyValues mpvs = new MutablePropertyValues(m);
            registryContext.registerPrototype((String)m.get("name"), o.getClass(), mpvs);
        }
        catch (Exception e)
        {
            throw new MuleException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterConnector(String connectorName) throws UMOException
    {
        UMOConnector c = lookupConnector(connectorName);
        if (c != null)
        {
            c.dispose();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void registerEndpoint(UMOEndpoint endpoint) throws UMOException
    {
        registerPrototype(endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpoint(String endpointName)
    {
        UMOEndpoint p = lookupEndpoint(endpointName);
        if (p != null)
        {
            //TODO Kill it
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerTransformer(UMOTransformer transformer) throws UMOException
    {
        registerPrototype(transformer);
        logger.info("Transformer " + transformer.getName() + " has been initialised successfully");
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterTransformer(String transformerName)
    {
        UMOTransformer t = lookupTransformer(transformerName);
        if(t!=null) {
            //TODO Kill it
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(Object key, Object value)
    {
        applicationProps.put(key, value);
    }

    public void addProperties(Map props)
    {
        applicationProps.putAll(props);
    }


    public void initialise() throws InitialisationException
    {
        this.managementContext = managementContext;

        if (!initialised.get())
        {
            initialising.set(true);

            // Fire message notifications if the option is set. This will fire
            // inbound and outbound message events that can
            // consume resources in high throughput systems
            // TODO MERGE no such method?
            //if (config.isEnableMessageEvents())
            //{
            //    notificationManager.registerEventType(MessageNotification.class,
            //            MessageNotificationListener.class);
            //}

            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_INITIALISNG));
            if (id == null)
            {
                logger.warn("No unique id has been set on this manager");
            }
            try
            {
                try
                {
                    TransactionalQueueManager queueMgr = new TransactionalQueueManager();
                    // TODO RM: The persistence strategy should come from the user's config.
                    QueuePersistenceStrategy ps = new CachingPersistenceStrategy(new MemoryPersistenceStrategy()/*config.getPersistenceStrategy()*/);
                    queueMgr.setPersistenceStrategy(ps);
                }
                catch (Exception e)
                {
                    throw new InitialisationException(new Message(Messages.INITIALISATION_FAILURE_X,
                            "QueueManager"), e);
                }

                getContainerContext().initialise();
                initialiseObjectsOfType(UMOConnector.class, -1, -1);
                initialiseObjectsOfType(UMOEndpoint.class, -1, -1);
                initialiseAgents();
                initialiseObjectsOfType(UMOModel.class, -1, -1);

            }
            catch (InitialisationException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
            finally
            {
                initialised.set(true);
                initialising.set(false);
                fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_INITIALISED));
            }
        }
    }


    protected void registerAdminAgent() throws UMOException
    {
        // Allows users to disable all server components and connections
        // this can be useful for testing
        boolean disable = MapUtils.getBooleanValue(System.getProperties(),
                MuleProperties.DISABLE_SERVER_CONNECTIONS_SYSTEM_PROPERTY, false);

        // if endpointUri is blanked out do not setup server components
        //TODO RM* Admin agent should be explicit
//        if (StringUtils.isBlank(config.getServerUrl()))
//        {
//            logger.info("Server endpointUri is null, not registering Mule Admin agent");
//            disable = true;
//        }
//
//        if (disable)
//        {
//            unregisterAgent(MuleAdminAgent.AGENT_NAME);
//        }
//        else
//        {
//            if (lookupAgent(MuleAdminAgent.AGENT_NAME) == null)
//            {
//                registerAgent(new MuleAdminAgent());
//            }
//        }
    }



    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @throws org.mule.umo.UMOException if the the connectors or components fail to start
     */
    public synchronized void start() throws UMOException
    {
        if (!initialised.get())
        {
            throw new IllegalStateException("Not Initialised");
        }

        if (!started.get())
        {
            starting.set(true);
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTING));
            registerAdminAgent();
            startObjectsOfType(UMOConnector.class, -1, -1);
            startObjectsOfType(UMOAgent.class, -1, -1);
            startObjectsOfType(UMOModel.class, ManagerNotification.MANAGER_STARTING_MODELS, ManagerNotification.MANAGER_STARTED_MODELS);

            started.set(true);
            starting.set(false);

            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTED));
        }
    }

    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @param serverUrl the server Url for this instance
     * @throws org.mule.umo.UMOException if the the connectors or components fail to start
     */
    public void start(String serverUrl) throws UMOException
    {
        // this.createClientListener = createRequestListener;
        //TODO RM*
        //config.setServerUrl(serverUrl);
        start();
    }


    /**
     * Stops the <code>MuleManager</code> which stops all sessions and connectors
     *
     * @throws org.mule.umo.UMOException if either any of the sessions or connectors fail to stop
     */
    public synchronized void stop() throws UMOException
    {
        started.set(false);
        stopping.set(true);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPING));

        stopObjectsOfType(UMOConnector.class, -1, -1);
        stopObjectsOfType(UMOAgent.class, -1, -1);
        stopObjectsOfType(UMOModel.class, ManagerNotification.MANAGER_STOPPING_MODELS, ManagerNotification.MANAGER_STOPPED_MODELS);

        stopping.set(false);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED));
    }


    protected void stopObjectsOfType(Class clazz, int preNotifId, int postNotifId) throws UMOException
    {
        if(preNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), preNotifId));
        }
        String objectName = ClassUtils.getClassName(clazz) + "s";
        logger.debug("Stopping " + objectName + "...");
        Map objects = registryContext.getBeansOfType(clazz);
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof Stoppable)
            {
                ((Stoppable)o).stop();
            }
        }
        logger.info(objectName + " have been stopped successfully");
        if(postNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), postNotifId));
        }
    }

    protected void initialiseObjectsOfType(Class clazz, int preNotifId, int postNotifId) throws UMOException
    {
        if(preNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), preNotifId));
        }

        String objectName = ClassUtils.getClassName(clazz) + "s";
        logger.debug("Initialising " + objectName + "...");
        Map objects = registryContext.getBeansOfType(clazz);
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof Initialisable)
            {
                ((Initialisable)o).initialise();
            }
        }
        logger.info(objectName + " have been initialised successfully");
        if(postNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), postNotifId));
        }
    }

    protected void startObjectsOfType(Class clazz, int preNotifId, int postNotifId) throws UMOException
    {
        if(preNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), preNotifId));
        }

        String objectName = ClassUtils.getClassName(clazz) + "s";
        logger.debug("Starting " + objectName + "...");
        Map objects = registryContext.getBeansOfType(clazz);
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof Startable)
            {
                ((Startable)o).start();
            }
        }
        logger.info(objectName + " have been started successfully");
        if(postNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), postNotifId));
        }
    }

    protected void disposeObjectsOfType(Class clazz, int preNotifId, int postNotifId)
    {
        if(preNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), preNotifId));
        }
        String objectName = ClassUtils.getClassName(clazz) + "s";
        logger.debug("Disposing " + objectName + "...");
        Map objects = registryContext.getBeansOfType(clazz);
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if(o instanceof Disposable)
            {
                ((Disposable)o).dispose();
            }
        }
        logger.info(objectName + " have been disposed successfully");
        if(postNotifId != -1)
        {
            fireSystemEvent(new ManagerNotification(id, config.getClusterId(), config.getDomainId(), postNotifId));
        }
    }

    public UMOModel lookupModel(String name)
    {
        try
        {
            return (UMOModel) lookupObject(name);
        }
        catch (ObjectNotFoundException e)
        {
            logger.info(e);
            return null;
        }
    }

    public void registerModel(UMOModel model) throws UMOException
    {
        registerSingleton(model);
    }

    public void unregisterModel(String name)
    {
        UMOModel model = lookupModel(name);
        if (model != null)
        {
            model.dispose();
        }
    }

    public Map getModels()
    {
        return Collections.unmodifiableMap(registryContext.getBeansOfType(UMOModel.class));
    }

    /**
     * {@inheritDoc}
     */
    public Map getConnectors()
    {
        return Collections.unmodifiableMap(registryContext.getBeansOfType(UMOConnector.class));
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpoints()
    {
        return Collections.unmodifiableMap(registryContext.getBeansOfType(UMOImmutableEndpoint.class));
    }

    /**
     * {@inheritDoc}
     */
    public Map getTransformers()
    {
        return Collections.unmodifiableMap(registryContext.getBeansOfType(UMOTransformer.class));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStarted()
    {
        return started.get();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInitialised()
    {
        return initialised.get();
    }

    /**
     * Determines if the server is currently initialising
     *
     * @return true if if the server is currently initialising, false otherwise
     */
    public boolean isInitialising()
    {
        return initialising.get();
    }

    /**
     * Determines in the manager is in the process of stopping.
     */
    public boolean isStopping()
    {
        return stopping.get();
    }

    /**
     * {@inheritDoc}
     */
    public void registerAgent(UMOAgent agent) throws UMOException
    {
        registerSingleton(agent);
    }

    public UMOAgent lookupAgent(String name)
    {
        try
        {
            return (UMOAgent) lookupObject(name);
        }
        catch (ObjectNotFoundException e)
        {
            logger.info(e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public UMOAgent unregisterAgent(String name) throws UMOException
    {

        UMOAgent agent = (UMOAgent) lookupObject(name);
        if (agent != null)
        {
            //TODO AP Is this the wrong way round?
            agent.dispose();
            agent.unregistered();
        }
        return agent;
    }

    /**
     * Initialises all registered agents
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     */
    protected void initialiseAgents() throws InitialisationException
    {
        logger.info("Initialising agents...");

        // Do not iterate over the map directly, as 'complex' agents
        // may spawn extra agents during initialisation. This will
        // cause a ConcurrentModificationException.
        // Use a cursorable iteration, which supports on-the-fly underlying
        // data structure changes.
        Collection agentsSnapshot = registryContext.getBeansOfType(UMOAgent.class).values();
        CursorableLinkedList agentRegistrationQueue = new CursorableLinkedList(agentsSnapshot);
        CursorableLinkedList.Cursor cursor = agentRegistrationQueue.cursor();

        // the actual agent object refs are the same, so we are just
        // providing different views of the same underlying data

        try
        {
            while (cursor.hasNext())
            {
                UMOAgent umoAgent = (UMOAgent) cursor.next();

                int originalSize = agentsSnapshot.size();
                logger.debug("Initialising agent: " + umoAgent.getName());
                umoAgent.initialise();
                // thank you, we are done with you
                cursor.remove();

                // Direct calls to MuleManager.registerAgent() modify the original
                // agents map, re-check if the above agent registered any
                // 'child' agents.
                int newSize = agentsSnapshot.size();
                int delta = newSize - originalSize;
                if (delta > 0)
                {
                    // TODO there's some mess going on in
                    // http://issues.apache.org/jira/browse/COLLECTIONS-219
                    // watch out when upgrading the commons-collections.
                    Collection tail = CollectionUtils.retainAll(agentsSnapshot, agentRegistrationQueue);
                    Collection head = CollectionUtils.subtract(agentsSnapshot, tail);

                    // again, above are only refs, all going back to the original agents map

                    // re-order the queue
                    agentRegistrationQueue.clear();
                    // 'spawned' agents first
                    agentRegistrationQueue.addAll(head);
                    // and the rest
                    agentRegistrationQueue.addAll(tail);

                    // update agents map with a new order in case we want to re-initialise
                    // MuleManager on the fly
                    for (Iterator it = agentRegistrationQueue.iterator(); it.hasNext();)
                    {
                        UMOAgent theAgent = (UMOAgent) it.next();
                        theAgent.initialise();
                    }
                }
            }
        }
        finally
        {
            // close the cursor as per JavaDoc
            cursor.close();
        }
        logger.info("Agents Successfully Initialised");
    }


    /**
     * associates a Dependency Injector container or Jndi container with Mule. This
     * can be used to integrate container managed resources with Mule resources
     *
     * @param container a Container context to use. By default, there is a default
     *                  Mule container <code>MuleContainerContext</code> that will assume
     *                  that the reference key for an oblect is a classname and will try to
     *                  instanciate it.
     */
    public void registerContainerContext(UMOContainerContext container) throws UMOException
    {
        if (container == null)
        {
            if (containerContext != null)
            {
                containerContext.dispose();
            }
            containerContext = new MultiContainerContext();
        }
        else
        {
            containerContext.addContainer(container);
        }
    }

    /**
     * associates a Dependency Injector container with Mule. This can be used to
     * integrate container managed resources with Mule resources
     *
     * @return the container associated with the Manager
     */
    public UMOContainerContext getContainerContext()
    {
        return containerContext;
    }

    /**
     * {@inheritDoc}
     */
    public void registerListener(UMOServerNotificationListener l) throws NotificationException
    {
        registerListener(l, null);
    }

    public void registerListener(UMOServerNotificationListener l, String resourceIdentifier)
            throws NotificationException
    {
        if (notificationManager == null)
        {
            throw new NotificationException(new Message(Messages.SERVER_EVENT_MANAGER_NOT_ENABLED));
        }
        notificationManager.registerListener(l, resourceIdentifier);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterListener(UMOServerNotificationListener l)
    {
        if (notificationManager != null)
        {
            notificationManager.unregisterListener(l);
        }
    }

    /**
     * Looks up the service descriptor from a singleton cache and creates a new one if not found.
     */
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        AbstractServiceDescriptor.Key key = new AbstractServiceDescriptor.Key(name, overrides);
        ServiceDescriptor sd = (ServiceDescriptor) sdCache.get(key);

        synchronized (this)
        {
            if (sd == null)
            {
                sd = createServiceDescriptor(type, name, overrides);

                sdCache.put(key, sd);
            }
        }
        return sd;
    }

    /**
     * @deprecated ServiceDescriptors will be created upon bundle startup for OSGi.
     */
    protected ServiceDescriptor createServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        if (props == null)
        {
            throw new ServiceException(new Message(Messages.FAILED_LOAD_X, type + " " + name));
        }
        return ServiceDescriptorFactory.create(type, name, props, overrides, applicationContext);
    }

    /**
     * Fires a mule 'system' event. These are notifications that are fired because
     * something within the Mule instance happened such as the Model started or the
     * server is being disposed.
     *
     * @param e the event that occurred
     */
    protected void fireSystemEvent(UMOServerNotification e)
    {
        if (notificationManager != null)
        {
            notificationManager.fireEvent(e);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Event Manager is not enabled, ignoring event: " + e);
        }
    }

    /**
     * Fires a server notification to all registered
     * {@link org.mule.impl.internal.notifications.CustomNotificationListener}
     * notificationManager.
     * <p/>
     * TODO RM: This method now duplicates #fireSystemEvent() completely
     *
     * @param notification the notification to fire. This must be of type
     *                     {@link org.mule.impl.internal.notifications.CustomNotification}
     *                     otherwise an exception will be thrown.
     * @throws UnsupportedOperationException if the notification fired is not a
     *                                       {@link org.mule.impl.internal.notifications.CustomNotification}
     */
    public void fireNotification(UMOServerNotification notification)
    {
        // if(notification instanceof CustomNotification) {
        if (notificationManager != null)
        {
            notificationManager.fireEvent(notification);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Event Manager is not enabled, ignoring notification: " + notification);
        }
        // } else {
        // throw new UnsupportedOperationException(new
        // Message(Messages.ONLY_CUSTOM_EVENTS_CAN_BE_FIRED).getMessage());
        // }
    }


    /**
     * New registration method - just pass in the object, the registry
     * will take care of the rest
     */
    public Registration registerMuleObject(Registerable parent, Registerable object) throws RegistrationException
    {
        return new MuleRegistration();
    }

    public void deregisterComponent(String registryId) throws DeregistrationException
    {

    }

    public Object lookupObject(Object key) throws ObjectNotFoundException
    {
        return getContainerContext().getComponent(key);
    }



}
