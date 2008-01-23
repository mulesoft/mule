/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.component.Component;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.model.Model;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.config.MuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.container.MultiContainerContext;
import org.mule.lifecycle.ContainerManagedLifecyclePhase;
import org.mule.lifecycle.GenericLifecycleManager;
import org.mule.registry.AbstractRegistry;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ConfigurableApplicationContext;

/** TODO */
public class SpringRegistry extends AbstractRegistry
{
    public static final String REGISTRY_ID = "org.mule.Registry.Spring";

    protected ConfigurableApplicationContext applicationContext;

    /**
     * TODO MULE-1908
     *
     * @deprecated Should MultiContainerContext still be used in 2.x? MULE-1908
     */
    protected MultiContainerContext containerContext;

    public SpringRegistry()
    {
        super(REGISTRY_ID);
    }

    public SpringRegistry(String id)
    {
        super(id);
    }

    public SpringRegistry(ConfigurableApplicationContext applicationContext)
    {
        super(REGISTRY_ID);
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(String id, ConfigurableApplicationContext applicationContext)
    {
        super(id);
        this.applicationContext = applicationContext;
    }

    protected LifecycleManager createLifecycleManager()
    {
        GenericLifecycleManager lcm = new GenericLifecycleManager();
        lcm.registerLifecycle(new ContainerManagedLifecyclePhase(Initialisable.PHASE_NAME,
                Initialisable.class, Disposable.PHASE_NAME));
        lcm.registerLifecycle(new ContainerManagedLifecyclePhase(Disposable.PHASE_NAME, Disposable.class,
                Initialisable.PHASE_NAME));
        return lcm;
    }

    protected Object doLookupObject(String key)
    {
        if (StringUtils.isBlank(key))
        {
            logger.warn(
                    MessageFactory.createStaticMessage("Detected a lookup attempt with an empty or null key"),
                    new Throwable().fillInStackTrace());
            return null;
        }

        try
        {
            return applicationContext.getBean(key);
        }
        catch (NoSuchBeanDefinitionException e)
        {
            logger.debug(e);
            return null;
        }
    }

    protected Collection doLookupObjects(Class type)
    {
        Map map = applicationContext.getBeansOfType(type);
        // MULE-2762
        //if (logger.isDebugEnabled())
        //{
        //    MapUtils.debugPrint(System.out, "Beans of type " + type, map);
        //}
        return map.values();
    }

    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides)
            throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        if (props == null)
        {
            throw new ServiceException(CoreMessages.failedToLoad(type + " " + name));
        }
        return ServiceDescriptorFactory.create(type, name, props, overrides, this);
    }

    /**
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    protected synchronized MuleConfiguration getLocalConfiguration()
    {
        return (MuleConfiguration) applicationContext.getBean(MuleProperties.OBJECT_MULE_CONFIGURATION);
    }

    /** {@inheritDoc} */
    public TransactionManager getTransactionManager()
    {
        try
        {
            return (TransactionManager) lookupObject(TransactionManager.class);
        }
        catch (RegistrationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Collection getModels()
    {
        return applicationContext.getBeansOfType(Model.class).values();
    }

    /** {@inheritDoc} */
    public Collection getConnectors()
    {
        return applicationContext.getBeansOfType(Connector.class).values();
    }

    public Collection getAgents()
    {
        return applicationContext.getBeansOfType(Agent.class).values();
    }

    /** {@inheritDoc} */
    public Collection getEndpoints()
    {
        return applicationContext.getBeansOfType(ImmutableEndpoint.class).values();
    }

    /** {@inheritDoc} */
    public Collection getTransformers()
    {
        return applicationContext.getBeansOfType(Transformer.class).values();
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public boolean isRemote()
    {
        return false;
    }

    public void registerConnector(Connector connector)
            throws MuleException
    {
        unsupportedOperation("registerConnector", connector);
    }

    public void unregisterConnector(String connectorName) throws MuleException
    {
        unsupportedOperation("unregisterConnector", connectorName);
    }

    public void registerEndpoint(ImmutableEndpoint endpoint)
            throws MuleException
    {
        unsupportedOperation("registerEndpoint", endpoint);
    }

    public void unregisterEndpoint(String endpointName)
    {
        unsupportedOperation("unregisterEndpoint", endpointName);
    }

    protected void doRegisterTransformer(Transformer transformer) throws MuleException
    {
        unsupportedOperation("registerTransformer", transformer);
    }

    public void unregisterTransformer(String transformerName)
    {
        unsupportedOperation("unregistertransformer", transformerName);
    }

    /** {@inheritDoc} */
    public void registerComponent(Component component)
            throws MuleException
    {
        unsupportedOperation("registerComponent", component);
    }

    public void unregisterComponent(String componentName)
    {
        unsupportedOperation("unregisterComponent", componentName);
    }

    public void registerModel(Model model) throws MuleException
    {
        unsupportedOperation("registerModel", model);
    }

    public void unregisterModel(String modelName)
    {
        unsupportedOperation("unregisterModel", modelName);
    }

    public void registerAgent(Agent agent) throws MuleException
    {
        unsupportedOperation("registerAgent", agent);
    }

    public void unregisterAgent(String agentName) throws MuleException
    {
        unsupportedOperation("unregisterAgent", agentName);
    }

    protected void doRegisterObject(String key,
                                    Object value,
                                    Object metadata) throws RegistrationException
    {
        unsupportedOperation("doRegisterObject", key);
    }

    public void unregisterObject(String key)
    {
        unsupportedOperation("unregisterObject", key);
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        unsupportedOperation("registryObjects", objects);
    }

    public void setConfiguration(MuleConfiguration config)
    {
        unsupportedOperation("setConfiguration", config);
    }

    public void registerEndpointBuilder(String name,
                                        EndpointBuilder builder) throws MuleException
    {
        unsupportedOperation("registerEndpointBuilder", builder);
    }
    
    protected void doDispose()
    {
        super.doDispose();
        applicationContext.close();
    }
}
