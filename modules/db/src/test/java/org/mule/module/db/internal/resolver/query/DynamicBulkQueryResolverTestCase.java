/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.expression.ExpressionManager;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DynamicBulkQueryResolverTestCase extends AbstractBulkQueryResolverTestCase
{

    public static final String DYNAMIC_BULK_QUERY = "#[bulkQuery]";

    @Test
    public void doesNotResolvesBulkQueryWhenThereIsNoEvent() throws Exception
    {
        DynamicBulkQueryResolver bulkQueryResolver = new DynamicBulkQueryResolver(DYNAMIC_BULK_QUERY, null, null);

        BulkQuery resolvedBulkQuery = bulkQueryResolver.resolve(null);

        assertThat(resolvedBulkQuery, nullValue());
    }

    @Test(expected = QueryResolutionException.class)
    public void throwsErrorOnEmptyBulkQuery() throws Exception
    {
        QueryTemplateParser queryTemplateParser = createQueryTemplateParser();

        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(expressionManager.parse(DYNAMIC_BULK_QUERY, muleEvent)).thenReturn("");

        DynamicBulkQueryResolver bulkQueryResolver = new DynamicBulkQueryResolver(DYNAMIC_BULK_QUERY, queryTemplateParser, expressionManager);

        bulkQueryResolver.resolve(muleEvent);
    }

    @Test
    public void resolvesDynamicBulkQuery() throws Exception
    {
        QueryTemplateParser queryTemplateParser = createQueryTemplateParser();

        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(expressionManager.parse(DYNAMIC_BULK_QUERY, muleEvent)).thenReturn(BULK_SQL_QUERY);

        DynamicBulkQueryResolver bulkQueryResolver = new DynamicBulkQueryResolver(DYNAMIC_BULK_QUERY, queryTemplateParser, expressionManager);

        BulkQuery resolvedBulkQuery = bulkQueryResolver.resolve(muleEvent);

        assertResolvedBulkQuery(resolvedBulkQuery);
    }
}
