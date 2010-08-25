/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.lifecycle.phases.ContainerManagedLifecyclePhase;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.registry.AbstractRegistry;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

        this.springContextInitialised.set(false);
    }

    @Override
    protected RegistryLifecycleManager createLifecycleManager()
    {
        Map<String, LifecyclePhase> phases = new HashMap<String, LifecyclePhase>(3);
        phases.put(Initialisable.PHASE_NAME, new SpringContextInitialisePhase());
        phases.put(Startable.PHASE_NAME, new MuleContextStartPhase());
        phases.put(Stoppable.PHASE_NAME, new MuleContextStopPhase());
        phases.put(Disposable.PHASE_NAME, new SpringContextDisposePhase());
        return new RegistryLifecycleManager(getRegistryId(), this, phases);
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

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> lookupByType(Class<T> type)
    {
        try
        {
            return applicationContext.getBeansOfType(type);
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

    /////////////////////////////////////////////////////////////////////////////////////
    // Spring custom lifecycle phases
    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * A lifecycle phase that will delegate any lifecycle invocations to a container such as Spring or Guice
     */
    class SpringContextInitialisePhase extends ContainerManagedLifecyclePhase
    {
        public SpringContextInitialisePhase()
        {
            super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);
            registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
        }

        /**
         * We don't need to apply any lifecycle here since Spring manages that for us
         *
         * @param o the object apply lifecycle to.  This parameter will be ignorred
         * @throws LifecycleException never thrown
         */
        @Override
        public void applyLifecycle(Object o) throws LifecycleException
        {
            //Spring starts initialised, do nothing here
        }
    }


    /**
     * A lifecycle phase that will delegate to the {@link org.mule.config.spring.SpringRegistry#doDispose()} method which in
     * turn will destroy the application context managed by this registry
     */
    class SpringContextDisposePhase extends ContainerManagedLifecyclePhase
    {
        public SpringContextDisposePhase()
        {
            super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);
            registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
            //You can dispose from all phases
            registerSupportedPhase(LifecyclePhase.ALL_PHASES);
        }

        @Override
        public void applyLifecycle(Object o) throws LifecycleException
        {
            doDispose();
        }
    }

}
