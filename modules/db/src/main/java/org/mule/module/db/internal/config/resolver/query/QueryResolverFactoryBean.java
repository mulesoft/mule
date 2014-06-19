/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.resolver.query;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.param.ParamValueResolver;
import org.mule.module.db.internal.resolver.query.DynamicQueryResolver;
import org.mule.module.db.internal.resolver.query.ParametrizedQueryResolver;
import org.mule.module.db.internal.resolver.query.QueryResolver;
import org.mule.module.db.internal.resolver.query.StaticQueryResolver;

import org.springframework.beans.factory.FactoryBean;

public class QueryResolverFactoryBean implements FactoryBean<QueryResolver>, MuleContextAware
{

    private final Query query;
    private final ParamValueResolver paramValueResolver;
    private final DbConfigResolver dbConfigResolver;
    private MuleContext muleContext;

    public QueryResolverFactoryBean(Query query, ParamValueResolver paramValueResolver, DbConfigResolver dbConfigResolver)
    {
        this.query = query;
        this.paramValueResolver = paramValueResolver;
        this.dbConfigResolver = dbConfigResolver;
    }

    @Override
    public QueryResolver getObject() throws Exception
    {
        if (isDynamic(query))
        {
            return new DynamicQueryResolver(query, new SimpleQueryTemplateParser(), muleContext.getExpressionManager());
        }
        else if (hasParameters(query))
        {
            return new ParametrizedQueryResolver(query, paramValueResolver);
        }
        else
        {
            return new StaticQueryResolver(query);
        }
    }

    private boolean isDynamic(Query query)
    {
        return query.getQueryTemplate().isDynamic();
    }

    private boolean hasParameters(Query query)
    {
        return query.getQueryTemplate().getParams().size() > 0;
    }


    @Override
    public Class<?> getObjectType()
    {
        return QueryResolver.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
