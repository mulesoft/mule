/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

/**
 * Message is put to topic with two subscribers
 */
public class JmsTopicTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "providers/activemq/jms-topic.xml";
    }
    
    public void testJmsTopic() throws Exception
    {
        // One message is sent.
        dispatchMessage();
        // The same message is read twice from the same JMS topic.
        receiveMessage();
        receiveMessage();
    }

    public void testMultipleSend() throws Exception
    {
        // One message is sent.
        dispatchMessage();
        dispatchMessage();
        // The same message is read twice from the same JMS topic.
        receiveMessage();
        receiveMessage();
        receiveMessage();
        receiveMessage();
    }
}
