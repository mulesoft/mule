/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.agent.Agent;
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
