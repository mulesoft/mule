/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.construct.AbstractFlowConstruct;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractFlowConstructFactoryBean
    implements FactoryBean, ApplicationContextAware, MuleContextAware, Initialisable
{
    protected ApplicationContext applicationContext;
    protected MuleContext muleContext;
    protected String name;
    protected AbstractFlowConstruct flowConstruct;

    public boolean isSingleton()
    {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            flowConstruct = createFlowConstruct();
        }
        catch (MuleException me)
        {
            throw new InitialisationException(me, this);
        }

        flowConstruct.initialise();
    }

    public Object getObject() throws Exception
    {
        return flowConstruct;
    }

    protected abstract AbstractFlowConstruct createFlowConstruct() throws MuleException;
}
