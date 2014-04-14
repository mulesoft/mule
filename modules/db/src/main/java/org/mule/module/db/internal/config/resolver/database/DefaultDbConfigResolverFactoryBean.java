/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.resolver.database;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

import org.mule.module.db.internal.resolver.database.DefaultDbConfigResolver;

import org.springframework.beans.factory.FactoryBean;

public class DefaultDbConfigResolverFactoryBean implements FactoryBean<DefaultDbConfigResolver>, MuleContextAware
{

    private MuleContext context;

    @Override
    public DefaultDbConfigResolver getObject() throws Exception
    {
        return new DefaultDbConfigResolver(context.getRegistry());
    }

    @Override
    public Class<?> getObjectType()
    {
        return DefaultDbConfigResolver.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }
}
