/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.reliability;

import static org.junit.Assert.assertEquals;

import org.mule.tck.probe.Probe;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;


/**
 * Verify that no inbound messages are lost when exceptions occur.  
 * The message must either make it all the way to the SEDA queue (in the case of 
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of JDBC, this will cause the ACK query to not be executed and therefore 
 * the source data will still be present the next time the database is polled.
 */
public class InboundMessageLossTransactionsTestCase extends InboundMessageLossTestCase
{
    public InboundMessageLossTransactionsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);     
    }
  
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "reliability/jdbc-connector.xml, reliability/inbound-message-loss-transactions.xml"},            
        });
    }      
    
    @Override
    @Test
    public void testComponentException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (4, '" + TEST_MESSAGE + "', NULL, NULL)"));

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                try
                {
                    Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
                        "SELECT DATA FROM TEST WHERE TYPE = 4 AND ACK IS NULL", new ArrayHandler());
                    // Although a component exception occurs after the SEDA queue, the use of transactions 
                    // bypasses the SEDA queue, so message should get redelivered.
                    return (queryResult != null && queryResult.length == 1);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String describeFailure()
            {
                return "Row should not be acknowledged (marked read)";
            }
        });
    }    
}
