/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.util;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.config.i18n.MessageFactory;
import org.mule.object.AbstractObjectFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * This is an implementation of the ObjectFactory interface which simply delegates to 
 * the Spring ApplicationContext.  Since the delegation happens each time a call to 
 * getOrCreate() is made, this will correctly handle Spring beans which are 
 * non-singletons (factory beans, etc.)
 * 
 * Singleton usage:
 * 
 *   <model>
 *       <service name="myOrangeService">
 *           <service>
 *               <spring-object bean="myBean"/>
 *           </service>
 *       </service>
 *   </model>
 *
 *   <spring:bean id="myBean" class="com.foo.Bar"/>
 *   
 * Non-singleton usage:
 * 
 *   <model>
 *       <service name="myOrangeService">
 *           <service>
 *               <spring-object bean="myFactoryBean"/>
 *           </service>
 *       </service>
 *   </model>
 *
 *   <spring:bean id="myFactoryBean" class="com.foo.BarFactory" factory-method="getNewBar"/>
 */
public class SpringBeanLookup extends AbstractObjectFactory implements ApplicationContextAware
{
    private ApplicationContext applicationContext;
    private String bean;

    @Override
    public void initialise() throws InitialisationException
    {
        if (bean == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Bean name has not been set."), this);
        }
        if (applicationContext == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("ApplicationContext has not been injected."), this);
        }

        // Get instance of spring bean to determine bean type.
        // We do this because the result of org.springframework.beans.factory.BeanFactory.getType(String) when
        // used before bean initialization does not always return the same type as afterwards. One specific
        // case when AOP is used, and the actual bean class is returned before initialization but a proxy
        // afterwards. This affects both prototype beans and lazy-init singletons.
        objectClass = applicationContext.getBean(bean).getClass();
    }

    @Override
    public void dispose()
    {
        // Not implemented for Spring Beans
    }

    @Override
    public Object getInstance(MuleContext muleContext) throws Exception
    {
        Object instance = applicationContext.getBean(bean);
        if(instance instanceof FlowConstructAware)
        {
            //The servie cannot be autowired from within Spring, so we do it here
            ((FlowConstructAware)instance).setFlowConstruct(flowConstruct);
        }
        if(instance instanceof ServiceAware  && flowConstruct instanceof Service)
        {
            //The servie cannot be autowired from within Spring, so we do it here
            ((ServiceAware)instance).setService((Service) flowConstruct);
        }
        fireInitialisationCallbacks(instance);
        return instance;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public String getBean()
    {
        return bean;
    }

    public void setBean(String bean)
    {
        this.bean = bean;
    }
    
    @Override
    public boolean isSingleton()
    {
        return applicationContext.isSingleton(bean);
    }

    @Override
    public boolean isExternallyManagedLifecycle()
    {
        return true;
    }

    public boolean isAutoWireObject()
    {
        //Spring does the wiring
        return false;
    }
}
