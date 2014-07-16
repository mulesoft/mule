/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JdbcFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{
    public JdbcFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setPopulateTestData(false);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-functional-config-service.xml"},
            {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-functional-config-flow.xml"}
        });
    }

    @Test
    public void testDirectSql() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("jdbc://SELECT * FROM TEST", 1000);
        assertResultSetEmpty(message);

        QueryRunner qr = jdbcConnector.getQueryRunner();
        int updated = qr.update(jdbcConnector.getConnection(), "INSERT INTO TEST(TYPE, DATA) VALUES (1, '" + TEST_MESSAGE + "')");
        assertEquals(1, updated);
        message = client.request("jdbc://SELECT * FROM TEST", 1000);
        assertResultSetNotEmpty(message);
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("jdbc://writeTest?type=2", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        QueryRunner qr = jdbcConnector.getQueryRunner();
        Object[] obj2 =
            (Object[]) qr.query(jdbcConnector.getConnection(), "SELECT DATA FROM TEST WHERE TYPE = 2", new ArrayHandler());
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(TEST_MESSAGE, obj2[0]);
    }

    @Test
    public void testSendMap() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("data", TEST_MESSAGE);
        client.send("jdbc://writeMap?type=2", new DefaultMuleMessage(map, muleContext));

        QueryRunner qr = jdbcConnector.getQueryRunner();
        Object[] obj2 =
            (Object[]) qr.query(jdbcConnector.getConnection(), "SELECT DATA FROM TEST WHERE TYPE = 2", new ArrayHandler());
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(TEST_MESSAGE, obj2[0]);
    }

    @Test
    public void testReceive() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("jdbc://getTest?type=1", 1000);
        assertResultSetEmpty(message);

        QueryRunner qr = jdbcConnector.getQueryRunner();
        int updated = qr.update(jdbcConnector.getConnection(), "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + TEST_MESSAGE
            + "', NULL, NULL)");
        assertEquals(1, updated);

        message = client.request("jdbc://getTest?type=1", 1000);
        assertResultSetNotEmpty(message);
    }

    @Test
    public void testReceiveAndSend() throws Exception
    {
        QueryRunner qr = jdbcConnector.getQueryRunner();
        qr.update(jdbcConnector.getConnection(),
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + TEST_MESSAGE + "', NULL, NULL)");

        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < 20000)
        {
            Object[] rs =
                (Object[]) qr.query(jdbcConnector.getConnection(), "SELECT COUNT(*) FROM TEST WHERE TYPE = 2", new ArrayHandler());
            assertNotNull(rs);
            assertEquals(1, rs.length);
            if (((Number)rs[0]).intValue() > 0)
            {
                break;
            }
            Thread.sleep(100);
        }

        Object[] obj2 =
            (Object[]) qr.query(jdbcConnector.getConnection(), "SELECT DATA FROM TEST WHERE TYPE = 2", new ArrayHandler());
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(TEST_MESSAGE + " Received", obj2[0]);
    }
}
