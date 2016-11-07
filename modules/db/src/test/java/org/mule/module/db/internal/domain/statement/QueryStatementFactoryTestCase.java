/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.statement;

import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;
import static java.sql.ResultSet.TYPE_SCROLL_SENSITIVE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.domain.query.QueryType.STORE_PROCEDURE_CALL;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class QueryStatementFactoryTestCase extends AbstractMuleTestCase
{
    private final String sqlText = "call test";
    private CallableStatement createdStatement;
    private DatabaseMetaData metadata;
    private DbConnection connection;

    @Before
    public void setUp() throws Exception
    {
        createdStatement = mock(CallableStatement.class);
        metadata = mock(DatabaseMetaData.class);
        connection = mock(DbConnection.class);
    }

    @Test
    public void createsPreparedStatementsWithScrolling() throws SQLException
    {
        when(metadata.supportsResultSetType(TYPE_SCROLL_INSENSITIVE)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.prepareCall(sqlText, TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY)).thenReturn(createdStatement);

        checkStatementCreated(connection);
    }

    @Test
    public void createsPreparedStatementsWithForwardOnly() throws SQLException
    {
        when(metadata.supportsResultSetType(TYPE_SCROLL_INSENSITIVE)).thenReturn(false);
        when(metadata.supportsResultSetType(TYPE_SCROLL_SENSITIVE)).thenReturn(false);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.prepareCall(sqlText, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY)).thenReturn(createdStatement);

        checkStatementCreated(connection);
    }

    private void checkStatementCreated(DbConnection connection) throws SQLException
    {
        QueryStatementFactory factory = new QueryStatementFactory();
        QueryTemplate queryTemplate = new QueryTemplate(sqlText, STORE_PROCEDURE_CALL, Collections.<QueryParam>emptyList());
        CallableStatement statement = (CallableStatement) factory.create(connection, queryTemplate);

        assertThat(statement, is(createdStatement));
    }
}
