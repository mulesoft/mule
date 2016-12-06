/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import org.mule.module.db.internal.domain.connection.ConnectionFactory;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.connection.OracleTransactionalDbConnectionFactory;
import org.mule.module.db.internal.domain.transaction.TransactionCoordinationDbTransactionManager;
import org.mule.module.db.internal.domain.type.DbTypeManager;

import javax.sql.DataSource;

public class OracleDbConfig extends GenericDbConfig
{

    public OracleDbConfig(DataSource dataSource, String name, DbTypeManager dbTypeManager)
    {
        super(dataSource, name, dbTypeManager);
    }

    @Override
    protected DbConnectionFactory createDbConnectionFactory(DataSource dataSource, ConnectionFactory connectionFactory, DbTypeManager dbTypeManager)
    {
        return new OracleTransactionalDbConnectionFactory(new TransactionCoordinationDbTransactionManager(), dbTypeManager, connectionFactory, dataSource);
    }
}
