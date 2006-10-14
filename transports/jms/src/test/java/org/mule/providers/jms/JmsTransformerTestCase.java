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

import javax.jms.Message;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.providers.jms.transformers.AbstractJmsTransformer;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;

public class JmsTransformerTestCase extends AbstractMuleTestCase
{

    public void testHeaders()
    {
        // already valid headers are returned as-is, so we can assertSame
        assertSame("identifier", AbstractJmsTransformer.encodeHeader("identifier"));
        assertSame("_identifier", AbstractJmsTransformer.encodeHeader("_identifier"));
        assertSame("identifier_", AbstractJmsTransformer.encodeHeader("identifier_"));
        assertSame("ident_ifier", AbstractJmsTransformer.encodeHeader("ident_ifier"));

        assertEquals("_identifier", AbstractJmsTransformer.encodeHeader("-identifier"));
        assertEquals("identifier_", AbstractJmsTransformer.encodeHeader("identifier-"));
        assertEquals("ident_ifier", AbstractJmsTransformer.encodeHeader("ident-ifier"));
        assertEquals("_ident_ifier_", AbstractJmsTransformer.encodeHeader("-ident_ifier-"));
        assertEquals("_ident_ifier_", AbstractJmsTransformer.encodeHeader("-ident-ifier-"));
    }

    public void testCustomJMSProperty() throws Exception
    {
        // Warning: this test is REALLY complicated :)
        // The purpose is to test whether custom JMS message properties survive
        // transformations when their name begins with "JMS" (MULE-1120).

        // First we need a JMS message wrapped into a UMOMessage. This turned out to
        // be trickier than expected (ha ha) since getMessage() returns a Mock object
        // and any methods invoked on this object have to be registered in advance.
        UMOMessage msg = new MuleMessage(new JmsMessageAdapter(JmsConnectorTestCase.getMessage()));

        // Now we set a custom "JMS-like" property on the UMOMessage
        msg.setProperty("JMS_CUSTOM_PROPERTY", "customValue");

        // The AbstractJMSTransformer will only apply JMS properties to the
        // underlying message when a "current event" is available, so we need to set
        // one.
        RequestContext.setEvent(new MuleEvent(msg, MuleTestUtils.getTestEvent("previous")));

        // The transformer we are going to use is ObjectToJMSMessage, which will
        // return the same (but mockingly modified!) JMS message that is used as
        // input.
        ObjectToJMSMessage transformer = new ObjectToJMSMessage();
        Message transformed = (Message)transformer.transform(msg.getPayload());

        // Finally we can assert that the setProperty done to the UMOMessage actually
        // made it through to the wrapped JMS Message. Yay!
        assertEquals("customValue", transformed.getObjectProperty("JMS_CUSTOM_PROPERTY"));
    }

}
