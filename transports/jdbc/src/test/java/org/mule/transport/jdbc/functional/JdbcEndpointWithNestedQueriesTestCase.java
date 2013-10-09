/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.api.client.MuleClient;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Tests Mule disposal in the context of nested queries on JDBC endpoints. This test is related to MULE-6607.
 */
public class JdbcEndpointWithNestedQueriesTestCase extends AbstractJdbcFunctionalTestCase
{
    public JdbcEndpointWithNestedQueriesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setPopulateTestData(false);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
            {ConfigVariant.FLOW, "jdbc-endpoint-with-nested-queries-test.xml"}
        });
    }
    
    @Test
    public void testDisposeAfterQueryExecution() throws Exception
    {
        QueryRunner queryRunner = jdbcConnector.getQueryRunner();
        Connection connection = jdbcConnector.getConnection();

        // Send a message to trigger nested query on JDBC outbound endpoint to execute.
        MuleClient client = muleContext.getClient();
        client.send("vm://in", "some test data", null);

        // Assert that query executed correctly.
        List<?> results = (List<?>) queryRunner.query(connection, "SELECT * FROM TEST", new ArrayListHandler());
        assertEquals(1, results.size());

        // Try to dispose gracefully.
        try
        {
            muleContext.dispose();
        }
        catch (Exception ex)
        {
            fail("Server disposal failed");
        }
    }
}
