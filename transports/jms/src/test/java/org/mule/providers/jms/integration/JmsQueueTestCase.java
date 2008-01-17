/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.integration;

/**
 * Message is sent to and received from simple queue.
 */
public class JmsQueueTestCase extends AbstractJmsFunctionalTestCase
{

    public void testJmsQueue() throws Exception
    {
        dispatchMessage();
        receiveMessage();
        receive(scenarioNotReceive);
    }

    protected String getConfigResources()
    {
        return "providers/activemq/jms-queue.xml";
    }
}
