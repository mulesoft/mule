/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.ResultSet;

import org.junit.After;
import org.junit.Test;

@SmallTest
public class StatementStreamingResultSetCloserTestCase extends AbstractMuleTestCase
{

    private final StatementStreamingResultSetCloser resultSetCloser = new StatementStreamingResultSetCloser();
    private final DbConnection connection = mock(DbConnection.class);
    private final ResultSet resultSet1 = mock(ResultSet.class);
    private final ResultSet resultSet2 = mock(ResultSet.class);

    @After
    public void after() {
        assertThat(resultSetCloser.getLocksCount(), is(0));
    }

    @Test
    public void closesRegisteredResultSet() throws Exception
    {
        resultSetCloser.trackResultSet(connection, resultSet1);

        resultSetCloser.close(connection, resultSet1);

        verify(resultSet1).close();
    }

    @Test
    public void tracksMultipleResultSetFromConnection() throws Exception
    {
        resultSetCloser.trackResultSet(connection, resultSet1);
        resultSetCloser.trackResultSet(connection, resultSet2);

        resultSetCloser.close(connection, resultSet1);
        resultSetCloser.close(connection, resultSet2);

        verify(resultSet1).close();
        verify(resultSet2).close();
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenClosingUnTrackedConnection() throws Exception
    {
        resultSetCloser.close(connection, resultSet1);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenClosingUnTrackedResultSet() throws Exception
    {
        resultSetCloser.trackResultSet(connection, resultSet1);

        resultSetCloser.close(connection, resultSet1);
        resultSetCloser.close(connection, resultSet1);
    }
}
