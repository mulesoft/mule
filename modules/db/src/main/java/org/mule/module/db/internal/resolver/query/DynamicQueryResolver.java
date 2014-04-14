/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.parser.QueryTemplateParser;

import java.util.Collections;

/**
 * Resolves a dynamic query evaluating expressions using a given event
 */
public class DynamicQueryResolver implements QueryResolver
{

    private final Query query;
    private final QueryTemplateParser queryTemplateParser;
    private final ExpressionManager expressionManager;

    public DynamicQueryResolver(Query query, QueryTemplateParser queryTemplateParser, ExpressionManager expressionManager)
    {
        this.query = query;
        this.queryTemplateParser = queryTemplateParser;
        this.expressionManager = expressionManager;
    }

    @Override
    public Query resolve(DbConnection connection, MuleEvent muleEvent)
    {
        try
        {
            QueryTemplate queryTemplate = query.getQueryTemplate();
            String resolvedSqlText = expressionManager.parse(queryTemplate.getSqlText(), muleEvent);
            queryTemplate = queryTemplateParser.parse(resolvedSqlText);

            return new Query(queryTemplate, Collections.<QueryParamValue>emptyList());
        }
        catch (RuntimeException e)
        {
            throw new QueryResolutionException("Error parsing query", e);
        }
    }
}
