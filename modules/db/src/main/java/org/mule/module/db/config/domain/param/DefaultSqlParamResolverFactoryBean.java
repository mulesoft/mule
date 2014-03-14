/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.domain.param;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

import org.mule.module.db.resolver.param.DynamicQueryParamResolver;
import org.mule.module.db.resolver.param.QueryParamResolver;

import org.springframework.beans.factory.FactoryBean;

public class DefaultSqlParamResolverFactoryBean implements FactoryBean<QueryParamResolver>, MuleContextAware
{

    private MuleContext context;

    @Override
    public QueryParamResolver getObject() throws Exception
    {
        return new DynamicQueryParamResolver(context.getExpressionManager());
    }

    @Override
    public Class<?> getObjectType()
    {
        return QueryParamResolver.class;
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
