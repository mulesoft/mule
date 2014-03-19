/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.vendor.oracle.domain.connection;

import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.domain.connection.DefaultDbConnectionReleaser;
import org.mule.module.db.domain.connection.TransactionalDbConnectionFactory;
import org.mule.module.db.domain.database.DbConfig;
import org.mule.module.db.domain.transaction.DbTransactionManager;
import org.mule.module.db.domain.transaction.TransactionalAction;
import org.mule.module.db.domain.type.DbTypeManager;
import org.mule.module.db.resolver.param.GenericParamTypeResolverFactory;

import java.sql.Connection;

/**
 * Creates {@link OracleDbConnection} instances
 */
public class OracleDbConnectionFactory extends TransactionalDbConnectionFactory
{

    public OracleDbConnectionFactory(DbConfig dbConfig, DbTransactionManager dbTransactionManager, DbTypeManager dbTypeManager)
    {
        super(dbConfig, dbTransactionManager, dbTypeManager);
    }

    @Override
    protected DbConnection doCreateDbConnection(Connection connection, TransactionalAction transactionalAction)
    {
        return new OracleDbConnection(connection, transactionalAction, new DefaultDbConnectionReleaser(this), new GenericParamTypeResolverFactory(dbTypeManager));
    }
}
