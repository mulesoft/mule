/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.config.SpringContainerContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A temporary factory bean thst will associate the currenct Application context with a MuleContainer context
 * This can be removed when we plug the registry in
 */
public class SpringContainerContextFactoryBean implements FactoryBean, ApplicationContextAware
{

    private ApplicationContext context;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.context = applicationContext;
    }


    public Object getObject() throws Exception
    {
        SpringContainerContext ctx = new SpringContainerContext();
        ctx.setExternalBeanFactory(context);
        return ctx;
    }

    public Class getObjectType()
    {
        return SpringContainerContext.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
