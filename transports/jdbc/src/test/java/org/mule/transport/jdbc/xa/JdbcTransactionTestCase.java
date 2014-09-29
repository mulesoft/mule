/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import static junit.framework.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.transport.jdbc.JdbcTransaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

public class JdbcTransactionTestCase
{

    @Test
    public void closeConnectionIfErrorOnCommit() throws Exception
    {
        performTest(Operation.COMMIT);
    }

    @Test
    public void closeConnectionIfErrorOnRollback() throws Exception
    {
        performTest(Operation.ROLLBACK);
    }

    private void performTest(Operation op) throws SQLException, TransactionException
    {
        JdbcTransaction tx = new JdbcTransaction(mock(MuleContext.class));

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(connection.getAutoCommit()).thenReturn(false);

        tx.bindResource(dataSource, connection);

        switch(op)
        {
            case COMMIT: doThrow(new SQLException()).when(connection).commit(); break;
            case ROLLBACK: doThrow(new SQLException()).when(connection).rollback(); break;
        }
        doThrow(new SQLException()).when(connection).rollback();

        try{
            switch(op)
            {
                case COMMIT: tx.commit(); break;
                case ROLLBACK: tx.rollback(); break;
            }
            fail();
        } catch (TransactionException e)
        {
            e.printStackTrace();
            // Expected
        }

        verify(connection).close();
    }

    enum Operation
    {
        COMMIT,
        ROLLBACK;
    }
}
