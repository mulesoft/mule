/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.vendors;

import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterSerializationTestCase;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.jms.JmsMessageAdapter;

import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQTextMessage;


public class ActiveMQMessageAdapterSerializationTestCase extends AbstractMessageAdapterSerializationTestCase
{
    private static final String KEY = "jms-key";
    private static final String VALUE = "jms-value";

    @Override
    protected MessageAdapter createMessageAdapter() throws Exception
    {
        TextMessage jmsMessage = new ActiveMQTextMessage();
        jmsMessage.setText(PAYLOAD);
        jmsMessage.setStringProperty(KEY, VALUE);
        
        
        MessageAdapter messageAdapter = new JmsMessageAdapter(jmsMessage);
        messageAdapter.setProperty(STRING_PROPERTY_KEY, STRING_PROPERTY_VALUE);
        
        return messageAdapter;
    }

    protected void doAdditionalAssertions(DefaultMessageAdapter messageAdapter)
    {
        assertEquals(VALUE, messageAdapter.getProperty(KEY));
    }
    
}


