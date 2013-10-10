/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.registry.AbstractRegistry;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringRegistry extends AbstractRegistry
{
    public static final String REGISTRY_ID = "org.mule.Registry.Spring";

    /**
     * Key used to lookup Spring Application Context from SpringRegistry via Mule's
     * Registry interface.
     */
    public static final String SPRING_APPLICATION_CONTEXT = "springApplicationContext";

    protected ApplicationContext applicationContext;

    //This is used to track the Spring context lifecycle since there is no way to confirm the
    //lifecycle phase from the application context
    protected AtomicBoolean springContextInitialised = new AtomicBoolean(false);

    public SpringRegistry(MuleContext muleContext)
    {
        super(REGISTRY_ID, muleContext);
    }

    public SpringRegistry(String id, MuleContext muleContext)
    {
        super(id, muleContext);
    }

    public SpringRegistry(ApplicationContext applicationContext, MuleContext muleContext)
    {
        super(REGISTRY_ID, muleContext);
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(String id, ApplicationContext applicationContext, MuleContext muleContext)
    {
        super(id, muleContext);
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(ConfigurableApplicationContext applicationContext, ApplicationContext parentContext, MuleContext muleContext)
    {
        super(REGISTRY_ID, muleContext);
        applicationContext.setParent(parentContext);
        this.applicationContext = applicationContext;
    }

    public SpringRegistry(String id, ConfigurableApplicationContext applicationContext, ApplicationContext parentContext, MuleContext muleContext)
    {
        super(id, muleContext);
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
        //This is used to track the Spring context lifecycle since there is no way to confirm the lifecycle phase from the application context
        springContextInitialised.set(true);
    }

    @Override
    public void doDispose()
    {
        // check we aren't trying to close a context which has never been started,
        // spring's appContext.isActive() isn't working for this case
        if (!this.springContextInitialised.get())
        {
            return;
        }

        if (applicationContext instanceof ConfigurableApplicationContext
                && ((ConfigurableApplicationContext) applicationContext).isActive())
        {
            ((ConfigurableApplicationContext) applicationContext).close();
        }

        // release the circular implicit ref to MuleContext
        applicationContext = null;

        this.springContextInitialised.set(false);
    }

    @Override
    protected RegistryLifecycleManager createLifecycleManager()
    {
        return new SpringRegistryLifecycleManager(getRegistryId(), this, muleContext);
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

    public <T> Collection<T> lookupObjects(Class<T> type)
    {
        return lookupByType(type).values();
    }
    
    /**
     * For lifecycle we only want spring to return singleton objects from it's application context
     */
    @Override
    public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type)
    {
        return internalLookupByType(type, false, false).values();
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        return internalLookupByType(type, true, true);
    }

    @SuppressWarnings("unchecked")
    protected <T> Map<String, T> internalLookupByType(Class<T> type, boolean nonSingletons, boolean eagerInit)
    {
        try
        {
            return applicationContext.getBeansOfType(type, nonSingletons, eagerInit);
        }
        catch (FatalBeanException fbex)
        {
            // FBE is a result of a broken config, propagate it (see MULE-3297 for more details)
            String message = String.format("Failed to lookup beans of type %s from the Spring registry", type);
            throw new MuleRuntimeException(MessageFactory.createStaticMessage(message), fbex);
        }
        catch (Exception e)
        {
            logger.debug(e);
            return Collections.emptyMap();
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
