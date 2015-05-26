/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.serialization.DefaultObjectSerializer;
import org.mule.api.serialization.ObjectSerializer;

import org.springframework.beans.factory.SmartFactoryBean;

/**
 * An eager {@link SmartFactoryBean} which returns the
 * {@link ObjectSerializer} which got configured as the
 * {@link MuleContext}'s default by invoking
 * {@link MuleContext#getObjectSerializer()}
 * <p/>
 * Because this class is annotated with the
 * {@link DefaultObjectSerializer} qualified, this factory
 * bean will be used to resolve injections requests for
 * such qualifier
 *
 * @since 3.7.0
 */
@DefaultObjectSerializer
public class DefaultObjectSerializerFactoryBean implements SmartFactoryBean<ObjectSerializer>
{

    private final MuleContext muleContext;

    public DefaultObjectSerializerFactoryBean(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public ObjectSerializer getObject() throws Exception
    {
        return muleContext.getObjectSerializer();
    }

    @Override
    public Class<?> getObjectType()
    {
        return ObjectSerializer.class;
    }

    @Override
    public boolean isPrototype()
    {
        return false;
    }

    @Override
    public boolean isEagerInit()
    {
        return true;
    }


    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
