/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.domain.connection;

import org.mule.module.db.domain.transaction.TransactionalAction;
import org.mule.module.db.result.resultset.ResultSetHandler;
import org.mule.module.db.result.statement.StatementResultIterator;
import org.mule.module.db.result.statement.StatementResultIteratorFactory;

import java.sql.Connection;

/**
 * Wraps a {@link Connection} adding connector's specific functionality
 */
public interface DbConnection extends Connection
{

    /**
     * Provides access to the JDBC wrapped connection
     *
     * @return the wrapped JDBC connection
     */
    Connection getDelegate();

    /**
     * Returns the {@link StatementResultIteratorFactory} used to create
     * the {@link StatementResultIterator} for this connection.
     *
     * @param resultSetHandler used to process resultSets created from this connection
     * @return the {@link StatementResultIterator} for this connection.
     */
    StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler);

    /**
     * Indicates which {@link TransactionalAction} used to create this connection

     * @return connection's transactional action
     */
    TransactionalAction getTransactionalAction();

    /**
     * Indicates that the connection is not used anymore
     */
    void release();

}
