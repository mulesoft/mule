/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jdbc;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.providers.jdbc.JdbcConnector;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOConnector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.enhydra.jdbc.standard.StandardDataSource;

public class JdbcNullParamsTestCase extends AbstractJdbcFunctionalTestCase
{
    public static final int idValue = 1;
    public static final String SQL_READ_NULL = "SELECT ID, TYPE, DATA, ACK, RESULT FROM TEST WHERE ID = " + idValue + " AND ACK IS NULL";
    public static final String SQL_ACK_NULL = "UPDATE TEST SET ACK = ${NOW} WHERE ID = ${id}";
    public static final String SQL_WRITE_NULL = "INSERT INTO TEST(ID, TYPE, DATA, ACK, RESULT) VALUES(1, NULL, NULL, NULL, NULL)";

    protected DataSource createDataSource() throws Exception
    {
        StandardDataSource ds = new StandardDataSource();
        ds.setDriverName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:.");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
    
    public UMOConnector createConnector() throws Exception
    {
        JdbcConnector connector = new JdbcConnector();
        connector.setDataSource(getDataSource());
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        connector.setPollingFrequency(5000);

        Map queries = new HashMap();
        queries.put("getTest", SQL_READ_NULL);
        queries.put("getTest.ack", SQL_ACK_NULL);
        queries.put("writeTest", SQL_WRITE_NULL);
        connector.setQueries(queries);

        return connector;
    }
    
    public void testJdbcNullParams() throws Exception
    {
        MuleClient client = new MuleClient();
        
        //check that db is still empty
        UMOMessage reply = client.receive("jdbc://getTest", 1000);
        assertTrue(reply.getPayload() instanceof Collection);
        assertTrue(((Collection)reply.getPayload()).isEmpty());
        
        //execute the write query by sending a message on the jdbc://writeTest
        //the message is a nullpayload since we are not taking any params from any object
        //No other params will be sent to this endpoint
        client.dispatch("jdbc://writeTest", new MuleMessage(NullPayload.getInstance()));
        
        //get the data which was written by the previous statement and test it
        reply = client.receive("jdbc://getTest", 1000);
        
        assertNotNull(reply);
        assertTrue(reply.getPayload() instanceof Collection);
        Collection result = (Collection)reply.getPayload();
        assertEquals(1, result.size());   
        
        Map res = (Map)result.iterator().next();
        
        //check that id is equal to the one set originally and all others are null
        Integer id = (Integer)res.get("ID");
        assertEquals(idValue, id.intValue());
        assertNull(res.get("TYPE"));
        assertNull(res.get("DATA"));
        assertNull(res.get("RESULT"));
    }

}


