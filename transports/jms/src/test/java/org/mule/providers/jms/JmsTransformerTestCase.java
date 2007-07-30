/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.constraint.IsEqual;
import com.mockobjects.dynamic.ConstraintMatcher;
import com.mockobjects.dynamic.FullConstraintMatcher;
import com.mockobjects.dynamic.Mock;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.commons.collections.IteratorUtils;

public class JmsTransformerTestCase extends AbstractMuleTestCase
{

    public void testCustomJMSProperty() throws Exception
    {
        // Warning: this test is REALLY complicated :)
        // The purpose is to test whether custom JMS message properties survive
        // transformations when their name begins with "JMS" (MULE-1120).

        // First we need a JMS message wrapped into a UMOMessage. This turned out to
        // be trickier than expected (ha ha) since mocking a Message depends on the
        // specific calls made to the mocked class.
        Mock mockMessage = new Mock(TextMessage.class);
        mockMessage.expectAndReturn("getJMSCorrelationID", null);
        mockMessage.expectAndReturn("getJMSMessageID", "1234567890");
        mockMessage.expectAndReturn("getJMSDeliveryMode", new Integer(1));
        mockMessage.expectAndReturn("getJMSDestination", null);
        mockMessage.expectAndReturn("getJMSPriority", new Integer(4));
        mockMessage.expectAndReturn("getJMSRedelivered", Boolean.FALSE);
        mockMessage.expectAndReturn("getJMSReplyTo", null);
        mockMessage.expectAndReturn("getJMSExpiration", new Long(0));
        mockMessage.expectAndReturn("getJMSTimestamp", new Long(0));
        mockMessage.expectAndReturn("getJMSType", null);

        mockMessage.expect("toString");
        mockMessage.expect("toString");

        mockMessage.expect("clearProperties");

        mockMessage.expectAndReturn("getPropertyNames",
            IteratorUtils.asEnumeration(IteratorUtils.emptyIterator()));

        mockMessage.expectAndReturn("getObjectProperty", "JMS_CUSTOM_PROPERTY", "customValue");

        ConstraintMatcher setPropertyMatcher = new FullConstraintMatcher(new Constraint[]{
            new IsEqual("JMS_CUSTOM_PROPERTY"), new IsEqual("customValue")});
        mockMessage.expect("setObjectProperty", setPropertyMatcher);

        Message mockTextMessage = (Message)mockMessage.proxy();
        UMOMessage msg = new MuleMessage(new JmsMessageAdapter(mockTextMessage));

        // Now we set a custom "JMS-like" property on the UMOMessage
        msg.setProperty("JMS_CUSTOM_PROPERTY", "customValue");

        // The AbstractJMSTransformer will only apply JMS properties to the
        // underlying message when a "current event" is available, so we need to set
        // one.
        assertNotNull("The test hasn't been configured properly, no managementContext available", managementContext);
        RequestContext.safeSetEvent(new MuleEvent(msg, MuleTestUtils.getTestEvent("previous", managementContext)));

        // The transformer we are going to use is ObjectToJMSMessage, which will
        // return the same (but mockingly modified!) JMS message that is used as
        // input.
        ObjectToJMSMessage transformer = new ObjectToJMSMessage();
        Message transformed = (Message)transformer.transform(msg.getPayload());

        // Finally we can assert that the setProperty done to the UMOMessage actually
        // made it through to the wrapped JMS Message. Yay!
        assertEquals("customValue", transformed.getObjectProperty("JMS_CUSTOM_PROPERTY"));

        // note that we don't verify() the mock since we have no way of knowing
        // whether toString was actually called (environment dependency)
    }

}
