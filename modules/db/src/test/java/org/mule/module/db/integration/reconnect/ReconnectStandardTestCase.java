/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.reconnect;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

import java.util.List;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class ReconnectStandardTestCase extends AbstractDbIntegrationTestCase
{

    public static final int EXPECTED_CONNECTION_ERRORS = 2;

    private static int errorCount;

    public ReconnectStandardTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/reconnect/derby-db-reconnect-standard-config.xml"};
    }

    @Test
    public void reconnectsAfterConnectionFailure() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testReconnection", TEST_MESSAGE, null);

        assertMessageContains(response, getAllPlanetRecords());

        assertThat(errorCount, equalTo(EXPECTED_CONNECTION_ERRORS));
    }

    public static class EnableDatabaseConnection implements RetryNotifier
    {

        public synchronized void onFailure(RetryContext context, Throwable e)
        {

            errorCount++;

            if (errorCount == EXPECTED_CONNECTION_ERRORS)
            {
                // Fixes datasource's URL to enable connection
                DbConfigResolver dbConfigResolver = muleContext.getRegistry().get("badDbConfig");
                DbConfig config = dbConfigResolver.resolve(null);
                StandardDataSource dataSource = (StandardDataSource) config.getDataSource();
                dataSource.setUrl("jdbc:derby:target/muleEmbeddedDB;create=true");
            }
        }

        public void onSuccess(RetryContext context)
        {

        }
    }
}