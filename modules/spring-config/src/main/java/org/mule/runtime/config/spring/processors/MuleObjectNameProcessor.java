/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.processors;

import org.mule.runtime.core.api.NameableObject;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;

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
    private final Class<? extends NameableObject> managedTypes[] = new Class[] {
                                                                                Connector.class,
                                                                                Transformer.class,
                                                                                Agent.class
    };

    @Override
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

    @Override
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
