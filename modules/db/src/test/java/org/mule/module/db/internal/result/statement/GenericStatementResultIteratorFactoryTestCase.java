/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
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
import java.sql.Statement;
import java.util.Collections;

import org.junit.Test;

@SmallTest
public class GenericStatementResultIteratorFactoryTestCase extends AbstractMuleTestCase
{

    private final GenericStatementResultIteratorFactory resultIteratorFactory = new GenericStatementResultIteratorFactory(null);
    private final Statement statement = mock(CallableStatement.class);

    @Test(expected = IllegalArgumentException.class)
    public void createsIterator() throws Exception
    {
        DbConnection connection = createMockConnection(true);

        StatementResultIterator statementResultIterator = resultIteratorFactory.create(connection, statement, new QueryTemplate(null, STORE_PROCEDURE_CALL, Collections.<QueryParam>emptyList()), null);

        assertThat(statementResultIterator, instanceOf(StatementResultIterator.class));
    }

    private DbConnection createMockConnection(boolean supportsMultipleOpenResults) throws SQLException
    {
        DbConnection connection = mock(DbConnection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.supportsMultipleOpenResults()).thenReturn(supportsMultipleOpenResults);
        when(connection.getMetaData()).thenReturn(metaData);

        return connection;
    }
}