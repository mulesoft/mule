/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.database.GenericDbConfig;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class CustomDbTypeConfigTestCase extends AbstractDbIntegrationTestCase
{

    public static final String CUSTOM_TYPE_NAME1 = "CUSTOM_TYPE1";
    public static final int CUSTOM_TYPE_ID1 = 3001;
    public static final String CUSTOM_TYPE_NAME2 = "CUSTOM_TYPE2";
    public static final int CUSTOM_TYPE_ID2 = 3002;

    public CustomDbTypeConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Collections.singletonList(new Object[] {"integration/config/custom-type-db-config.xml", new DerbyTestDatabase()});
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[0];
    }

    @Test
    public void resolvesCustomDbTypes() throws Exception
    {
        DbConfigResolver dbConfigResolver  = muleContext.getRegistry().lookupObject("dbConfig");
        GenericDbConfig dbConfig = (GenericDbConfig) dbConfigResolver.resolve(null);
        DbTypeManager dbTypeManager = dbConfig.getDbTypeManager();
        DbConnectionFactory connectionFactory = dbConfig.getConnectionFactory();
        DbConnection connection = connectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED);

        try
        {
            assertResolvesType(connection, dbTypeManager, CUSTOM_TYPE_NAME1, CUSTOM_TYPE_ID1);
            assertResolvesType(connection, dbTypeManager, CUSTOM_TYPE_NAME2, CUSTOM_TYPE_ID2);
        }
        finally
        {
            connectionFactory.releaseConnection(connection);
        }
    }

    protected void assertResolvesType(DbConnection connection, DbTypeManager dbTypeManager, String name, int id)
    {
        DbType cursor = dbTypeManager.lookup(connection, name);

        assertThat(name, equalTo(cursor.getName()));
        assertThat(id, equalTo(cursor.getId()));
    }
}