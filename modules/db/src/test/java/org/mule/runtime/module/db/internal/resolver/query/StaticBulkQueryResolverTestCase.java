/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class StaticBulkQueryResolverTestCase extends AbstractBulkQueryResolverTestCase
{

    @Test
    public void doesNotResolvesBulkQueryWhenThereIsNoEvent() throws Exception
    {
        StaticBulkQueryResolver bulkQueryResolver = new StaticBulkQueryResolver(BULK_SQL_QUERY, null);

        BulkQuery resolvedBulkQuery = bulkQueryResolver.resolve(null);

        assertThat(resolvedBulkQuery, nullValue());
    }

    @Test(expected = QueryResolutionException.class)
    public void throwsErrorOnEmptyBulkQuery() throws Exception
    {
        StaticBulkQueryResolver bulkQueryResolver = new StaticBulkQueryResolver("", null);
        bulkQueryResolver.resolve(muleEvent);
    }

    @Test
    public void resolvesStaticBulkQuery() throws Exception
    {
        QueryTemplateParser queryTemplateParser = createQueryTemplateParser();

        StaticBulkQueryResolver bulkQueryResolver = new StaticBulkQueryResolver(BULK_SQL_QUERY, queryTemplateParser);

        BulkQuery resolvedBulkQuery = bulkQueryResolver.resolve(muleEvent);

        assertResolvedBulkQuery(resolvedBulkQuery);
    }

    @Test
    public void cachesResolvedBulkQueries() throws Exception
    {
        QueryTemplateParser queryTemplateParser = createQueryTemplateParser();

        StaticBulkQueryResolver bulkQueryResolver = new StaticBulkQueryResolver(BULK_SQL_QUERY, queryTemplateParser);

        BulkQuery resolvedBulkQuery1 = bulkQueryResolver.resolve(muleEvent);
        BulkQuery resolvedBulkQuery2 = bulkQueryResolver.resolve(muleEvent);

        assertThat(resolvedBulkQuery1, sameInstance(resolvedBulkQuery2));
        verify(queryTemplateParser, times(2)).parse(anyString());
    }

}
