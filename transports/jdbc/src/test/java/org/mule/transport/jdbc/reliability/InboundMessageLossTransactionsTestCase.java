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


public class InboundMessageLossTransactionsTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/jdbc-connector.xml, reliability/inbound-message-loss-transactions.xml";
    }
    
    @Override
    public void testComponentException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (4, '" + TEST_MESSAGE + "', NULL, NULL)"));

        Thread.sleep(DELAY);

        Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
            "SELECT DATA FROM TEST WHERE TYPE = 4 AND ACK IS NULL", new ArrayHandler());
        // Although a component exception occurs after the SEDA queue, the use of transactions 
        // bypasses the SEDA queue, so message should get redelivered.
        assertNotNull(queryResult);
        assertEquals(1, queryResult.length);
    }    
}
