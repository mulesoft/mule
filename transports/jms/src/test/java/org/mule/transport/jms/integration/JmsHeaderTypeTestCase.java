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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

public class JmsHeaderTypeTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsHeaderTypeTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-header-type.xml";
    }

    @Override
    protected Message createJmsMessage(Object payload, Session session) throws JMSException
    {
        Message message = super.createJmsMessage(payload, session);
        message.setJMSType("NATALI");
        return message;
    }
    
    public void testTranspJmsH2() throws Exception
    {
        Message input = send(DEFAULT_INPUT_MESSAGE);
        
        assertPayloadEquals("NATALI !!!", receive());

        // No more messages
        assertNull(receiveNoWait());
    }
}
