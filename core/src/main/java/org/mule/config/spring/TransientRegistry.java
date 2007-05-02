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

import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.ManagementContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.internal.notifications.AdminNotificationListener;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.impl.internal.notifications.ComponentNotificationListener;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.impl.internal.notifications.ConnectionNotificationListener;
import org.mule.impl.internal.notifications.CustomNotification;
import org.mule.impl.internal.notifications.CustomNotificationListener;
import org.mule.impl.internal.notifications.ManagementNotification;
import org.mule.impl.internal.notifications.ManagementNotificationListener;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.ManagerNotificationListener;
import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.impl.internal.notifications.ModelNotificationListener;
import org.mule.impl.internal.notifications.RegistryNotification;
import org.mule.impl.internal.notifications.RegistryNotificationListener;
import org.mule.impl.internal.notifications.SecurityNotification;
import org.mule.impl.internal.notifications.SecurityNotificationListener;
import org.mule.impl.internal.notifications.ServerNotificationManager;
import org.mule.impl.lifecycle.ContainerManagedLifecyclePhase;
import org.mule.impl.lifecycle.GenericLifecycleManager;
import org.mule.impl.lifecycle.phases.DisposePhase;
import org.mule.impl.lifecycle.phases.InitialisePhase;
import org.mule.impl.lifecycle.phases.StartPhase;
import org.mule.impl.lifecycle.phases.StopPhase;
import org.mule.impl.model.ModelServiceDescriptor;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.impl.work.MuleWorkManager;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.registry.Registry;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.lifecycle.UMOLifecyclePhase;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.BeanUtils;
import org.mule.util.SpiUtils;
import org.mule.util.UUID;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.TransactionalQueueManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * TODO
 */
public class TransientRegistry extends AbstractRegistry
{
    public static final String REGISTRY_ID = "org.mule.Registry.Transient";
    /**
     * Service descriptor cache.
     *
     * @deprecated This needs to be redesigned for an OSGi environment where ServiceDescriptors may change.
     */
    // @GuardedBy("this")
    protected static Map sdCache = new HashMap();

    /**
     * Map of Maps registry
     */
    private Map registry;

    //TODO how do we handle Muleconfig across Registries
    private MuleConfiguration config; // = new MuleConfiguration();

    public TransientRegistry()
    {
        super(REGISTRY_ID);
        init();
    }

    public TransientRegistry(RegistryFacade parent)
    {
        super(REGISTRY_ID, parent);
        init();
    }

    private void init()
    {
         registry = new HashMap(8);

        //Register ManagementContext Injector for locally registered objects
        getObjectTypeMap(ObjectProcessor.class).put(MuleProperties.OBJECT_MANAGMENT_CONTEXT_PROCESSOR,
                new ManagementContextDependencyProcessor());

        //getObjectTypeMap(ObjectProcessor.class).put("_muleSeriveProcessor",
        //        new RegisteredServiceProcessor());

    }

    protected UMOLifecycleManager createLifecycleManager()
    {
        GenericLifecycleManager lcm = new GenericLifecycleManager();
        UMOLifecyclePhase initPhase = new InitialisePhase();
        initPhase.setRegistryScope(RegistryFacade.SCOPE_IMMEDIATE);
        lcm.registerLifecycle(initPhase);
        UMOLifecyclePhase disposePhase = new DisposePhase();
        disposePhase.setRegistryScope(RegistryFacade.SCOPE_IMMEDIATE);
        lcm.registerLifecycle(disposePhase);
        return lcm;
    }

    //@java.lang.Override
    protected void doInitialise() throws InitialisationException
    {
        int oldScope = getDefaultScope();
        setDefaultScope(RegistryFacade.SCOPE_IMMEDIATE);
        try
        {
            applyProcessors(getConnectors());
            applyProcessors(getTransformers());
            applyProcessors(getEndpoints());
            applyProcessors(getAgents());
            applyProcessors(getModels());
            applyProcessors(getServices());
            applyProcessors(lookupCollection(Object.class));

            getManagementContext().fireNotification(new RegistryNotification(this, RegistryNotification.REGISTRY_INITIALISED));
        }
        finally
        {
            setDefaultScope(oldScope);
        }

    }

    protected void applyProcessors( Map objects)
    {
        if(objects==null) return;
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            Map processors = lookupCollection(ObjectProcessor.class);
            for (Iterator iterator2 = processors.values().iterator(); iterator2.hasNext();)
            {
                ObjectProcessor op = (ObjectProcessor)iterator2.next();
                op.process(o);
            }
        }
    }


    protected Object doLookupObject(Object key, Class returntype) throws ObjectNotFoundException
    {
        if(key==null)
        {
            throw new NullPointerException("Object key cannot be null");
        }
        Map objects = (Map) registry.get(returntype);
        if (objects == null)
        {
            objects = (Map) registry.get(Object.class);
            if (objects == null)
            {
                throw new ObjectNotFoundException(key.toString());
            }
        }
        Object o = objects.get(key);
        if(o==null)
        {
            throw new ObjectNotFoundException(key.toString());
        }
        return o;
    }

    protected MuleConfiguration getLocalConfiguration()
    {
        return config;
    }

    public void setConfiguration(MuleConfiguration config)
    {
        this.config = config;
    }

    public Map doLookupCollection(Class returntype)
    {
        return (Map) registry.get(returntype);
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
                if(getParent()!=null)
                {
                    sd = getParent().lookupServiceDescriptor(type, name, overrides);
                    sdCache.put(key, sd);
                }
                else
                {
                    sd = createServiceDescriptor(type, name, overrides);

                }
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
        return ServiceDescriptorFactory.create(type, name, props, overrides, null);
    }

    protected Map getObjectTypeMap(Object o)
    {
        Object key;
        if(o instanceof Class)
        {
            key = (Class)o;
        }
        else if(o instanceof String)
        {
            key = o;
        }
        else
        {
            key = o.getClass();
        }
        Map objects = (Map)registry.get(key);
        if(objects==null)
        {
            objects = new HashMap(8);
            registry.put(key, objects);
        }
        return objects;
    }

    protected Object applyProcessors(Object object)
    {
        Object theObject = object;
        Map processors = lookupCollection(ObjectProcessor.class);
        for (Iterator iterator = processors.values().iterator(); iterator.hasNext();)
        {
            ObjectProcessor o = (ObjectProcessor)iterator.next();
            theObject = o.process(theObject);
        }
        return theObject;
    }

    protected void applyLifecycle(Object object)
    {
        try
        {
            lifecycleManager.applyLifecycle(getManagementContext(), object);
        }
        catch (UMOException e)
        {
            //TODO
            throw new MuleRuntimeException(Message.createStaticMessage("Failed to invoke Lifecycle on object: " + object), e);
        }
    }
    /**
     * Allows for arbitary registration of transient objects
     * @param key
     * @param value
     */
    public void registerObject(Object key, Object value)
    {
        registerObject(Object.class, key, value);
    }

    /**
     * Allows for arbitary registration of transient objects
     * @param key
     * @param value
     */
    public void registerObject(Class type, Object key, Object value)
    {
        if(isInitialised() || isInitialising())
        {
            value = applyProcessors(value);
        }
        getObjectTypeMap(type).put(key, value);
        applyLifecycle(value);
    }

    //@java.lang.Override
    public void registerAgent(UMOAgent agent) throws UMOException
    {
        registerObject(UMOAgent.class, agent.getName(), agent);
    }

    //@java.lang.Override
    public void registerConnector(UMOConnector connector) throws UMOException
    {
        registerObject(UMOConnector.class, connector.getName(), connector);
    }

    //@java.lang.Override
    public void registerEndpoint(UMOEndpoint endpoint) throws UMOException
    {
        registerObject(UMOImmutableEndpoint.class, endpoint.getName(), endpoint);
    }

    //@java.lang.Override
    public void registerModel(UMOModel model) throws UMOException
    {
        registerObject(UMOModel.class, model.getName(), model);
    }


    //@java.lang.Override
    public void registerProperties(Map props)
    {
        getObjectTypeMap(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES).putAll(props);
    }

    //@java.lang.Override
    public void registerProperty(Object key, Object value)
    {
        getObjectTypeMap(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES).put(key, value);
    }

    //@java.lang.Override
    public Object lookupProperty(Object key)
    {
        return getObjectTypeMap(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES).get(key);
    }

    //@java.lang.Override
    public void registerTransformer(UMOTransformer transformer) throws UMOException
    {
        registerObject(UMOTransformer.class, transformer.getName(), transformer);
    }

    //@java.lang.Override
    public void registerService(UMODescriptor service) throws UMOException
    {
        String modelName = service.getModelName();
        UMOModel model = lookupModel(modelName);
        if(model==null)
        {
            //TODO
            throw new IllegalStateException("Service must be associated with an existing model. Not found: " + modelName);
        }
        registerObject(UMODescriptor.class, service.getName(), service);
        model.registerComponent(service);
    }

    //@java.lang.Override
    public UMODescriptor unregisterService(String serviceName)
    {
        return (UMODescriptor)getObjectTypeMap(UMODescriptor.class).remove(serviceName);
    }


    //@java.lang.Override
    public UMOAgent unregisterAgent(String agentName) throws UMOException
    {
        return (UMOAgent)getObjectTypeMap(UMOAgent.class).remove(agentName);
    }

    //@java.lang.Override
    public UMOConnector unregisterConnector(String connectorName) throws UMOException
    {
        return (UMOConnector)getObjectTypeMap(UMOConnector.class).remove(connectorName);
    }

    //@java.lang.Override
    public UMOImmutableEndpoint unregisterEndpoint(String endpointName)
    {
        return (UMOImmutableEndpoint)getObjectTypeMap(UMOImmutableEndpoint.class).remove(endpointName);
    }

    //@java.lang.Override
    public UMOModel unregisterModel(String modelName)
    {
        return (UMOModel)getObjectTypeMap(UMOModel.class).remove(modelName);
    }

    //@java.lang.Override
    public UMOTransformer unregisterTransformer(String transformerName)
    {
        return (UMOTransformer)getObjectTypeMap(UMOTransformer.class).remove(transformerName);
    }

    //@java.lang.Override
    public UMOEndpoint lookupEndpoint(String name)
    {
        UMOEndpoint ep = super.lookupEndpoint(name);
        if(ep!=null)
        {
            try
            {
                //TODO: friggin' cloning
                ep = new MuleEndpoint(ep);                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return ep;
    }

    //@java.lang.Override
    public UMOTransformer lookupTransformer(String name)
    {
        UMOTransformer transformer = super.lookupTransformer(name);
        if(transformer!=null)
        {
            try
            {
                if(transformer.getEndpoint()!=null)
                {
                    throw new IllegalStateException("Endpoint cannot be set");
                }
//                Map props = BeanUtils.describe(transformer);
//                props.remove("endpoint");
//                props.remove("strategy");
//                transformer = (UMOTransformer)ClassUtils.instanciateClass(transformer.getClass(), ClassUtils.NO_ARGS);
                //TODO: friggin' cloning
                transformer = (UMOTransformer)BeanUtils.cloneBean(transformer);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return transformer;
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public boolean isRemote()
    {
        return false;
    }



    public static TransientRegistry createNew() throws UMOException
    {
        //Use the default server lifecycleManager
        //UMOLifecycleManager lifecycleManager = new DefaultLifecycleManager();
        UMOLifecycleManager lifecycleManager = new GenericLifecycleManager();


        //Create Lifecycle phases
        Class[] ignorredObjects = new Class[]{Registry.class, UMOManagementContext.class};

        lifecycleManager.registerLifecycle(new ContainerManagedLifecyclePhase(Initialisable.PHASE_NAME, Initialisable.class));
        lifecycleManager.registerLifecycle(new StartPhase(ignorredObjects));
        lifecycleManager.registerLifecycle(new StopPhase(ignorredObjects));
        lifecycleManager.registerLifecycle(new ContainerManagedLifecyclePhase(Disposable.PHASE_NAME, Disposable.class));

        //Create the registry
        TransientRegistry registry = new TransientRegistry();

        RegistryContext.setRegistry(registry);

        MuleConfiguration config = new MuleConfiguration();

        registry.setConfiguration(config);

        QueueManager queueManager = new TransactionalQueueManager();
        queueManager.setPersistenceStrategy(new CachingPersistenceStrategy(new MemoryPersistenceStrategy()));

        ThreadingProfile tp = config.getDefaultThreadingProfile();
        UMOWorkManager workManager = new MuleWorkManager(tp, "MuleServer");

        ServerNotificationManager notificationManager = new ServerNotificationManager();
        notificationManager.registerEventType(ManagerNotificationListener.class, ManagerNotification.class);
        notificationManager.registerEventType(ModelNotificationListener.class, ModelNotification.class);
        notificationManager.registerEventType(ComponentNotificationListener.class, ComponentNotification.class);
        notificationManager.registerEventType(SecurityNotificationListener.class, SecurityNotification.class);
        notificationManager.registerEventType(ManagementNotificationListener.class, ManagementNotification.class);
        notificationManager.registerEventType(AdminNotificationListener.class, AdminNotification.class);
        notificationManager.registerEventType(CustomNotificationListener.class, CustomNotification.class);
        notificationManager.registerEventType(ConnectionNotificationListener.class, ConnectionNotification.class);
        notificationManager.registerEventType(RegistryNotificationListener.class, RegistryNotification.class);

        UMOSecurityManager securityManager = new MuleSecurityManager();

        UMOManagementContext context = new ManagementContext(lifecycleManager);
        context.setId(UUID.getUUID());

        registry.registerObject(MuleProperties.OBJECT_MANAGMENT_CONTEXT, context);
        registry.registerObject(ObjectProcessor.class, MuleProperties.OBJECT_MANAGMENT_CONTEXT_PROCESSOR,
                new ManagementContextDependencyProcessor(context));

        //Register objects so we get lifecycle management
        registry.registerObject(MuleProperties.OBJECT_MANAGMENT_CONTEXT, context);
        registry.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, securityManager);
        registry.registerObject(MuleProperties.OBJECT_WORK_MANAGER, workManager);
        registry.registerObject(MuleProperties.OBJECT_NOTIFICATION_MANAGER, notificationManager);
        registry.registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);

        //Set the object explicitly on the ManagementContext
        context.setWorkManager(workManager);
        context.setSecurityManager(securityManager);
        context.setNotificationManager(notificationManager);
        context.setQueueManager(queueManager);


        //Register the system Model
        ModelServiceDescriptor sd = (ModelServiceDescriptor)
            registry.lookupServiceDescriptor(ServiceDescriptorFactory.MODEL_SERVICE_TYPE, config.getSystemModelType(), null);

        UMOModel model = sd.createModel();
        model.setName(MuleProperties.OBJECT_SYSTEM_MODEL);
        registry.registerModel(model);
        return registry;
    }
}
