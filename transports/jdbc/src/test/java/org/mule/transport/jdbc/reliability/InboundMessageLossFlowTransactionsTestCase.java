/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.reliability;

/**
 * Verify that no inbound messages are lost when exceptions occur.  
 * The message must either make it all the way to the SEDA queue (in the case of 
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of JDBC, this will cause the ACK query to not be executed and therefore 
 * the source data will still be present the next time the database is polled.
 */
public class InboundMessageLossFlowTransactionsTestCase extends InboundMessageLossTransactionsTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                "reliability/jdbc-connector.xml",
                "reliability/inbound-message-loss-flow-transactions.xml"
        };
    }
}