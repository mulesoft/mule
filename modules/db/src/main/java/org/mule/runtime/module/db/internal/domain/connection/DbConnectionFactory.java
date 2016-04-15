/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.module.db.internal.domain.transaction.TransactionalAction;

import java.sql.SQLException;

/**
 * Creates database connections
 */
public interface DbConnectionFactory
{

    /**
     * Creates a connection with a given {@link TransactionalAction}
     *
     * @param transactionalAction indicates whether or not the factory should
     *                            look at active transactions in order to lookup
     *                            for already created connections
     * @return a connection for the given {@link TransactionalAction}
     * @throws SQLException when not able to obtain a new connection
     */
    DbConnection createConnection(TransactionalAction transactionalAction) throws SQLException;

    /**
     * Indicates that the connection  is not used anymore
     *
     * @param connection connection to release
     */
    void releaseConnection(DbConnection connection);
}
