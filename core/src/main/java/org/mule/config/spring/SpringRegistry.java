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

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.lifecycle.ContainerManagedLifecyclePhase;
import org.mule.impl.lifecycle.GenericLifecycleManager;
import org.mule.impl.registry.AbstractRegistry;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.Properties;

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
        lcm.registerLifecycle(new ContainerManagedLifecyclePhase(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME));
        lcm.registerLifecycle(new ContainerManagedLifecyclePhase(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME));
        return lcm;
    }

    protected Object doLookupObject(String key)
    {
        if (StringUtils.isBlank(key))
        {
            logger.warn(MessageFactory.createStaticMessage("Detected a lookup attempt with an empty or null key"),
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
        catch (Exception e)
        {
            logger.error(e);
            return null;
        }
    }

//  TODO MULE-1908
//    protected Object doLookupInContainerContext(String key, Class returntype) throws ObjectNotFoundException
//    {
//        if(containerContext==null)
//        {
//            containerContext = new MultiContainerContext();
//
//            Collection containers = doLookupObjects(UMOContainerContext.class);
//            if(containers.size()>0)
//            {
//                for (Iterator iterator = containers.iterator(); iterator.hasNext();)
//                {
//                    UMOContainerContext context = (UMOContainerContext) iterator.next();
//                    containerContext.addContainer(context);
//                }
//            }
//        }
//        Object o = containerContext.getComponent(key);
//        return o;
//    }

    protected Collection doLookupObjects(Class type)
    {
        return applicationContext.getBeansOfType(type).values();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        if (props == null)
        {
            throw new ServiceException(CoreMessages.failedToLoad(type + " " + name));
        }
        return ServiceDescriptorFactory.create(type, name, props, overrides, applicationContext);
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public boolean isRemote()
    {
        return false;
    }

}

