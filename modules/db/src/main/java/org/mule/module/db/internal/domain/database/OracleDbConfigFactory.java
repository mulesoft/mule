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
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.ResolvedDbType;
import org.mule.module.db.internal.domain.type.oracle.OracleXmlType;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * Creates {@link DbConfig} instances for Oracle databases
 */
public class OracleDbConfigFactory extends GenericDbConfigFactory
{

    private static final int CURSOR_TYPE_ID = -10;
    private static final String CURSOR_TYPE_NAME = "CURSOR";

    @Override
    protected List<DbType> getVendorDataTypes()
    {
        List<DbType> dbTypes = new ArrayList<DbType>();
        dbTypes.add(new ResolvedDbType(CURSOR_TYPE_ID, CURSOR_TYPE_NAME));
        dbTypes.add(new OracleXmlType());

        return dbTypes;
    }

    @Override
    protected DbConnectionFactory createDbConnectionFactory(DataSource dataSource, ConnectionFactory connectionFactory, DbTypeManager dbTypeManager)
    {
        return new OracleTransactionalDbConnectionFactory(new TransactionCoordinationDbTransactionManager(), dbTypeManager, connectionFactory, dataSource);
    }
}
