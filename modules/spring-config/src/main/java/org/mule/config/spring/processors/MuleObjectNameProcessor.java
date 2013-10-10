/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.processors;

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

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {

        if (o instanceof Connector)
        {
            if (((Connector)o).getName() == null || overwrite)
            {
                ((Connector)o).setName(s);
            }
        }
        else if (o instanceof Transformer)
        {
            if (((Transformer)o).getName() == null || overwrite)
            {
               ((Transformer)o).setName(s);
            }
        }
        else if (o instanceof Service)
        {
            if (((Service)o).getName() == null || overwrite)
            {
                ((Service)o).setName(s);
            }
        }
        else if (o instanceof Model)
        {
            if (((Model)o).getName() == null || overwrite)
            {
                ((Model)o).setName(s);
            }
        }
        else if (o instanceof Agent)
        {
            ((Agent)o).setName(s);
        }
        return o;
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
