/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.resolver.query;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.domain.query.QueryType;
import org.mule.module.db.parser.SimpleQueryTemplateParser;
import org.mule.module.db.resolver.database.DbConfigResolver;
import org.mule.module.db.resolver.param.QueryParamResolver;
import org.mule.module.db.resolver.query.DynamicQueryResolver;
import org.mule.module.db.resolver.query.ParametrizedQueryResolver;
import org.mule.module.db.resolver.query.QueryResolver;
import org.mule.module.db.resolver.query.StaticQueryResolver;
import org.mule.module.db.resolver.query.StoredProcedureQueryResolver;

import org.springframework.beans.factory.FactoryBean;

public class QueryResolverFactoryBean implements FactoryBean<QueryResolver>, MuleContextAware
{

    private final Query query;
    private final QueryParamResolver queryParamResolver;
    private final DbConfigResolver dbConfigResolver;
    private MuleContext muleContext;

    public QueryResolverFactoryBean(Query query, QueryParamResolver queryParamResolver, DbConfigResolver dbConfigResolver)
    {
        this.query = query;
        this.queryParamResolver = queryParamResolver;
        this.dbConfigResolver = dbConfigResolver;
    }

    @Override
    public QueryResolver getObject() throws Exception
    {
        if (isDynamic(query))
        {
            return new DynamicQueryResolver(query, new SimpleQueryTemplateParser(), muleContext.getExpressionManager());
        }
        else if (query.getQueryTemplate().getType() == QueryType.STORE_PROCEDURE_CALL)
        {
            return new StoredProcedureQueryResolver(query, dbConfigResolver, queryParamResolver);
        }
        else if (hasParameters(query))
        {
            return new ParametrizedQueryResolver(query, queryParamResolver);
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
