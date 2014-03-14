/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.query;

import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.domain.query.QueryTemplate;
import org.mule.module.db.parser.QueryTemplateParser;
import org.mule.module.db.parser.QueryTemplateParsingException;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DynamicQueryResolverTestCase extends AbstractQueryResolverTestCase
{

    @Test
    public void returnsOriginalQueryWhenEventIsNull() throws Exception
    {
        Query query = createSelectQuery(STATIC_SQL_TEXT, null);

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, null, null);

        Query resolvedQuery = queryResolver.resolve(null);

        assertThat(query, sameInstance(resolvedQuery));
    }

    @Test
    public void resolvesDynamicQuery() throws Exception
    {
        Query query = createSelectQuery(DYNAMIC_SQL_TEXT, null);

        ExpressionManager expressionManager = mock(ExpressionManager.class);
        String staticSqlText = STATIC_SQL_TEXT;
        when(expressionManager.parse(DYNAMIC_SQL_TEXT, muleEvent)).thenReturn(staticSqlText);

        QueryTemplate expectedQueryTemplate = createSelectQueryTemplate(staticSqlText);
        QueryTemplateParser queryTemplateParser = mock(QueryTemplateParser.class);
        when(queryTemplateParser.parse(staticSqlText)).thenReturn(expectedQueryTemplate);

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, queryTemplateParser, expressionManager);

        Query resolvedQuery = queryResolver.resolve(muleEvent);

        assertThat(expectedQueryTemplate, sameInstance(resolvedQuery.getQueryTemplate()));
    }

    @Test(expected = QueryResolutionException.class)
    public void throwsErrorOnParsingError() throws Exception
    {
        Query query = createSelectQuery(DYNAMIC_SQL_TEXT, null);

        ExpressionManager expressionManager = mock(ExpressionManager.class);
        String staticSqlText = STATIC_SQL_TEXT;
        when(expressionManager.parse(DYNAMIC_SQL_TEXT, muleEvent)).thenReturn(staticSqlText);

        QueryTemplateParser queryTemplateParser = mock(QueryTemplateParser.class);
        when(queryTemplateParser.parse(staticSqlText)).thenThrow(new QueryTemplateParsingException("Parse error"));

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, queryTemplateParser, expressionManager);

        queryResolver.resolve(muleEvent);
    }

    @Test(expected = QueryResolutionException.class)
    public void throwsErrorOnExpressionEvaluationError() throws Exception
    {
        Query query = createSelectQuery(DYNAMIC_SQL_TEXT, null);

        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(expressionManager.parse(DYNAMIC_SQL_TEXT, muleEvent)).thenThrow(new ExpressionRuntimeException(CoreMessages.createStaticMessage("Error")));

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, null, expressionManager);

        queryResolver.resolve(muleEvent);
    }
}
