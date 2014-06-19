/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.sql.executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.executor.SelectExecutor;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.statement.StatementFactory;
import org.mule.module.db.internal.result.resultset.ResultSetHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class QueryTestCase extends AbstractMuleTestCase
{

    @Test
    public void testQueryReturnsTheExpectedResult() throws Exception
    {
        ResultSet resultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        String sqlText = "SELECT * FROM dummy";
        when(statement.executeQuery(sqlText)).thenReturn(resultSet);
        StatementFactory statementFactory = mock(StatementFactory.class);

        DbConnection connection = mock(DbConnection.class);

        ResultSetHandler resultHandler = mock(ResultSetHandler.class);
        List<Object> processedResult = new ArrayList<Object>();
        when(resultHandler.processResultSet(connection, resultSet)).thenReturn(processedResult);
        SelectExecutor selectExecutor = new SelectExecutor(statementFactory, resultHandler);

        QueryTemplate queryTemplate = new QueryTemplate(sqlText, QueryType.SELECT, Collections.<QueryParam>emptyList());
        Mockito.when(statementFactory.create(connection, queryTemplate)).thenReturn(statement);

        Query query = new Query(queryTemplate, null);
        Object result = selectExecutor.execute(connection, query);

        assertEquals(processedResult, result);
    }
}
