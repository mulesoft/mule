/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.module.db.config.domain.param;

import org.mule.module.db.resolver.param.StaticQueryParamResolver;
import org.mule.module.db.resolver.param.QueryParamResolver;

import org.springframework.beans.factory.FactoryBean;

public class StaticQueryParamResolverFactoryBean implements FactoryBean<QueryParamResolver>
{

    @Override
    public QueryParamResolver getObject() throws Exception
    {
        return new StaticQueryParamResolver();
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

}
