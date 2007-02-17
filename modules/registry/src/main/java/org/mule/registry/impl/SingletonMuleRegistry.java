/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.MuleException;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.internal.admin.MuleAdminAgent;
import org.mule.impl.model.ModelFactory;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MapUtils;
import org.mule.util.SpiUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class SingletonMuleRegistry extends AbstractMuleRegistry implements Startable, Stoppable
{
    /**
     * Connectors registry
     */
    private Map connectors = new HashMap();

    /**
     * Endpoints registry
     */
    private Map endpointIdentifiers = new HashMap();

    /**
     * Holds any registered agents
     */
    private Map agents = new LinkedHashMap();

    /**
     * Holds a list of global endpoints accessible to any client code
     */
    private Map endpoints = new HashMap();

    /**
     * The model being used
     */
    private Map models = new LinkedHashMap();

    /**
     * Collection for transformers registered in this component
     */
    private Map transformers = new HashMap();

    /**
     * Maintains a reference to any interceptor stacks configured on the manager
     */
    private Map interceptorsMap = new HashMap();

    /**
     * Service descriptor cache. 
     * 
     * @deprecated This needs to be redesigned for an OSGi environment where ServiceDescriptors may change.
     */
    // @GuardedBy("this")
    protected static Map sdCache = new HashMap();

    
    public void initialise() throws InitialisationException
    {
        super.initialise();

        try
        {
            // There should always be a default system model registered.
            UMOModel systemModel = ModelFactory.createModel(SYSTEM_MODEL_TYPE);
            systemModel.setName(SYSTEM_MODEL);
            systemModel.initialise();
            registerModel(systemModel);

            // Create admin agent unless disabled.
            boolean disableAdmin = MapUtils.getBooleanValue(System.getProperties(),
                MuleProperties.DISABLE_SERVER_CONNECTIONS_SYSTEM_PROPERTY, false);
//            if (StringUtils.isBlank(MuleManager.config.getServerUrl()))
//            {
//                logger.info("Server endpointUri is null, not registering Mule Admin agent");
//                disableAdmin = true;
//            }
            if (!disableAdmin)
            {
                UMOAgent agent = new MuleAdminAgent();
                agent.initialise();
                registerAgent(agent);
            }
        }
        catch (UMOException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public void dispose()
    {
        super.dispose();

        transformers.clear();
        endpoints.clear();
        endpointIdentifiers.clear();

        transformers = null;
        endpoints = null;
        endpointIdentifiers = null;
    }

    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @throws UMOException if the the connectors or components fail to start
     */
    public synchronized void start() throws UMOException
    {
        startConnectors();
        startAgents();
        //fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTING_MODELS));
        for (Iterator i = models.values().iterator(); i.hasNext();)
        {
            UMOModel model = (UMOModel) i.next();
            model.start();
        }
        //fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTED_MODELS));
    
    }
    
    /**
     * Starts the connectors
     *
     * @throws MuleException if the connectors fail to start
     */
    private void startConnectors() throws UMOException
    {
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.startConnector();
        }
        logger.info("Connectors have been started successfully");
    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and connectors
     *
     * @throws UMOException if either any of the sessions or connectors fail to stop
     */
    public synchronized void stop() throws UMOException
    {
        stopConnectors();
        stopAgents();

        logger.debug("Stopping model...");
        //fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPING_MODELS));
        for (Iterator i = models.values().iterator(); i.hasNext();)
        {
            UMOModel model = (UMOModel) i.next();
            model.stop();
        }
        //fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED_MODELS));
    }
    
    /**
     * Stops the connectors
     *
     * @throws MuleException if any of the connectors fail to stop
     */
    private void stopConnectors() throws UMOException
    {
        logger.debug("Stopping connectors...");
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.stopConnector();
        }
        logger.info("Connectors have been stopped successfully");
    }

    /**
     * {@inheritDoc}
     */
    protected void startAgents() throws UMOException
    {
        UMOAgent umoAgent;
        logger.info("Starting agents...");
        for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
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
        for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
        {
            UMOAgent umoAgent = (UMOAgent) iterator.next();
            logger.debug("Stopping agent: " + umoAgent.getName());
            umoAgent.stop();
        }
        logger.info("Agents Successfully Stopped");
    }

    public UMOComponent registerComponent(UMODescriptor descriptor, String modelName) throws RegistryException
    {
        try
        {
            UMOModel model = lookupModel(modelName);
            if (model == null)
            {
                throw new RegistryException(Message.createStaticMessage("Unable to look up model: " + modelName));
            }
    
            if (descriptor == null)
            {
                throw new ModelException(new Message(Messages.X_IS_NULL, "UMO Descriptor"));
            }
            // Set the es if one wasn't set in the configuration
            if (descriptor.getExceptionListener() == null)
            {
                descriptor.setExceptionListener(model.getExceptionListener());
            }
    
            descriptor.initialise();
    
            // detect duplicate descriptor declarations
            
            if (model.getDescriptor(descriptor.getName()) != null)
            {
                throw new ModelException(new Message(Messages.DESCRIPTOR_X_ALREADY_EXISTS, descriptor.getName()));
            }
    
            UMOComponent component = (UMOComponent)model.getComponent(descriptor.getName());
            if (component == null)
            {
                component = model.createComponent(descriptor);
                model.addComponent(descriptor, component);
            }
    
            logger.debug("Added Mule UMO: " + descriptor.getName());
    
            logger.info("Initialising component: " + descriptor.getName());
            component.initialise();

            return component;
        }
        catch (UMOException e)
        {
            throw new RegistryException(Message.createStaticMessage("Unable to register component"), e);
        }
    }

    public UMOComponent registerSystemComponent(UMODescriptor descriptor) throws RegistryException
    {
        return registerComponent(descriptor, SYSTEM_MODEL);
    }
    
    public void unregisterComponent(String name) throws RegistryException
    {
        UMOComponent component = null;
        for (Iterator iterator = models.values().iterator(); iterator.hasNext();)
        {
            component = ((UMOModel) iterator.next()).removeComponent(name);
            if (component != null)
            {
                // The component was found and removed.
                return;
            }
        }
        // The component was not found in any model.
        throw new RegistryException(new Message(Messages.COMPONENT_X_NOT_REGISTERED, name));
    }

    public UMOComponent lookupComponent(String name) throws RegistryException
    {
        UMOComponent component = null;
        for (Iterator iterator = models.values().iterator(); iterator.hasNext();)
        {
            component = ((UMOModel) iterator.next()).getComponent(name);
            if (component != null)
            {
                return component;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public UMOConnector lookupConnector(String name)
    {
        return (UMOConnector) connectors.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public String lookupEndpointIdentifier(String logicalName, String defaultName)
    {
        String name = (String) endpointIdentifiers.get(logicalName);
        if (name == null)
        {
            return defaultName;
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public UMOEndpoint lookupEndpoint(String logicalName)
    {
        UMOEndpoint endpoint = (UMOEndpoint) endpoints.get(logicalName);
        if (endpoint != null)
        {
            return (UMOEndpoint) endpoint.clone();
        }
        else
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public UMOEndpoint lookupEndpointByAddress(String address)
    {
        UMOEndpoint endpoint = null;
        if (address != null)
        {
            boolean found = false;
            Iterator iterator = endpoints.keySet().iterator();
            while (!found && iterator.hasNext())
            {
                endpoint = (UMOEndpoint) endpoints.get(iterator.next());
                found = (address.equals(endpoint.getEndpointURI().toString()));
            }
        }
        return endpoint;
    }

    /**
     * {@inheritDoc}
     */
    public UMOTransformer lookupTransformer(String name)
    {
        return (UMOTransformer) transformers.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public void registerConnector(UMOConnector connector) throws RegistryException
    {
        connectors.put(connector.getName(), connector);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterConnector(String connectorName) throws RegistryException
    {
        UMOConnector c = (UMOConnector) connectors.remove(connectorName);
        if (c != null)
        {
            c.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerEndpointIdentifier(String logicalName, String endpoint)
    {
        endpointIdentifiers.put(logicalName, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpointIdentifier(String logicalName)
    {
        endpointIdentifiers.remove(logicalName);
    }

    /**
     * {@inheritDoc}
     */
    public void registerEndpoint(UMOEndpoint endpoint)
    {
        endpoints.put(endpoint.getName(), endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpoint(String endpointName)
    {
        UMOEndpoint p = (UMOEndpoint) endpoints.get(endpointName);
        if (p != null)
        {
            endpoints.remove(p);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerTransformer(UMOTransformer transformer) throws RegistryException
    {
        transformers.put(transformer.getName(), transformer);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterTransformer(String transformerName)
    {
        transformers.remove(transformerName);
    }

    public UMOModel lookupModel(String name)
    {
        return (UMOModel)models.get(name);
    }

    public void registerModel(UMOModel model) throws RegistryException
    {
        models.put(model.getName(), model);
    }

    public void unregisterModel(String name) throws RegistryException
    {
        UMOModel model = lookupModel(name);
        if(model!=null)
        {
            models.remove(model);
        }
    }

    public Map getModels()
    {
        return Collections.unmodifiableMap(models);
    }

    /**
     * {@inheritDoc}
     */
    public void registerInterceptorStack(String name, UMOInterceptorStack stack)
    {
        interceptorsMap.put(name, stack);
    }

    /**
     * {@inheritDoc}
     */
    public UMOInterceptorStack lookupInterceptorStack(String name)
    {
        return (UMOInterceptorStack) interceptorsMap.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public Map getConnectors()
    {
        return Collections.unmodifiableMap(connectors);
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpointIdentifiers()
    {
        return Collections.unmodifiableMap(endpointIdentifiers);
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpoints()
    {
        return Collections.unmodifiableMap(endpoints);
    }

    /**
     * {@inheritDoc}
     */
    public Map getTransformers()
    {
        return Collections.unmodifiableMap(transformers);
    }

    /**
     * {@inheritDoc}
     */
    public Map getAgents()
    {
        return Collections.unmodifiableMap(agents);
    }

    /**
     * {@inheritDoc}
     */
    public void registerAgent(UMOAgent agent) throws RegistryException
    {
        agents.put(agent.getName(), agent);
        agent.registered();
    }

    public UMOAgent lookupAgent(String name)
    {
        return (UMOAgent) agents.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterAgent(String name) throws RegistryException
    {
        if (name == null)
        {
            throw new RegistryException(new Message(Messages.X_IS_NULL, "Agent name"));
        }
        UMOAgent agent = (UMOAgent) agents.remove(name);
        if (agent != null)
        {
            agent.unregistered();
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
                try 
                {
                    sd = createServiceDescriptor(type, name, overrides);
                }
                catch (ServiceException e)
                {
                    logger.info("Unable to create service descriptor: " + e.getMessage());
                    return null;
                }
                sdCache.put(key, sd);
            }
        }
        return sd;
    }
        
    /**
     * @deprecated ServiceDescriptors will be created upon bundle startup for OSGi.
     */
    protected ServiceDescriptor createServiceDescriptor(String type, String name, Properties overrides)
        throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        return ServiceDescriptorFactory.create(type, name, props, overrides);
    }

    public void register(Registerable object) throws RegistryException
    {
        if (object instanceof UMOConnector)
        {
            registerConnector((UMOConnector) object);
        }
        else if (object instanceof UMOEndpoint)
        {
            registerEndpoint((UMOEndpoint) object);
        }
        else if (object instanceof UMOTransformer)
        {
            registerTransformer((UMOTransformer) object);
        }
        else if (object instanceof UMOModel)
        {
            registerModel((UMOModel) object);
        }
        else if (object instanceof UMOAgent)
        {
            registerAgent((UMOAgent) object);
        }
        else
        {
            logger.debug("Attempt to register object of unknown type: " + object.getClass().getName());
        }
    }
}

