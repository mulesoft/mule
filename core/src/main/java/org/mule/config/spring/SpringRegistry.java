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

import org.mule.config.ConfigurationException;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.providers.service.TransportFactory;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.registry.DeregistrationException;
import org.mule.registry.Registration;
import org.mule.registry.RegistrationException;
import org.mule.registry.RegistryStore;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.registry.impl.MuleRegistration;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MapUtils;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.queue.TransactionalQueueManager;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * TODO
 */
public class SpringRegistry implements RegistryFacade, ApplicationContextAware

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
    //  private MuleConfiguration config = new MuleConfiguration();

    /**
     * Connectors registry
     */
    // private Map connectors = new HashMap();

    /**
     * Holds any application scoped environment properties set in the config
     */
    private Map applicationProps = new HashMap();

    /**
     * Holds any registered agents
     */
    //private Map agents = new LinkedHashMap();

    /**
     * Holds a list of global endpoints accessible to any client code
     */
    //private Map endpoints = new HashMap();

    /**
     * The model being used
     */
    //private Map models = new LinkedHashMap();

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
    //private Map transformers = new HashMap();

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


    private MultiContainerContext containerContext = null;

    private static Log logger = LogFactory.getLog(SpringRegistry.class);

    protected UMOManagementContext managementContext;

    protected ApplicationContext applicationContext;


    public SpringRegistry(ApplicationContext applicationContext)
    {
        //Default to using the defaultContext in the constructor to avoid NPEs
        //When setManagementContext is called, this reference will be overwritten
        //And the parent will be set to the defaultCotext
        setApplicationContext(applicationContext);
    }

    /**
     * Default Constructor
     */
    public SpringRegistry()
    {
        //defaultContext = new ClassPathXmlApplicationContext("default-mule-config.xml");

        //Default to using the defaultContext in the constructor to avoid NPEs
        //When setManagementContext is called, this reference will be overwritten
        //And the parent will be set to the defaultCotext
        //this.applicationContext  = defaultContext;

    }

    /**
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    public synchronized MuleConfiguration getConfiguration()
    {
        Map temp = applicationContext.getBeansOfType(MuleConfiguration.class, true, false);
        if (temp.size() > 0)
        {
            return (MuleConfiguration) temp.values().toArray()[temp.size()-1];
        }
        return null;
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

        //this.config = config;
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
        disposeConnectors();

        for (Iterator i = getModels().values().iterator(); i.hasNext();)
        {
            UMOModel model = (UMOModel) i.next();
            model.dispose();
        }

        disposeAgents();

        //transformers.clear();
        //endpoints.clear();
        //models.clear();
        containerContext.dispose();
        containerContext = null;
        // props.clearErrors();
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSED));

        //transformers = null;
        //endpoints = null;
        // props = null;
        initialised.set(false);

        //config = new MuleConfiguration();
    }

    /**
     * Destroys all connectors
     */
    private synchronized void disposeConnectors()
    {
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSING_CONNECTORS));
        for (Iterator iterator = getConnectors().values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.dispose();
        }
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSED_CONNECTORS));
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
            return null;
        }
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
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerConnector(UMOConnector connector) throws UMOException
    {
//        connectors.put(connector.getName(), connector);
//        if (initialised.get() || initialising.get())
//        {
//            connector.initialise();
//        }
        if ((started.get() || starting.get()) && !connector.isStarted())
        {
            connector.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterConnector(String connectorName) throws UMOException
    {
//        UMOConnector c = (UMOConnector) connectors.remove(connectorName);
//        if (c != null)
//        {
//            c.dispose();
//        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerEndpoint(UMOEndpoint endpoint) throws UMOException
    {
        //endpoints.put(endpoint.getName(), endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpoint(String endpointName)
    {
//        UMOEndpoint p = (UMOEndpoint) endpoints.get(endpointName);
//        if (p != null)
//        {
//            endpoints.remove(p);
//        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerTransformer(UMOTransformer transformer) throws UMOException
    {
        //transformer.initialise();

        // For now at least, we don't want a registration error to affect
        // the initialisation process.
        // try
        // {
        //TODO LM: Method not implemented yet?
        //transformer.register();
        // }
        // catch (RegistrationException re)
        // {
        //     logger.warn(re);
        // }

//        transformers.put(transformer.getName(), transformer);
//        logger.info("Transformer " + transformer.getName() + " has been initialised successfully");
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterTransformer(String transformerName)
    {
        //transformers.remove(transformerName);
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

    /**
     * {@inheritDoc}
     */
    public void setTransactionManager(TransactionManager newManager) throws UMOException
    {
        if (transactionManager != null)
        {
            throw new ConfigurationException(new Message(Messages.TX_MANAGER_ALREADY_SET));
        }
        transactionManager = newManager;
    }


    public void initialise() throws InitialisationException
    {
        if (!initialised.get())
        {
            initialising.set(true);

            containerContext = new MultiContainerContext();

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

//                getContainerContext().initialise();
//                initialiseConnectors();
//                initialiseEndpoints();
//                initialiseAgents();
//                for (Iterator i = getModels().values().iterator(); i.hasNext();)
//                {
//                    UMOModel model = (UMOModel) i.next();
//                    model.initialise();
//                    //TODO LM: Should the model be registered before or after initialisation?
//                    model.register();
//                }

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

//    protected void initialiseEndpoints() throws InitialisationException
//    {
//        UMOEndpoint ep;
//        for (Iterator iterator = this.endpoints.values().iterator(); iterator.hasNext();)
//        {
//            ep = (UMOEndpoint) iterator.next();
//            ep.initialise();
//            // the connector has been created for this endpoint so lets
//            // set the create connector to 0 so that every time this endpoint
//            // is referenced we don't create another connector
//            ep.setCreateConnector(0);
//        }
//    }

    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @throws UMOException if the the connectors or components fail to start
     */
    public synchronized void start() throws UMOException
    {
//        if (!initialised.get())
//        {
//            throw new IllegalStateException("Not Initialised");
//        }

        if (!started.get())
        {
            starting.set(true);
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTING));
            registerAdminAgent();
            startConnectors();
            startAgents();
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTING_MODELS));
            for (Iterator i = getModels().values().iterator(); i.hasNext();)
            {
                UMOModel model = (UMOModel) i.next();
                model.start();
            }
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTED_MODELS));

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
     * @throws UMOException if the the connectors or components fail to start
     */
    public void start(String serverUrl) throws UMOException
    {
        // this.createClientListener = createRequestListener;
        //TODO RM*
        //config.setServerUrl(serverUrl);
        start();
    }

    /**
     * Starts the connectors
     *
     * @throws org.mule.MuleException if the connectors fail to start
     */
    private void startConnectors() throws UMOException
    {
        for (Iterator iterator = getConnectors().values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.start();
        }
        logger.info("Connectors have been started successfully");
    }

//    private void initialiseConnectors() throws InitialisationException
//    {
//        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
//        {
//            UMOConnector c = (UMOConnector) iterator.next();
//            c.initialise();
//        }
//        logger.info("Connectors have been initialised successfully");
//    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and connectors
     *
     * @throws UMOException if either any of the sessions or connectors fail to stop
     */
    public synchronized void stop() throws UMOException
    {
        started.set(false);
        stopping.set(true);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPING));

        stopConnectors();
        stopAgents();

        logger.debug("Stopping model...");
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPING_MODELS));
        for (Iterator i = getModels().values().iterator(); i.hasNext();)
        {
            UMOModel model = (UMOModel) i.next();
            model.stop();
        }
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED_MODELS));

        stopping.set(false);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED));
    }

    /**
     * Stops the connectors
     *
     * @throws org.mule.MuleException if any of the connectors fail to stop
     */
    private void stopConnectors() throws UMOException
    {
        logger.debug("Stopping connectors...");
        for (Iterator iterator = getConnectors().values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.stop();
        }
        logger.info("Connectors have been stopped successfully");
    }

    public UMOModel lookupModel(String name)
    {
        try
        {
            return (UMOModel) lookupObject(name);
        }
        catch (ObjectNotFoundException e)
        {
            return null;
        }
    }

    public void registerModel(UMOModel model) throws UMOException
    {
//        if(models.get(model.getName())!=null)
//        {
//            throw new MuleException(new Message(Messages.CONTAINER_X_ALREADY_REGISTERED, "model:" + model.getName()));
//        }
//        models.put(model.getName(), model);
//        if (initialised.get())
//        {
//            model.initialise();
//        }

        if (started.get())
        {
            model.start();
        }
    }

    public void unregisterModel(String name)
    {
//        UMOModel model = lookupModel(name);
//        if (model != null)
//        {
//            models.remove(model);
//            model.dispose();
//        }
    }

    public Map getModels()
    {
        return applicationContext.getBeansOfType(UMOModel.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getConnectors()
    {
        return applicationContext.getBeansOfType(UMOConnector.class);
    }

    public Map getAgents()
    {
        return applicationContext.getBeansOfType(UMOAgent.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpoints()
    {
        return applicationContext.getBeansOfType(UMOImmutableEndpoint.class);
    }

    /**
     * {@inheritDoc}
     */
    public Map getTransformers()
    {
        return applicationContext.getBeansOfType(UMOTransformer.class);
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
//        logger.info("Adding agent " + agent.getName());
//        agents.put(agent.getName(), agent);
//        agent.registered();
//        // Don't allow initialisation while the server is being initalised,
//        // only when we are done. Otherwise the agent registration
//        // order can be corrupted.
//        if (initialised.get())
//        {
//            logger.info("Initialising agent " + agent.getName());
//            agent.initialise();
//        }
//        if ((started.get() || starting.get()))
//        {
//            logger.info("Starting agent " + agent.getName());
//            agent.start();
//        }
    }

    public UMOAgent lookupAgent(String name)
    {
        try
        {
            return (UMOAgent) lookupObject(name);
        }
        catch (ObjectNotFoundException e)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public UMOAgent unregisterAgent(String name) throws UMOException
    {
//        if (name == null)
//        {
//            return null;
//        }
//        UMOAgent agent = (UMOAgent) agents.remove(name);
//        if (agent != null)
//        {
//            agent.dispose();
//            agent.unregistered();
//        }
//        return agent;
        return null;
    }

    /**
     * Initialises all registered agents
     *
     * @throws InitialisationException
     */
//    protected void initialiseAgents() throws InitialisationException
//    {
//        logger.info("Initialising agents...");
//
//        // Do not iterate over the map directly, as 'complex' agents
//        // may spawn extra agents during initialisation. This will
//        // cause a ConcurrentModificationException.
//        // Use a cursorable iteration, which supports on-the-fly underlying
//        // data structure changes.
//        Collection agentsSnapshot = agents.values();
//        CursorableLinkedList agentRegistrationQueue = new CursorableLinkedList(agentsSnapshot);
//        CursorableLinkedList.Cursor cursor = agentRegistrationQueue.cursor();
//
//        // the actual agent object refs are the same, so we are just
//        // providing different views of the same underlying data
//
//        try
//        {
//            while (cursor.hasNext())
//            {
//                UMOAgent umoAgent = (UMOAgent) cursor.next();
//
//                int originalSize = agentsSnapshot.size();
//                logger.debug("Initialising agent: " + umoAgent.getName());
//                umoAgent.initialise();
//                // thank you, we are done with you
//                cursor.remove();
//
//                // Direct calls to MuleManager.registerAgent() modify the original
//                // agents map, re-check if the above agent registered any
//                // 'child' agents.
//                int newSize = agentsSnapshot.size();
//                int delta = newSize - originalSize;
//                if (delta > 0)
//                {
//                    // TODO there's some mess going on in
//                    // http://issues.apache.org/jira/browse/COLLECTIONS-219
//                    // watch out when upgrading the commons-collections.
//                    Collection tail = CollectionUtils.retainAll(agentsSnapshot, agentRegistrationQueue);
//                    Collection head = CollectionUtils.subtract(agentsSnapshot, tail);
//
//                    // again, above are only refs, all going back to the original agents map
//
//                    // re-order the queue
//                    agentRegistrationQueue.clear();
//                    // 'spawned' agents first
//                    agentRegistrationQueue.addAll(head);
//                    // and the rest
//                    agentRegistrationQueue.addAll(tail);
//
//                    // update agents map with a new order in case we want to re-initialise
//                    // MuleManager on the fly
//                    this.agents.clear();
//                    for (Iterator it = agentRegistrationQueue.iterator(); it.hasNext();)
//                    {
//                        UMOAgent theAgent = (UMOAgent) it.next();
//                        this.agents.put(theAgent.getName(), theAgent);
//                    }
//                }
//            }
//        }
//        finally
//        {
//            // close the cursor as per JavaDoc
//            cursor.close();
//        }
//        logger.info("Agents Successfully Initialised");
//    }

    /**
     * {@inheritDoc}
     */
    protected void startAgents() throws UMOException
    {
        UMOAgent umoAgent;
        logger.info("Starting agents...");
        for (Iterator iterator = getAgents().values().iterator(); iterator.hasNext();)
        {
            umoAgent = (UMOAgent) iterator.next();
            logger.info("Starting agent: " + umoAgent.getDescription());
            umoAgent.start();

        }
        logger.info("Agents Successfully Started");
    }

    /**
     * {@inheritDoc}
     */
    protected void stopAgents() throws UMOException
    {
        logger.info("Stopping agents...");
        for (Iterator iterator = getAgents().values().iterator(); iterator.hasNext();)
        {
            UMOAgent umoAgent = (UMOAgent) iterator.next();
            logger.debug("Stopping agent: " + umoAgent.getName());
            umoAgent.stop();
        }
        logger.info("Agents Successfully Stopped");
    }

    /**
     * {@inheritDoc}
     */
    protected void disposeAgents()
    {
        UMOAgent umoAgent;
        logger.info("disposing agents...");
        for (Iterator iterator = getAgents().values().iterator(); iterator.hasNext();)
        {
            umoAgent = (UMOAgent) iterator.next();
            logger.debug("Disposing agent: " + umoAgent.getName());
            umoAgent.dispose();
        }
        logger.info("Agents Successfully Disposed");
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
//        if (container == null)
//        {
//            if (containerContext != null)
//            {
//                containerContext.dispose();
//            }
//            containerContext = new MultiContainerContext();
//        }
//        else
//        {
            containerContext.addContainer(container);
       // }
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
        if (managementContext != null)
        {
            managementContext.fireNotification(e);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Event Manager is not enabled, ignoring event: " + e);
        }
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
        //TODO add method for loading a transport service descriptor. Remember to pass in the registry Context
    }

    public Object lookupObject(Object key) throws ObjectNotFoundException
    {
        return containerContext.getComponent(key);
    }


    public RegistryStore getRegistryStore()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getRegisteredComponents(String parentId, String type)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getRegisteredComponents(String parentId)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Registration getRegisteredComponent(String id)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void notifyStateChange(String id, int state)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void notifyPropertyChange(String id, String propertyName, Object propertyValue)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPersistenceMode()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public UMOManagementContext getManagementContext()
    {
        try
        {
            return (UMOManagementContext)lookupObject("_managementContextFactoryBean");
        }
        catch (ObjectNotFoundException e)
        {
            throw new IllegalStateException("ManagementContext not found in the runtime registry");
        }
    }

    public UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException
    {
        UMOEndpoint endpoint = TransportFactory.createEndpoint(uri, type);
        endpoint.initialise();
        return endpoint;
    }

    public UMOEndpoint getEndpointFromUri(String uri) throws ObjectNotFoundException
    {
        UMOEndpoint endpoint = null;
        if (uri != null)
        {
            endpoint = lookupEndpoint(uri);
        }
        return endpoint;
    }

    public UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException
    {
        String endpointName = uri.getEndpointName();
        if (endpointName != null)
        {
            UMOEndpoint endpoint = lookupEndpoint(endpointName);
            if (endpoint != null)
            {
                if (StringUtils.isNotEmpty(uri.getAddress()))
                {
                    endpoint.setEndpointURI(uri);
                }
            }
            return endpoint;
        }

        return null;
    }

    public UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uriIdentifier);
        if (endpoint == null)
        {
            endpoint = createEndpointFromUri(new MuleEndpointURI(uriIdentifier), type);

        }
        else
        {
            if (endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER))
            {
                endpoint.setType(type);
            }
            else if (!endpoint.getType().equals(type))
            {
                throw new IllegalArgumentException("Endpoint matching: " + uriIdentifier
                        + " is not of type: " + type + ". It is of type: "
                        + endpoint.getType());

            }
        }
        return endpoint;
    }

    public UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uri);
        if (endpoint == null)
        {
            endpoint = createEndpointFromUri(uri, type);
        }
        return endpoint;
    }

}

