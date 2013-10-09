/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.reliability;


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
    protected String getConfigResources()
    {
        return "reliability/activemq-clientack-config.xml, reliability/inbound-message-loss.xml";
    }
}


