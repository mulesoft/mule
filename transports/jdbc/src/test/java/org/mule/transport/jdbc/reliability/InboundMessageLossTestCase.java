/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.reliability;

import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.jdbc.functional.AbstractJdbcFunctionalTestCase;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayHandler;

/**
 * Verify that no inbound messages are lost when exceptions occur.  
 * The message must either make it all the way to the SEDA queue (in the case of 
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of JDBC, this will cause the ACK query to not be executed and therefore 
 * the source data will still be present the next time the database is polled.
 */
public class InboundMessageLossTestCase extends AbstractJdbcFunctionalTestCase
{
    /** Delay (in ms) to wait for row to be processed */
    public static final int DELAY = 1000;
    
    protected QueryRunner qr;
    
    public InboundMessageLossTestCase()
    {
        setPopulateTestData(false);
    }
    
    @Override
    protected String getConfigResources()
    {
        return "reliability/jdbc-connector.xml, reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        // Set SystemExceptionStrategy to redeliver messages (this can only be configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter("*"));

        qr = jdbcConnector.getQueryRunner();
    }

    public void testNoException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);

        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 1 AND ACK IS NULL", new ArrayHandler());
        // Delivery was successful so row should be acknowledged (marked read).
        assertNull(queryResult);
    }
    
    public void testTransformerException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (2, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);
        
        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 2 AND ACK IS NULL", new ArrayHandler());
        // Delivery failed so row should not be acknowledged (marked read).
        assertNotNull(queryResult);
        assertEquals(1, queryResult.length);
    }
    
    public void testRouterException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (3, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);

        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 3 AND ACK IS NULL", new ArrayHandler());
        // Delivery failed so row should not be acknowledged (marked read).
        assertNotNull(queryResult);
        assertEquals(1, queryResult.length);
    }
    
    public void testComponentException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (4, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);

        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 4 AND ACK IS NULL", new ArrayHandler());
        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        assertNull(queryResult);
    }    
}
