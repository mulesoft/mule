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

import org.mule.RegistryContext;
import org.mule.impl.ManagementContext;
import org.mule.impl.container.MultiContainerContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import org.mule.umo.security.UMOSecurityManager;

import java.util.Iterator;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <code>UMOManagerFactoryBean</code> is a MuleManager factory bean that is used to
 * configure the MuleManager from a spring context. This factory bean is responsible
 * for determining the instance type of UMOManager to create and then delegates
 * configuration calls to that instance depending on what is available in the
 * container. <p/> Apart from removing the need to explicitly wire the MuleManager
 * instance together there another advantage to using the
 * AutowireUMOManagerFactoryBean. There is no need to declare a UMOModel instance in
 * the configuration. If the factory doesn't find a UMOModel implementation it
 * creates a default one of type <i>org.mule.impl.model.seda.SedaModel</i>. The
 * model is automatically initialised with a SpringContainercontext using the current
 * beanFactory and defaults are used for the other Model properties. If you want to
 * override the defaults, such as define your own exception strategy, (which you will
 * most likely want to do) simply declare your exception strategy bean in the
 * container and it will automatically be set on the model. <p/> Most Mule objects
 * have explicit types and can be autowired, however some objects cannot be
 * autowired, such as a <i>java.util.Map</i> of endpoints for example. For these
 * objects Mule defines standard bean names that will be looked for in the container
 * during start up. <p/> muleEnvironmentProperties A map of properties to set on the
 * MuleManager. Accessible from your code using
 * AutowireUMOManagerFactoryBean.MULE_ENVIRONMENT_PROPERTIES_BEAN_NAME. <p/>
 * muleEndpointMappings A Map of logical endpointUri mappings accessible from your
 * code using AutowireUMOManagerFactoryBean.MULE_ENDPOINT_MAPPINGS_BEAN_NAME. <p/>
 * muleInterceptorStacks A map of interceptor stacks, where the name of the stack is
 * the key and a list of interceptors is the value. Accessible using from your code
 * using AutowireUMOManagerFactoryBean.MULE_INTERCEPTOR_STACK_BEAN_NAME.
 */
public class ManagementContextFactoryBean extends AbstractFactoryBean
        implements ApplicationContextAware
{
    /**
     * logger used by this class
     */
    protected static Log logger = LogFactory.getLog(ManagementContextFactoryBean.class);

    protected UMOManagementContext managementContext = new ManagementContext();

    //TODO LM: Replace
    protected RegistryFacade registry;

    private ApplicationContext context;

    protected Object createInstance() throws Exception
    {
        if(managementContext==null)
        {
            this.managementContext = new ManagementContext();
        }
        return managementContext;
    }

    public Class getObjectType()
    {
        return UMOManagementContext.class;
    }

    protected UMOManagementContext getManagementContext()
    {
        return managementContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.context = applicationContext;
        registry = RegistryContext.getRegistry();
    }


    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        managementContext.initialise();        
        init();
    }


    //@java.lang.Override
    protected void destroyInstance(Object instance) throws Exception
    {
        managementContext.dispose();
        managementContext = null;
        registry = null;
    }

    protected void init()
    {
        try
        {
            boolean legacy = false;
            //Legacy handling.  If the context contains an AutowireUMOManagerFactoryBean, then we're dealing
            //with an old Mule config file and we change the way we deal with some of the components
            Map temp = context.getBeansOfType(AutowireUMOManagerFactoryBean.LegacyManager.class);
            if(temp.size() > 0)
            {
                legacy = true;
                registry.getConfiguration().setId(((AutowireUMOManagerFactoryBean.LegacyManager)temp.values().iterator().next()).getManagerId());
                // set environment properties
                try
                {
                    setLegacyProperties((Map)context.getBean("muleEnvironmentProperties", Map.class));
                }
                catch (BeansException e)
                {
                    //ignore
                }

            }

            // Set the container Context
            Map containers = context.getBeansOfType(UMOContainerContext.class, true, false);
            setContainerContext(containers);

            // set mule transaction manager
            temp = context.getBeansOfType(UMOTransactionManagerFactory.class, true, false);
            if (temp.size() > 0)
            {
                managementContext.setTransactionManager(((UMOTransactionManagerFactory)temp.values().iterator().next()).create());
            }
            else
            {
                temp = context.getBeansOfType(TransactionManager.class, true, false);
                if (temp.size() > 0)
                {
                    managementContext.setTransactionManager(((TransactionManager)temp.values().iterator().next()));
                }
            }

            // set security manager
            temp = context.getBeansOfType(UMOSecurityManager.class, true, false);
            if (temp.size() > 0)
            {
               managementContext.setSecurityManager((UMOSecurityManager)temp.values().iterator().next());
            }

        }
        catch (Exception e)
        {
            throw new BeanInitializationException("Failed to wire MuleManager together: " + e.getMessage(), e);
        }
    }

    public void destroy() throws Exception
    {
        super.destroy();
        managementContext.dispose();
    }

    public void setManagerId(String managerId)
    {
       managementContext.setId(managerId);
    }

    protected void setContainerContext(Map containers) throws UMOException
    {

        for (Iterator iter = containers.values().iterator(); iter.hasNext();)
        {
            UMOContainerContext context =  (UMOContainerContext)iter.next();
            if(!(context instanceof MultiContainerContext))
            {
                registry.registerContainerContext(context);
            }

        }
    }

    protected void setLegacyProperties(Map props)
    {
        if(props!=null)
        {
            registry.addProperties(props);
        }
    }
}
