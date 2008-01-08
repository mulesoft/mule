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

import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Responsible for passing in the ManagementContext instance for all objects in the
 * registry that want it. For an object to get an instance of the ManagementContext
 * it must implement ManagementContextAware.
 * 
 * @see ManagementContextAware
 * @see org.mule.umo.UMOManagementContext
 */
public class ManagementContextPostProcessor implements BeanPostProcessor
{

    private UMOManagementContext managementContext;

    public ManagementContextPostProcessor(UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof ManagementContextAware)
        {
            if (managementContext == null)
            {
                return bean;
            }

            ((ManagementContextAware) bean).setManagementContext(managementContext);
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

}
