/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.module.db.internal.config.domain.param;

import org.mule.module.db.internal.resolver.param.ParamValueResolver;
import org.mule.module.db.internal.resolver.param.StaticParamValueResolver;

import org.springframework.beans.factory.FactoryBean;

public class StaticQueryParamResolverFactoryBean implements FactoryBean<ParamValueResolver>
{

    @Override
    public ParamValueResolver getObject() throws Exception
    {
        return new StaticParamValueResolver();
    }

    @Override
    public Class<?> getObjectType()
    {
        return ParamValueResolver.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

}
