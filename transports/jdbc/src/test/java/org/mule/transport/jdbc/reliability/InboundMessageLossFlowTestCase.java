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
public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    public InboundMessageLossFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);     
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{            
            {ConfigVariant.FLOW, "reliability/jdbc-connector.xml, reliability/inbound-message-loss-flow.xml"}
        });
    }          

    @Override
    @Test
    public void testTransformerException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (2, '" + TEST_MESSAGE + "', NULL, NULL)"));

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                try
                {
                    Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
                        "SELECT DATA FROM TEST WHERE TYPE = 2 AND ACK IS NULL", new ArrayHandler());
                    // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
                    // perspective, the message has been delivered successfully.
                    // Note that this behavior is different from services because the exception occurs before
                    // the SEDA queue for services.
                    return (queryResult == null);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String describeFailure()
            {
                return "Row should be acknowledged (marked read)";
            }
        });
    }
    
    @Override
    @Test
    public void testRouterException() throws Exception
    {
        assertEquals(1, qr.update(jdbcConnector.getConnection(), 
            "INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (3, '" + TEST_MESSAGE + "', NULL, NULL)"));

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                try
                {
                    Object[] queryResult = (Object[]) qr.query(jdbcConnector.getConnection(), 
                        "SELECT DATA FROM TEST WHERE TYPE = 3 AND ACK IS NULL", new ArrayHandler());
                    // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
                    // perspective, the message has been delivered successfully.
                    // Note that this behavior is different from services because the exception occurs before
                    // the SEDA queue for services.
                    return (queryResult == null);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String describeFailure()
            {
                return "Row should be acknowledged (marked read)";
            }
        });
    }    
}

