/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.parser.QueryTemplateParser;

/**
 * Base class for {@link BulkQueryResolver} implementations
 */
public abstract class AbstractBulkQueryResolver implements BulkQueryResolver
{

    private static final String BULK_QUERY_SEPARATOR = ";[\\r\\n]+";

    protected final String bulkQueryText;
    private final QueryTemplateParser parser;

    public AbstractBulkQueryResolver(String bulkQueryText, QueryTemplateParser queryTemplateParser)
    {
        this.bulkQueryText = bulkQueryText;
        this.parser = queryTemplateParser;
    }

    @Override
    public BulkQuery resolve(MuleEvent muleEvent)
    {
        if (muleEvent == null)
        {
            return null;
        }

        BulkQuery bulkQuery = createBulkQuery(muleEvent);

        if (bulkQuery.getQueryTemplates().size() == 0)
        {
            throw new QueryResolutionException("There are no queries on the resolved dynamic bulk query: " + this.bulkQueryText);
        }

        return bulkQuery;
    }

    protected String resolveBulkQueries(MuleEvent muleEvent, String bulkQuery)
    {
        return bulkQuery.trim();
    }

    protected BulkQuery createBulkQuery(MuleEvent muleEvent)
    {
        String queries = resolveBulkQueries(muleEvent, this.bulkQueryText);

        BulkQuery bulkQuery = new BulkQuery();

        String[] splitQueries = queries.split(BULK_QUERY_SEPARATOR);
        for (String sql : splitQueries)
        {
            if ("".equals(sql.trim()))
            {
                continue;
            }

            QueryTemplate queryTemplate = parser.parse(sql);
            bulkQuery.add(queryTemplate);
        }

        return bulkQuery;
    }
}
