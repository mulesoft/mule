/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.NameableObject;
import org.mule.api.agent.Agent;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * <code>MuleObjectNameProcessor</code> is used to set spring ids to Mule object
 * names so the the bean id and name property on the object don't both have to be
 * set.
 */

public class MuleObjectNameProcessor implements BeanPostProcessor
{
    private boolean overwrite = false;
    private final Class<? extends NameableObject> managedTypes[] = new Class[]
            {
                    Connector.class,
                    Transformer.class,
                    Service.class,
                    Model.class,
                    Agent.class
            };

    public Object postProcessBeforeInitialization(Object object, String beanName) throws BeansException
    {
        for (Class<? extends NameableObject> managedType : managedTypes)
        {
            if (managedType.isInstance(object))
            {
                setNameIfNecessary((NameableObject) object, beanName);
            }
        }

        return object;
    }

    private void setNameIfNecessary(NameableObject nameable, String name)
    {
        if (nameable.getName() == null || overwrite)
        {
            nameable.setName(name);
        }
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        return o;
    }

    public boolean isOverwrite()
    {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

}
