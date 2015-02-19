/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.jms.transformers.AbstractJmsTransformer;

import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/** <code>JmsTransformersTestCase</code> Tests the JMS transformer implementations. */
public class JmsMessageAwareTransformersMule2685TestCase extends AbstractJmsFunctionalTestCase
{
    protected final Log logger = LogFactory.getLog(getClass());

    private Session session = null;

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-transformers.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        session = getConnection(false, false).createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
        if (session != null)
        {
            session.close();
            session = null;
        }
    }

    @Test
    public void testMessageAwareTransformerChainedWithObjectToJMSMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        MuleMessage message = getTestMuleMessage("This is a test TextMessage");

        SetTestRecipientsTransformer trans = new SetTestRecipientsTransformer();
        MuleMessage result1 = (MuleMessage) trans.transform(message);

        // Check that transformer 1 set message property ok.
        assertEquals("vm://recipient1, vm://recipient1, vm://recipient3",
                     result1.getOutboundProperty("recipients"));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        Message result2 = (Message) trans2.transform(result1);

        // Test to see that ObjectToJMSMessage transformer transformed to JMS message
        // correctly
        assertTrue("Transformed object should be a TextMessage", result2 instanceof TextMessage);
        assertEquals("This is a test TextMessage", ((TextMessage) result2).getText());

        // Check to see if after the ObjectToJMSMessage transformer these properties
        // are on JMS message
        assertEquals("vm://recipient1, vm://recipient1, vm://recipient3",
            result2.getStringProperty("recipients"));

    }

    /** Test <i>AbstractMessageTransformer</i> which sets Message properties */
    private class SetTestRecipientsTransformer extends AbstractMessageTransformer
    {

        public SetTestRecipientsTransformer()
        {
            registerSourceType(DataTypeFactory.MULE_MESSAGE);
        }

        @Override
        public Object transformMessage(MuleMessage message, String outputEncoding)
        {

            String recipients = "vm://recipient1, vm://recipient1, vm://recipient3";
            logger.debug("Setting recipients to '" + recipients + "'");
            message.setOutboundProperty(StaticRecipientList.RECIPIENTS_PROPERTY, recipients);
            return message;
        }

    }

}
