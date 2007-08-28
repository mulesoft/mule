/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import org.mule.RegistryContext;
import org.mule.impl.model.ModelFactory;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;

import org.springframework.beans.BeansException;
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
 *
 * @deprecated Should be Using Mule Namespace-aware XML. The equivilent in Mule 2.0 is ManagementContextFactoryBean
 * @see org.mule.config.spring.ManagementContextFactoryBean
 */
public class AutowireUMOManagerFactoryBean extends AbstractFactoryBean implements ApplicationContextAware
{
    private String managerId;
    private LegacyManagerPlaceholder manager;
    private ApplicationContext context;
    private static String defaultModel;

    protected Object createInstance() throws Exception
    {
        throw new IllegalStateException("Legacy beans configuration is no longer supported.  Please migrate your configuration to Mule 2.0 configuration");
//        if(manager==null)
//        {
//            manager = new LegacyManager(getManagerId());
//        }
        //return manager;
    }

    public Class getObjectType()
    {
        return LegacyManagerPlaceholder.class;
    }

    public boolean isSingleton()
    {
        return true;
    }


    public String getManagerId()
    {
        return managerId;
    }

    public void setManagerId(String managerId)
    {
        this.managerId = managerId;
    }

    protected void registerDefaultModel() throws UMOException
    {
        UMOModel m = ModelFactory.createModel("seda");
        RegistryContext.getRegistry().registerModel(m);
        defaultModel = m.getName();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.context = applicationContext;
    }

}
