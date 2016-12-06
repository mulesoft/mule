/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.module.db.internal.domain.transaction.DbTransactionManager;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.resolver.param.GenericParamTypeResolverFactory;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * Custom {@link TransactionalDbConnectionFactory} for Oracle that creates {@link OracleDbConnection}
 * instances.
 */
public class OracleTransactionalDbConnectionFactory extends TransactionalDbConnectionFactory
{

    /**
     * {@inheritDoc}
     */
    public OracleTransactionalDbConnectionFactory(DbTransactionManager dbTransactionManager, DbTypeManager dbTypeManager, ConnectionFactory connectionFactory, DataSource dataSource)
    {
        super(dbTransactionManager, dbTypeManager, connectionFactory, dataSource);
    }

    @Override
    protected DbConnection doCreateDbConnection(Connection connection, TransactionalAction transactionalAction)
    {
        return new OracleDbConnection(connection, transactionalAction, new DefaultDbConnectionReleaser(this), new GenericParamTypeResolverFactory(dbTypeManager));
    }
}
