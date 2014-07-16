/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.module.db.internal.parser.QueryTemplateParsingException;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DynamicQueryResolverTestCase extends AbstractQueryResolverTestCase
{

    private final Query query = createQuery(createQueryTemplate(DYNAMIC_SQL_TEXT));

    @Test
    public void resolvesDynamicQuery() throws Exception
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        String staticSqlText = STATIC_SQL_TEXT;
        when(expressionManager.parse(DYNAMIC_SQL_TEXT, muleEvent)).thenReturn(staticSqlText);

        QueryTemplate expectedQueryTemplate = createQueryTemplate(staticSqlText);
        QueryTemplateParser queryTemplateParser = mock(QueryTemplateParser.class);
        when(queryTemplateParser.parse(staticSqlText)).thenReturn(expectedQueryTemplate);

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, queryTemplateParser, expressionManager);

        Query resolvedQuery = queryResolver.resolve(null, muleEvent);

        assertThat(expectedQueryTemplate, sameInstance(resolvedQuery.getQueryTemplate()));
    }

    @Test(expected = QueryResolutionException.class)
    public void throwsErrorOnParsingError() throws Exception
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        String staticSqlText = STATIC_SQL_TEXT;
        when(expressionManager.parse(DYNAMIC_SQL_TEXT, muleEvent)).thenReturn(staticSqlText);

        QueryTemplateParser queryTemplateParser = mock(QueryTemplateParser.class);
        when(queryTemplateParser.parse(staticSqlText)).thenThrow(new QueryTemplateParsingException("Parse error"));

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, queryTemplateParser, expressionManager);

        queryResolver.resolve(null, muleEvent);
    }

    @Test(expected = QueryResolutionException.class)
    public void throwsErrorOnExpressionEvaluationError() throws Exception
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(expressionManager.parse(DYNAMIC_SQL_TEXT, muleEvent)).thenThrow(new ExpressionRuntimeException(CoreMessages.createStaticMessage("Error")));

        DynamicQueryResolver queryResolver = new DynamicQueryResolver(query, null, expressionManager);

        queryResolver.resolve(null, muleEvent);
    }
}
