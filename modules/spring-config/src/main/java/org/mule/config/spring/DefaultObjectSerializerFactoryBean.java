/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.serialization.DefaultObjectSerializer;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.serialization.internal.DefaultObjectSerializerWrapper;

import org.springframework.beans.factory.SmartFactoryBean;

/**
 * An eager {@link SmartFactoryBean} which returns the {@link ObjectSerializer} which
 * got configured as the {@link MuleContext}'s default by invoking
 * {@link MuleContext#getObjectSerializer()}
 * <p/>
 * The default {@link ObjectSerializer} is wrapped in a
 * {@link DefaultObjectSerializerWrapper} so that the instance returned by this
 * factory is one annotated with {@link DefaultObjectSerializer}. Ideally, that
 * shouldn't be necessary since this class also has that annotation and that should
 * be enough. However, due to <a href="https://jira.spring.io/browse/SPR-12914">
 * Spring issue SPR-12914</a> we have to annotate both classes, since Spring
 * will only consider the annotation in this class when injecting in runtime,
 * while the annotation on {@link DefaultObjectSerializerWrapper} will only be
 * considered at startup time.
 * <p/>
 * When Spring fixes
 * that issue we can get rid of the {@link DefaultObjectSerializerWrapper}
 * and move the {@link DefaultObjectSerializer} annotation to this one
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
        ObjectSerializer serializer = new DefaultObjectSerializerWrapper(muleContext.getObjectSerializer());
        if (muleContext instanceof DefaultMuleContext)
        {
            ((DefaultMuleContext) muleContext).setObjectSerializer(serializer);
        }

        return serializer;
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
