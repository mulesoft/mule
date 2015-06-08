/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.transaction.Transaction;
import org.mule.module.db.internal.domain.transaction.DbTransactionManager;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

@SmallTest
public class TransactionalDbConnectionFactoryTestCase extends AbstractMuleTestCase
{

    private TransactionalDbConnectionFactory factory;
    private DataSource datasource = mock(DataSource.class);
    private DbTransactionManager dbTransactionManager = mock(DbTransactionManager.class);
    private ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

    @Test
    public void createsConnectionWhenJoinIfPossibleAndNoActiveTransaction() throws Exception
    {
        Connection expectedConnection = mock(Connection.class);
        when(connectionFactory.create(datasource)).thenReturn(expectedConnection);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, connectionFactory, datasource);
        DbConnection connection = factory.createConnection(TransactionalAction.JOIN_IF_POSSIBLE);

        assertWrappedConnection(connection, expectedConnection);
    }

    @Test
    public void createsConnectionWhenJoinIfPossibleAndActiveTransaction() throws Exception
    {
        Connection expectedConnection = mock(Connection.class);
        when(connectionFactory.create(datasource)).thenReturn(expectedConnection);

        Transaction transaction = mock(Transaction.class);
        when(transaction.hasResource(datasource)).thenReturn(false);

        when(dbTransactionManager.getTransaction()).thenReturn(transaction);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, connectionFactory, datasource);

        DbConnection connection = factory.createConnection(TransactionalAction.JOIN_IF_POSSIBLE);

        assertWrappedConnection(connection, expectedConnection);
    }

    @Test
    public void createsConnectionWhenNoSupportedAndNoActiveTransaction() throws Exception
    {
        Connection expectedConnection = mock(Connection.class);
        when(connectionFactory.create(datasource)).thenReturn(expectedConnection);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, connectionFactory, datasource);

        DbConnection connection = factory.createConnection(TransactionalAction.NOT_SUPPORTED);

        assertWrappedConnection(connection, expectedConnection);
    }

    @Test(expected = IllegalStateException.class)
    public void failsWhenAlwaysJoinAndNoActiveTransaction() throws Exception
    {
        Connection expectedConnection = mock(Connection.class);
        when(datasource.getConnection()).thenReturn(expectedConnection);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, datasource);

        factory.createConnection(TransactionalAction.ALWAYS_JOIN);
    }

    @Test
    public void usesConnectionFromActiveTransactionWhenJoinIfPossible() throws Exception
    {
        doUseActiveTransaction(TransactionalAction.JOIN_IF_POSSIBLE);
    }

    private void doUseActiveTransaction(TransactionalAction transactionalAction) throws SQLException
    {
        Connection expectedConnection = mock(Connection.class);

        Transaction transaction = mock(Transaction.class);
        when(transaction.hasResource(datasource)).thenReturn(true);
        when(transaction.getResource(datasource)).thenReturn(expectedConnection);

        when(dbTransactionManager.getTransaction()).thenReturn(transaction);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, datasource);
        DbConnection connection = factory.createConnection(transactionalAction);

        assertWrappedConnection(connection, expectedConnection);

        verify(datasource, times(0)).getConnection();
    }

    @Test
    public void usesConnectionFromActiveTransactionWhenAlwaysJoin() throws Exception
    {
        doUseActiveTransaction(TransactionalAction.ALWAYS_JOIN);
    }

    @Test
    public void bindsConnectionToActiveTransaction() throws Exception
    {
        Connection expectedConnection = mock(Connection.class);
        when(connectionFactory.create(datasource)).thenReturn(expectedConnection);

        Transaction transaction = mock(Transaction.class);
        when(transaction.hasResource(datasource)).thenReturn(false);

        when(dbTransactionManager.getTransaction()).thenReturn(transaction);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, connectionFactory, datasource);

        DbConnection connection = factory.createConnection(TransactionalAction.JOIN_IF_POSSIBLE);

        assertWrappedConnection(connection, expectedConnection);

        verify(transaction, times(1)).bindResource(datasource, expectedConnection);
    }

    @Test
    public void closesConnectionWhenNotSupported() throws Exception
    {
        when(dbTransactionManager.getTransaction()).thenReturn(null);
        DbConnection connection = mock(DbConnection.class);
        when(connection.getTransactionalAction()).thenReturn(TransactionalAction.NOT_SUPPORTED);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, null);

        factory.releaseConnection(connection);

        verify(connection, times(1)).commit();
        verify(connection, times(1)).close();
    }

    @Test(expected = ConnectionCommitException.class)
    public void closesConnectionWhenNotSupportedAndCommitFails() throws Exception
    {
        when(dbTransactionManager.getTransaction()).thenReturn(null);
        DbConnection connection = mock(DbConnection.class);
        when(connection.getTransactionalAction()).thenReturn(TransactionalAction.NOT_SUPPORTED);
        doThrow(new SQLException()).when(connection).commit();

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, null);

        factory.releaseConnection(connection);

        verify(connection, times(1)).commit();
        verify(connection, times(1)).close();
    }

    @Test
    public void closesConnectionWhenJoinIfPossibleAndNoActiveTransaction() throws Exception
    {
        when(dbTransactionManager.getTransaction()).thenReturn(null);
        DbConnection connection = mock(DbConnection.class);
        when(connection.getTransactionalAction()).thenReturn(TransactionalAction.JOIN_IF_POSSIBLE);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, null);

        factory.releaseConnection(connection);

        verify(connection, times(1)).commit();
        verify(connection, times(1)).close();
    }

    @Test
    public void keepsTransactedConnectionOpenWhenJoinIfPossibleAndActiveTransaction() throws Exception
    {
        Transaction transaction = mock(Transaction.class);
        when(dbTransactionManager.getTransaction()).thenReturn(transaction);
        DbConnection connection = mock(DbConnection.class);
        when(connection.getTransactionalAction()).thenReturn(TransactionalAction.JOIN_IF_POSSIBLE);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, null);

        factory.releaseConnection(connection);

        verify(connection, times(0)).commit();
        verify(connection, times(0)).close();
    }

    @Test
    public void keepsTransactedConnectionOpenWhenAlwaysJoin() throws Exception
    {
        Transaction transaction = mock(Transaction.class);
        when(dbTransactionManager.getTransaction()).thenReturn(transaction);
        DbConnection connection = mock(DbConnection.class);
        when(connection.getTransactionalAction()).thenReturn(TransactionalAction.ALWAYS_JOIN);

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, null, null);

        factory.releaseConnection(connection);

        verify(connection, times(0)).commit();
        verify(connection, times(0)).close();
    }

    @Test(expected = SQLException.class)
    public void managesConnectionCreationException() throws Exception
    {
        when(connectionFactory.create(datasource)).thenThrow(new ConnectionCreationException("Error"));

        factory = new TransactionalDbConnectionFactory(dbTransactionManager, null, connectionFactory, datasource);
        factory.createConnection(TransactionalAction.NOT_SUPPORTED);
    }

    private void assertWrappedConnection(DbConnection connection, Connection wrappedConnection) throws SQLException
    {
        final String sqlText = "select * from test";

        connection.prepareStatement(sqlText);
        verify(wrappedConnection).prepareStatement(sqlText);
    }
}
