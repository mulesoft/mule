/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.domain.query.QueryParamValue;
import org.mule.module.db.domain.query.QueryTemplate;
import org.mule.module.db.resolver.param.QueryParamResolver;

import java.util.List;

/**
 * Resolves a parameterized query evaluating parameter value expression using a given event
 */
public class ParametrizedQueryResolver implements QueryResolver
{

    private final Query query;
    private final QueryParamResolver queryParamResolver;

    public ParametrizedQueryResolver(Query query, QueryParamResolver queryParamResolver)
    {
        this.query = query;
        this.queryParamResolver = queryParamResolver;
    }

    @Override
    public Query resolve(MuleEvent muleEvent)
    {
        if (muleEvent == null)
        {
            return query;
        }

        List<QueryParamValue> resolvedParams = queryParamResolver.resolveParams(muleEvent, query.getParamValues());

        QueryTemplate queryTemplate = query.getQueryTemplate();

        return new Query(queryTemplate, resolvedParams);
    }
}