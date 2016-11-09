/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static java.sql.DriverManager.getConnection;
import static java.sql.DriverManager.registerDriver;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mule.module.db.integration.TestDbConfig.getDerbyResource;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class StalePooledConnectionResetTestCase extends AbstractDbIntegrationTestCase
{

    public StalePooledConnectionResetTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/derby-db-pool-reset-config.xml"};
    }

    @Test
    public void resetsStalePooledConnection() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        // Request to open a DB connection
        MuleMessage response = client.send("vm://testIn", TEST_MESSAGE, null);
        assertThat(response.getExceptionPayload(), is(nullValue()));

        stopDatabase();
        startDatabase();

        // Must process the second request without errors
        response = client.send("vm://testIn", TEST_MESSAGE, null);
        assertThat(response.getExceptionPayload(), is(nullValue()));
    }

    private void startDatabase() throws SQLException
    {
        registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
    }

    private void stopDatabase()
    {
        try
        {
            getConnection("jdbc:derby:;shutdown=true");
            fail("Expected to throw an exception while shutting down the database");
        }
        catch (Exception e)
        {
            // Expected
        }
    }

}
