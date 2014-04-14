/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.module.db.internal.parser.QueryTemplateParser;

/**
 * Resolves a bulk query evaluating expression using a given event
 */
public class DynamicBulkQueryResolver extends AbstractBulkQueryResolver
{

    private final ExpressionManager expressionManager;

    public DynamicBulkQueryResolver(String bulkQuery, QueryTemplateParser queryTemplateParser, ExpressionManager expressionManager)
    {
        super(bulkQuery, queryTemplateParser);
        this.expressionManager = expressionManager;
    }

    @Override
    protected String resolveBulkQueries(MuleEvent muleEvent, String bulkQuery)
    {
        return expressionManager.parse(bulkQuery.trim(), muleEvent);
    }
}
