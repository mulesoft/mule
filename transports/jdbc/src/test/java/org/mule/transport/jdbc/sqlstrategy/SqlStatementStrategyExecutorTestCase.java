/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.sqlstrategy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.jdbc.JdbcConnector;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class SqlStatementStrategyExecutorTestCase extends AbstractMuleTestCase
{
    public static final int TIMEOUT = 1000;
    @Mock
    private SqlStatementStrategy mockStatementStrategy;
    @Mock
    private Connection mockConnection;
    @Mock
    private JdbcConnector mockConnector;
    @Mock
    private ImmutableEndpoint mockEndpoint;
    @Mock
    private MuleEvent mockEvent;
    @Mock
    private Transaction mockTransaction;

    @Before
    public void setUpTest()
    {
        TransactionCoordination.getInstance().clear();
    }

    @Test
    public void testExecute() throws Exception
    {
        SqlStatementStrategyExecutor sqlStatementStrategyExecutor = new SqlStatementStrategyExecutor();
        sqlStatementStrategyExecutor.execute(mockStatementStrategy, mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockStatementStrategy).executeStatement(mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockConnection).commit();
        verify(mockConnection).close();
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteWithException() throws Exception
    {
        SqlStatementStrategyExecutor sqlStatementStrategyExecutor = new SqlStatementStrategyExecutor();
        when(mockStatementStrategy.executeStatement(mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection)).thenThrow(new RuntimeException());
        sqlStatementStrategyExecutor.execute(mockStatementStrategy, mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockStatementStrategy).executeStatement(mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockConnection).rollback();
        verify(mockConnection).close();
    }

    @Test
    public void testExecuteWithTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        SqlStatementStrategyExecutor sqlStatementStrategyExecutor = new SqlStatementStrategyExecutor();
        sqlStatementStrategyExecutor.execute(mockStatementStrategy, mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockStatementStrategy).executeStatement(mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockConnection, VerificationModeFactory.times(0)).commit();
        verify(mockConnection, VerificationModeFactory.times(0)).close();
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteWithExceptionWithTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        SqlStatementStrategyExecutor sqlStatementStrategyExecutor = new SqlStatementStrategyExecutor();
        when(mockStatementStrategy.executeStatement(mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection)).thenThrow(new RuntimeException());
        sqlStatementStrategyExecutor.execute(mockStatementStrategy, mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockStatementStrategy).executeStatement(mockConnector, mockEndpoint, mockEvent, TIMEOUT, mockConnection);
        verify(mockConnection, VerificationModeFactory.times(0)).rollback();
        verify(mockConnection, VerificationModeFactory.times(0)).close();
    }
}
