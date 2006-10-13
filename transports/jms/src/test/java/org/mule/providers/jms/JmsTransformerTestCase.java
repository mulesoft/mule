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

import org.mule.providers.jms.transformers.AbstractJmsTransformer;
import org.mule.tck.AbstractMuleTestCase;

public class JmsTransformerTestCase extends AbstractMuleTestCase
{

    public void testHeaders()
    {
        assertEquals("identifier", AbstractJmsTransformer.encodeHeader("identifier"));
        assertEquals("ident_ifier", AbstractJmsTransformer.encodeHeader("ident_ifier"));
        assertEquals("ident_ifier", AbstractJmsTransformer.encodeHeader("ident-ifier"));
    }

// disabled for now
//    public void testCustomJMSProperty() throws Exception
//    {
//        // Warning: this test is REALLY complicated :)
//        // The purpose is to test whether custom JMS message properties survive
//        // transformations when their name begins with "JMS" (MULE-1120).
//
//        // First we need a JMS message wrapped into a UMOMessage. This turned out to
//        // be trickier than expected (ha ha) since getMessage() returns a Mock object
//        // and any methods invoked on this object have to be registered in advance.
//        UMOMessage msg = new MuleMessage(new JmsMessageAdapter(JmsConnectorTestCase.getMessage()));
//
//        // Now we set a custom "JMS-like" property on the UMOMessage
//        msg.setProperty("JMS_CUSTOM_PROPERTY", "customValue");
//
//        // The AbstractJMSTransformer will only apply JMS properties to the
//        // underlying message when a "current event" is available, so we need to set
//        // one.
//        RequestContext.setEvent(new MuleEvent(msg, MuleTestUtils.getTestEvent("previous")));
//
//        // The transformer we are going to use is ObjectToJMSMessage, which will
//        // return the same (but mockingly modified!) JMS message that is used as
//        // input.
//        ObjectToJMSMessage transformer = new ObjectToJMSMessage();
//        Message transformed = (Message)transformer.transform(msg.getPayload());
//
//        // Finally we can assert that the setProperty done to the UMOMessage actually
//        // made it through to the wrapped JMS Message. Yay!
//        assertEquals("customValue", transformed.getObjectProperty("JMS_CUSTOM_PROPERTY"));
//    }

}
