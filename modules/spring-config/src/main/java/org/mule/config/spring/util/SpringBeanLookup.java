/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.util;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.object.ObjectFactory;

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
public class SpringBeanLookup implements ObjectFactory, ApplicationContextAware
{
    private ApplicationContext applicationContext;
    private String bean;

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        if (bean == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Bean name has not been set."), this);
        }
        if (applicationContext == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("ApplicationContext has not been injected."), this);
        }
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {
        // Not implemented for Spring Beans
    }

    public Class getObjectClass() throws Exception
    {
        return applicationContext.getType(bean);
    }

    public Object getInstance() throws Exception
    {
        return applicationContext.getBean(bean);
    }

    public void release(Object object) throws Exception
    {
        // Not implemented for Spring Beans
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
}