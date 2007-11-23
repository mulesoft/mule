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

import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.lifecycle.ContainerManagedLifecyclePhase;
import org.mule.impl.lifecycle.GenericLifecycleManager;
import org.mule.impl.registry.AbstractRegistry;
import org.mule.registry.RegistrationException;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MapUtils;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** TODO */
public class SpringRegistry extends AbstractRegistry implements ApplicationContextAware
{
    public static final String REGISTRY_ID = "org.mule.Registry.Spring";

    protected ApplicationContext applicationContext;

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

    public SpringRegistry(ApplicationContext applicationContext)
    {
        super(REGISTRY_ID);
        setApplicationContext(applicationContext);
    }

    public SpringRegistry(String id, ApplicationContext applicationContext)
    {
        super(id);
        setApplicationContext(applicationContext);
    }

    protected UMOLifecycleManager createLifecycleManager()
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
        if (logger.isDebugEnabled())
        {
            MapUtils.debugPrint(System.out, "Beans of type " + type, map);
        }
        return map.values();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
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
        return applicationContext.getBeansOfType(UMOModel.class).values();
    }

    /** {@inheritDoc} */
    public Collection getConnectors()
    {
        return applicationContext.getBeansOfType(UMOConnector.class).values();
    }

    public Collection getAgents()
    {
        return applicationContext.getBeansOfType(UMOAgent.class).values();
    }

    /** {@inheritDoc} */
    public Collection getEndpoints()
    {
        return applicationContext.getBeansOfType(UMOImmutableEndpoint.class).values();
    }

    /** {@inheritDoc} */
    public Collection getTransformers()
    {
        return applicationContext.getBeansOfType(UMOTransformer.class).values();
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public boolean isRemote()
    {
        return false;
    }

    public void registerConnector(UMOConnector connector)
            throws UMOException
    {
        unsupportedOperation("registerConnector", connector);
    }

    public void unregisterConnector(String connectorName) throws UMOException
    {
        unsupportedOperation("unregisterConnector", connectorName);
    }

    public void registerEndpoint(UMOImmutableEndpoint endpoint)
            throws UMOException
    {
        unsupportedOperation("registerEndpoint", endpoint);
    }

    public void unregisterEndpoint(String endpointName)
    {
        unsupportedOperation("unregisterEndpoint", endpointName);
    }

    protected void doRegisterTransformer(UMOTransformer transformer) throws UMOException
    {
        unsupportedOperation("registerTransformer", transformer);
    }

    public void unregisterTransformer(String transformerName)
    {
        unsupportedOperation("unregistertransformer", transformerName);
    }

    /** {@inheritDoc} */
    public void registerComponent(UMOComponent component)
            throws UMOException
    {
        unsupportedOperation("registerComponent", component);
    }

    public void unregisterComponent(String componentName)
    {
        unsupportedOperation("unregisterComponent", componentName);
    }

    public void registerModel(UMOModel model) throws UMOException
    {
        unsupportedOperation("registerModel", model);
    }

    public void unregisterModel(String modelName)
    {
        unsupportedOperation("unregisterModel", modelName);
    }

    public void registerAgent(UMOAgent agent) throws UMOException
    {
        unsupportedOperation("registerAgent", agent);
    }

    public void unregisterAgent(String agentName) throws UMOException
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
                                        UMOEndpointBuilder builder) throws UMOException
    {
        unsupportedOperation("registerEndpointBuilder", builder);
    }
}
