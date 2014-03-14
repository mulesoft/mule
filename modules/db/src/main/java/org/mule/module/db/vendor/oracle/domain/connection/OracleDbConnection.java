/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.vendor.oracle.domain.connection;

import org.mule.module.db.domain.connection.DefaultDbConnection;
import org.mule.module.db.domain.connection.DefaultDbConnectionReleaser;
import org.mule.module.db.result.resultset.ResultSetHandler;
import org.mule.module.db.result.statement.StatementResultIteratorFactory;
import org.mule.module.db.domain.transaction.TransactionalAction;
import org.mule.module.db.vendor.oracle.result.statement.OracleStatementResultIteratorFactory;

import java.sql.Connection;

/**
 * Defines a connection for Oracle databases
 */
public class OracleDbConnection extends DefaultDbConnection
{

    public OracleDbConnection(Connection delegate, TransactionalAction transactionalAction, DefaultDbConnectionReleaser connectionReleaseListener)
    {
        super(delegate, transactionalAction, connectionReleaseListener);
    }

    @Override
    public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler)
    {
        return new OracleStatementResultIteratorFactory(resultSetHandler);
    }
}
