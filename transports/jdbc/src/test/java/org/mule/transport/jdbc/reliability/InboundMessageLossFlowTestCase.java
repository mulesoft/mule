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

import org.apache.commons.dbutils.handlers.ArrayHandler;


public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/jdbc-connector.xml, reliability/inbound-message-loss-flow.xml";
    }

    @Override
    public void testTransformerException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (2, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);
        
        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 2 AND ACK IS NULL", new ArrayHandler());
        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertNull(queryResult);
    }
    
    @Override
    public void testRouterException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (3, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);

        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 3 AND ACK IS NULL", new ArrayHandler());
        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertNull(queryResult);
    }    
}

