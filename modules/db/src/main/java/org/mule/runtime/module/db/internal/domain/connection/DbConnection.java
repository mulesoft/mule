/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.result.resultset.ResultSetHandler;
import org.mule.module.db.internal.result.statement.StatementResultIterator;
import org.mule.module.db.internal.result.statement.StatementResultIteratorFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Wraps a {@link Connection} adding connector's specific functionality
 */
public interface DbConnection extends Connection
{

    /**
     * Returns the {@link StatementResultIteratorFactory} used to create
     * the {@link StatementResultIterator} for this connection.
     *
     * @param resultSetHandler used to process resultSets created from this connection
     * @return the {@link StatementResultIterator} for this connection.
     */
    StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler);

    /**
     * Determines actual parameter types for the parameters defined in a
     * query template.
     *
     * @param queryTemplate query template that needing parameter resolution
     * @return a not null map containing the parameter type for each parameter index
     * @throws SQLException when there are error processing the query
     */
    Map<Integer, DbType> getParamTypes(QueryTemplate queryTemplate) throws SQLException;

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
