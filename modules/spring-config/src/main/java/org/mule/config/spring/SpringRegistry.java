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

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.lifecycle.ContainerManagedLifecyclePhase;
import org.mule.lifecycle.GenericLifecycleManager;
import org.mule.registry.AbstractRegistry;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringRegistry extends AbstractRegistry
{
    public static final String REGISTRY_ID = "org.mule.Registry.Spring";

    /**
     * Key used to lookup Spring Application Context from SpringRegistry via Mule's
     * Registry interface.
     **/
    public static final String SPRING_APPLICATION_CONTEXT = "springApplicationContext";
    
    protected ApplicationContext applicationContext;

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
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(String id, ApplicationContext applicationContext)
    {
        super(id);
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(ConfigurableApplicationContext applicationContext, ApplicationContext parentContext)
    {
        super(REGISTRY_ID);
        applicationContext.setParent(parentContext);
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(String id, ConfigurableApplicationContext applicationContext, ApplicationContext parentContext)
    {
        super(id);
        applicationContext.setParent(parentContext);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (applicationContext instanceof ConfigurableApplicationContext)
        {
            ((ConfigurableApplicationContext) applicationContext).refresh();
        }
    }

    protected void doDispose()
    {
        // check we aren't trying to close an already closed context
        if (applicationContext instanceof MuleApplicationContext)
        {
            MuleContext muleContext = ((MuleApplicationContext) applicationContext).getMuleContext();
            if (!muleContext.isStarted())
            {
                // nothing to do
                return;
            }
        }
        
        if (applicationContext instanceof ConfigurableApplicationContext
            && ((ConfigurableApplicationContext) applicationContext).isActive())
        {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
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

    public Object lookupObject(String key)
    {
        if (StringUtils.isBlank(key))
        {
            logger.warn(
                    MessageFactory.createStaticMessage("Detected a lookup attempt with an empty or null key"),
                    new Throwable().fillInStackTrace());
            return null;
        }

        if (key.equals(SPRING_APPLICATION_CONTEXT) && applicationContext != null)
        {
            return applicationContext;
        }
        else
        {
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
    }

    public Collection lookupObjects(Class type)
    {
        try
        {
            Map map = applicationContext.getBeansOfType(type);
            // MULE-2762
            //if (logger.isDebugEnabled())
            //{
            //    MapUtils.debugPrint(System.out, "Beans of type " + type, map);
            //}
            return map.values();
        }
        catch (BeanCreationException bcex)
        {
            // BCE is a result of a broken config, propagate it (see MULE-3297 for more details)
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage(String.format("Failed to lookup beans of type %s from the Spring registry", type)),
                    bcex);
        }
        catch (Exception e)
        {
            logger.debug(e);
            return CollectionUtils.EMPTY_COLLECTION;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Registry is read-only
    ////////////////////////////////////////////////////////////////////////////////////

    public void registerObject(String key, Object value) throws RegistrationException
    {
        throw new UnsupportedOperationException("Registry is read-only so objects cannot be registered or unregistered.");
    }

    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        throw new UnsupportedOperationException("Registry is read-only so objects cannot be registered or unregistered.");
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        throw new UnsupportedOperationException("Registry is read-only so objects cannot be registered or unregistered.");
    }

    public void unregisterObject(String key)
    {
        throw new UnsupportedOperationException("Registry is read-only so objects cannot be registered or unregistered.");
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        throw new UnsupportedOperationException("Registry is read-only so objects cannot be registered or unregistered.");
    }
    
    ////////////////////////////////////////////////////////////////////////////////////
    // Registry meta-data
    ////////////////////////////////////////////////////////////////////////////////////
    
    public boolean isReadOnly()
    {
        return true;
    }

    public boolean isRemote()
    {
        return false;
    }
}
