/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.module.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.module.db.internal.resolver.param.ParamValueResolver;
import org.mule.module.db.internal.resolver.query.BulkQueryResolver;
import org.mule.module.db.internal.resolver.query.StaticBulkQueryResolver;
import org.mule.module.db.internal.resolver.query.DynamicBulkQueryResolver;

import org.springframework.beans.factory.FactoryBean;

public class BulkQueryResolverFactoryBean implements FactoryBean<BulkQueryResolver>, MuleContextAware
{

    private final String bulkQuery;
    private MuleContext context;

    public BulkQueryResolverFactoryBean(String bulkQuery)
    {
        this.bulkQuery = bulkQuery;
    }

    @Override
    public BulkQueryResolver getObject() throws Exception
    {
        if (context.getExpressionManager().isExpression(bulkQuery))
        {
            return new DynamicBulkQueryResolver(bulkQuery, new SimpleQueryTemplateParser(), context.getExpressionManager());
        }
        else
        {
            return new StaticBulkQueryResolver(bulkQuery, new SimpleQueryTemplateParser());
        }
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

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }
}
