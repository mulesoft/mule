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

import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

/**
 * Message is sent to and received from simple queue using compression in between
 */
public class JmsQueueWithCompressionTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsQueueWithCompressionTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-queue-with-compression.xml";
    }

    @Test
    public void testJmsQueue() throws Exception
    {
        // Lets test it doesn't blow up with serialized objects
        dispatchMessage(new Apple());
        receiveMessage();
        receive(scenarioNotReceive);
    }

    @Test
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
