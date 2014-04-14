/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.parser.QueryTemplateParser;

/**
 * Resolves a bulk query to a static value without using the current event
 */
public class StaticBulkQueryResolver extends AbstractBulkQueryResolver
{

    private BulkQuery bulkQuery;

    public StaticBulkQueryResolver(String bulkQuery, QueryTemplateParser queryTemplateParser)
    {
        super(bulkQuery, queryTemplateParser);
    }

    @Override
    protected BulkQuery createBulkQuery(MuleEvent muleEvent)
    {
        if (bulkQuery == null)
        {
            synchronized (this)
            {
                if (bulkQuery == null)
                {
                    bulkQuery = super.createBulkQuery(muleEvent);
                }
            }
        }

        return bulkQuery;
    }
}
