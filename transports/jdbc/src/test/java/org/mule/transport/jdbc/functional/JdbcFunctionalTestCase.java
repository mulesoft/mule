/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayHandler;

public class JdbcFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{
    public static final String DEFAULT_MESSAGE = "Test Message";
    
    public JdbcFunctionalTestCase()
    {
        setPopulateTestData(false);
    }
    
    protected String getConfigResources()
    {
        return super.getConfigResources() + ",jdbc-functional-config.xml";
    }

    public void testDirectSql() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.request("jdbc://?sql=SELECT * FROM TEST", 1000);
        assertResultSetEmpty(message);
        
        QueryRunner qr = jdbcConnector.getQueryRunner();
        int updated = qr.update(jdbcConnector.getConnection(), "INSERT INTO TEST(TYPE, DATA) VALUES (1, '" + DEFAULT_MESSAGE + "')");
        assertEquals(1, updated);
        message = client.request("jdbc://?sql=SELECT * FROM TEST", 1000);
        assertResultSetNotEmpty(message);
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("jdbc://writeTest?type=2", new DefaultMuleMessage(DEFAULT_MESSAGE));

        QueryRunner qr = jdbcConnector.getQueryRunner();
        Object[] obj2 = 
            (Object[]) qr.query(jdbcConnector.getConnection(), "SELECT DATA FROM TEST WHERE TYPE = 2", new ArrayHandler());
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(DEFAULT_MESSAGE, obj2[0]);
    }

    public void testSendMap() throws Exception
    {
        MuleClient client = new MuleClient();
        Map map = new HashMap();
        map.put("data", DEFAULT_MESSAGE);
        client.send("jdbc://writeMap?type=2", new DefaultMuleMessage(map));

        QueryRunner qr = jdbcConnector.getQueryRunner();
        Object[] obj2 = 
            (Object[]) qr.query(jdbcConnector.getConnection(), "SELECT DATA FROM TEST WHERE TYPE = 2", new ArrayHandler());
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(DEFAULT_MESSAGE, obj2[0]);
    }

    public void testReceive() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.request("jdbc://getTest?type=1", 1000);
        assertResultSetEmpty(message);

        QueryRunner qr = jdbcConnector.getQueryRunner();
        int updated = qr.update(jdbcConnector.getConnection(), "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + DEFAULT_MESSAGE
            + "', NULL, NULL)");
        assertEquals(1, updated);

        message = client.request("jdbc://getTest?type=1", 1000);
        assertResultSetNotEmpty(message);
    }

    public void testReceiveAndSend() throws Exception
    {
        QueryRunner qr = jdbcConnector.getQueryRunner();
        qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + DEFAULT_MESSAGE + "', NULL, NULL)");

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
        assertEquals(DEFAULT_MESSAGE + " Received", obj2[0]);
    }
}
