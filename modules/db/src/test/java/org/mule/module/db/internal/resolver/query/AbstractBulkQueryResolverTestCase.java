/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;

public class AbstractBulkQueryResolverTestCase extends AbstractMuleTestCase
{

    public static final String STATIC_SQL_1 = "delete from test1";
    public static final String STATIC_SQL_2 = "delete from test2";
    public static final String BULK_SQL_QUERY = STATIC_SQL_1 + ";\n" + STATIC_SQL_2;

    protected final MuleEvent muleEvent = mock(MuleEvent.class);

    protected QueryTemplateParser createQueryTemplateParser()
    {
        QueryTemplateParser queryTemplateParser = mock(QueryTemplateParser.class);
        when(queryTemplateParser.parse(STATIC_SQL_1)).thenReturn(new QueryTemplate(STATIC_SQL_1, QueryType.DELETE, Collections.<QueryParam>emptyList()));
        when(queryTemplateParser.parse(STATIC_SQL_2)).thenReturn(new QueryTemplate(STATIC_SQL_2, QueryType.DELETE, Collections.<QueryParam>emptyList()));

        return queryTemplateParser;
    }

    protected void assertResolvedBulkQuery(BulkQuery resolvedBulkQuery)
    {
        assertThat(resolvedBulkQuery.getQueryTemplates().size(), equalTo(2));
        assertThat(resolvedBulkQuery.getQueryTemplates().get(0).getSqlText(), equalTo(STATIC_SQL_1));
        assertThat(resolvedBulkQuery.getQueryTemplates().get(1).getSqlText(), equalTo(STATIC_SQL_2));
    }
}
