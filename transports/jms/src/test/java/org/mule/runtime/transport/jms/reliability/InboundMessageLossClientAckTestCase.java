/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.reliability;


/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of JMS, this will cause the failed message to be redelivered if
 * JMSRedelivery is configured.
 */
public class InboundMessageLossClientAckTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            "reliability/activemq-clientack-config.xml",
            "reliability/inbound-message-loss-flow.xml"
        };
    }
}


