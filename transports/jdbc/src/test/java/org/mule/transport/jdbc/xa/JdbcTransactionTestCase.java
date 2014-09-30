/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.jdbc.JdbcTransaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class JdbcTransactionTestCase extends AbstractMuleTestCase
{

    private JdbcTransaction tx;
    private Connection connection;

    @Before
    public void initializeMocks() throws SQLException, TransactionException
    {
        tx = new JdbcTransaction(mock(MuleContext.class));

        connection = mock(Connection.class);

        when(connection.getAutoCommit()).thenReturn(false);

        tx.bindResource(mock(DataSource.class), connection);
    }

    @After
    public void ensureConnectionCloseWasCalled() throws SQLException
    {
        verify(connection).close();
    }

    @Test(expected = TransactionException.class)
    public void closeConnectionIfErrorOnCommit() throws Exception
    {
        doThrow(new SQLException()).when(connection).commit();
        tx.commit();
    }

    @Test(expected = TransactionException.class)
    public void closeConnectionIfErrorOnRollback() throws Exception
    {
        doThrow(new SQLException()).when(connection).rollback();
        tx.rollback();
    }
}
