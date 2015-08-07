/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.transaction;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DbTransactionTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private SQLException expectedExceptionCause;

    private DbTransaction tx;
    private Connection connection;

    private MuleContext muleContext;

    @Before
    public void initializeMocks() throws SQLException, TransactionException
    {
        muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        when(muleContext.getConfiguration().getId()).thenReturn("myApp");

        tx = new DbTransaction(muleContext);

        connection = mock(Connection.class);

        when(connection.getAutoCommit()).thenReturn(false);

        tx.bindResource(mock(DataSource.class), connection);
    }

    @After
    public void ensureConnectionCloseWasCalled() throws SQLException
    {
        verify(connection).close();
    }

    @Test
    public void closeConnectionIfErrorOnCommit() throws Exception
    {
        addExceptionExpectation();
        doThrow(expectedExceptionCause).when(connection).commit();
        tx.commit();
    }

    @Test
    public void closeConnectionIfErrorOnRollback() throws Exception
    {
        addExceptionExpectation();
        doThrow(expectedExceptionCause).when(connection).rollback();
        tx.rollback();
    }

    @Test
    public void closeConnectionIfCommitOk() throws Exception
    {
        tx.commit();
    }

    @Test
    public void closeConnectionIfRollbackOk() throws Exception
    {
        tx.rollback();
    }

    @Test
    public void raiseCloseExceptionAfterCommitOk() throws Exception
    {
        addExceptionExpectation();
        doThrow(expectedExceptionCause).when(connection).close();
        tx.commit();
    }

    @Test
    public void raiseCloseExceptionAfterRollbackOk() throws Exception
    {
        addExceptionExpectation();
        doThrow(expectedExceptionCause).when(connection).close();
        tx.rollback();
    }

    @Test
    public void raiseCommitExceptionIfCommitAndCloseFail() throws Exception
    {
        addExceptionExpectation();
        doThrow(expectedExceptionCause).when(connection).commit();
        doThrow(new SQLException()).when(connection).close();
        tx.commit();
    }

    @Test
    public void raiseRollbackExceptionIfRollbackAndCloseFail() throws Exception
    {
        addExceptionExpectation();
        doThrow(expectedExceptionCause).when(connection).rollback();
        doThrow(new SQLException()).when(connection).close();
        tx.rollback();
    }

    private void addExceptionExpectation()
    {
        expectedExceptionCause = new SQLException();
        expectedException.expect(TransactionException.class);
        expectedException.expectCause(is(expectedExceptionCause));
    }
}
