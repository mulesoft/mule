/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.MessagingException;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;

import com.mockobjects.dynamic.Mock;

import javax.jms.TextMessage;

import org.apache.commons.collections.IteratorUtils;

public class JmsMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{  
    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new JmsMessageAdapter(payload);
    }

    public Object getValidMessage() throws Exception
    {
        Mock message = new Mock(TextMessage.class);

        message.expectAndReturn("getText", "Test JMS Message");
        message.expectAndReturn("getText", "Test JMS Message");

        message.expectAndReturn("getJMSCorrelationID", null);
        message.expectAndReturn("getJMSMessageID", "1234567890");
        message.expectAndReturn("getJMSDeliveryMode", new Integer(1));
        message.expectAndReturn("getJMSDestination", null);
        message.expectAndReturn("getJMSPriority", new Integer(4));
        message.expectAndReturn("getJMSRedelivered", Boolean.FALSE);
        message.expectAndReturn("getJMSReplyTo", null);
        message.expectAndReturn("getJMSExpiration", new Long(0));
        message.expectAndReturn("getJMSTimestamp", new Long(0));
        message.expectAndReturn("getJMSType", null);

        message.expect("toString");

        message.expectAndReturn("getPropertyNames",
                                IteratorUtils.asEnumeration(IteratorUtils.emptyIterator()));

        return message.proxy();
    }

    public void testIllegalSpecification() throws Exception
    {
        JmsMessageAdapter messageAdapter = (JmsMessageAdapter)this.createAdapter(this.getValidMessage());

        // these will work
        messageAdapter.setSpecification(JmsConstants.JMS_SPECIFICATION_102B);
        messageAdapter.setSpecification(JmsConstants.JMS_SPECIFICATION_11);

        try
        {
            // this will not :)
            messageAdapter.setSpecification("1.2");
            fail("JmsMessageAdapter should fail on setting an invalid specification");
        }
        catch (IllegalArgumentException iax)
        {
            // OK
        }
    }

}
