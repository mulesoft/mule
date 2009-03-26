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

import org.mule.transport.jms.JmsConstants;

import java.util.HashMap;
import java.util.Map;

public class JmsHeaderTypeTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "integration/jms-header-type.xml";
    }

    /**
     * @name TC-TRANSP-JMS-H-2
     * @description The JMSType header field contains a message type identifier supplied by a client when a message is sent.
     * @configuration 2 Components contain inbound endpoint with filter by JMSType
     *  The first - OLGA
     * The second - NATALI
     * @test-procedure
     *  - Create messages and set JMSType
     * - Send messages to Queue
     * - get messages, Every message must be processed the specified component
     * @expected 2 messages are successful received, content is unique
     */
    public void testTranspJmsH2() throws Exception
    {
        Map props = new HashMap();
        props.put(JmsConstants.JMS_TYPE, "OLGA");        
        dispatchMessage(DEFAULT_INPUT_MESSAGE, props);
        receiveMessage("OLGA !!!");        
        receive(scenarioNotReceive);

        props = new HashMap();
        props.put(JmsConstants.JMS_TYPE, "NATALI");        
        dispatchMessage(DEFAULT_INPUT_MESSAGE, props);
        receiveMessage("NATALI !!!");        
        receive(scenarioNotReceive);
    }
}
