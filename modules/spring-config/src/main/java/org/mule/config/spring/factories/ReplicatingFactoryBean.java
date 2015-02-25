/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.FactoryBean;

public class ReplicatingFactoryBean implements FactoryBean<Object>, MuleContextAware, Initialisable
{

    private String key;
    private String type;
    private Object defaultValue;

    private Object actualValue;
    private MuleContext muleContext;
    private Class<?> clazz;

    @Override
    public void initialise() throws InitialisationException
    {
        checkArgument(!StringUtils.isBlank(key), "key cannot be null");
        checkArgument(!StringUtils.isBlank(type), "must set type");
        try
        {
            clazz = ClassUtils.getClass(type);
        }
        catch (ClassNotFoundException e)
        {
            throw new InitialisationException(e, this);
        }

        actualValue = muleContext.getRegistry().lookupObject(key);
        if (actualValue == null)
        {
            actualValue = defaultValue;
        }
    }

    @Override
    public Object getObject() throws Exception
    {
        return actualValue;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    @Override
    public Class<?> getObjectType()
    {
        return clazz;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
    }
}
