/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.domain.connection;

import org.mule.module.db.result.statement.GenericStatementResultIteratorFactory;
import org.mule.module.db.result.resultset.ResultSetHandler;
import org.mule.module.db.result.statement.StatementResultIteratorFactory;
import org.mule.module.db.domain.transaction.TransactionalAction;

import java.sql.Connection;

/**
 * Implements connector side of {@link DbConnection}
 */
public abstract class AbstractDbConnection implements DbConnection
{

    private final TransactionalAction transactionalAction;
    private final DefaultDbConnectionReleaser connectionReleaseListener;
    protected final Connection delegate;

    public AbstractDbConnection(Connection delegate, TransactionalAction transactionalAction, DefaultDbConnectionReleaser connectionReleaseListener)
    {
        this.delegate = delegate;
        this.transactionalAction = transactionalAction;
        this.connectionReleaseListener = connectionReleaseListener;
    }

    @Override
    public Connection getDelegate()
    {
        return delegate;
    }

    @Override
    public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler)
    {
        return new GenericStatementResultIteratorFactory(resultSetHandler);
    }

    @Override
    public TransactionalAction getTransactionalAction()
    {
        return transactionalAction;
    }

    @Override
    public void release()
    {
        connectionReleaseListener.release(this);
    }
}
