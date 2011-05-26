/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.reliability;

import java.util.concurrent.TimeUnit;


public class InboundMessageLossTransactionsTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/activemq-config.xml, reliability/inbound-message-loss-transactions.xml";
    }
    
    @Override
    public void testComponentException() throws Exception
    {
        putMessageOnQueue("componentException");
        
        // Although a component exception occurs after the SEDA queue, the use of transactions 
        // bypasses the SEDA queue, so message should get redelivered.
        assertTrue("Message should have been redelivered", 
            messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS));
    }    
}
