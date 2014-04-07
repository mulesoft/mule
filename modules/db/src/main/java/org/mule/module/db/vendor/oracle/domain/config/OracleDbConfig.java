/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.vendor.oracle.domain.config;

import org.mule.module.db.domain.connection.TransactionalDbConnectionFactory;
import org.mule.module.db.domain.database.GenericDbConfig;
import org.mule.module.db.domain.transaction.TransactionCoordinationDbTransactionManager;
import org.mule.module.db.domain.type.DbTypeManager;
import org.mule.module.db.vendor.oracle.domain.connection.OracleDbConnectionFactory;

import javax.sql.DataSource;

/**
 * Defines a configuration for Oracle databases
 */
public class OracleDbConfig extends GenericDbConfig
{

    public OracleDbConfig(DataSource dataSource, String name, DbTypeManager dbTypeManager)
    {
        super(dataSource, name, dbTypeManager);
    }

    @Override
    protected TransactionalDbConnectionFactory doCreateConnectionFactory()
    {
        return new OracleDbConnectionFactory(this, new TransactionCoordinationDbTransactionManager(), getDbTypeManager());
    }
}
