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
import org.mule.module.db.internal.domain.executor.UpdateExecutor;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.statement.StatementFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Statement;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class UpdateTestCase extends AbstractMuleTestCase
{

    @Test
    public void testUpdate() throws Exception
    {
        Statement statement = mock(Statement.class);
        String sqlText = "UPDATE dummy SET NAME='Mercury' WHERE id=777";
        when(statement.executeUpdate(sqlText, Statement.NO_GENERATED_KEYS)).thenReturn(1);
        StatementFactory statementFactory = mock(StatementFactory.class);
        DbConnection connection = mock(DbConnection.class);
        UpdateExecutor updateExecutor = new UpdateExecutor(statementFactory);

        QueryTemplate queryTemplate = new QueryTemplate(sqlText, QueryType.UPDATE, Collections.<QueryParam>emptyList());
        Mockito.when(statementFactory.create(connection, queryTemplate)).thenReturn(statement);
        Query query = new Query(queryTemplate, null);

        Object result = updateExecutor.execute(connection, query);

        assertEquals(1, result);
    }
}
