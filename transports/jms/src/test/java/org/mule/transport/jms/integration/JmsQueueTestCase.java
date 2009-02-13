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
 * Message is sent to and received from simple queue.
 */
public abstract class JmsQueueTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsQueueTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-queue.xml";
    }

    public void testJmsQueue() throws Exception
    {
        dispatchMessage();
        receiveMessage();
        receive(scenarioNotReceive);
    }

    public void testMultipleSend() throws Exception
    {
        dispatchMessage();
        dispatchMessage();
        dispatchMessage();
        receiveMessage();
        receiveMessage();
        receiveMessage();
        receive(scenarioNotReceive);
    }
}
