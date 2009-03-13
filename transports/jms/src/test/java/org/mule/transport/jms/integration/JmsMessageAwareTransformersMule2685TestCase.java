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

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.transformers.AbstractJmsTransformer;
import org.mule.transport.jms.transformers.ObjectToJMSMessage;

import javax.jms.ConnectionFactory;
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

    protected String getConfigResources()
    {
        return "integration/jms-transformers.xml";
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector1");
        ConnectionFactory cf = connector.getConnectionFactory();

        session = cf.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    // @Override
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

        MuleMessage message = new DefaultMuleMessage("This is a test TextMessage");

        SetTestRecipientsTransformer trans = new SetTestRecipientsTransformer();
        MuleMessage result1 = (MuleMessage) trans.transform(message);

        // Check that transformer 1 set message property ok.
        assertEquals("vm://recipient1, vm://recipient1, vm://recipient3", result1.getProperty("recipients"));

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

    /*
     * This class overrides getSession() to return the specified test MuleSession;
     * otherwise we would need a full-fledged JMS connector with dispatchers etc.
     * TODO check if we really need this stateful transformer now
     */
    public static class SessionEnabledObjectToJMSMessage extends ObjectToJMSMessage
    {
        private final Session transformerSession;

        public SessionEnabledObjectToJMSMessage(Session session)
        {
            super();
            transformerSession = session;
        }

        // @Override
        protected Session getSession()
        {
            return transformerSession;
        }
    }

    /** Test <i>AbstractMessageAwareTransformer</i> which sets Message properties */
    private class SetTestRecipientsTransformer extends AbstractMessageAwareTransformer
    {

        public SetTestRecipientsTransformer()
        {
            registerSourceType(MuleMessage.class);
        }

        public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
        {

            String recipients = "vm://recipient1, vm://recipient1, vm://recipient3";
            logger.debug("Setting recipients to '" + recipients + "'");
            message.setProperty(StaticRecipientList.RECIPIENTS_PROPERTY, recipients);
            return message;
        }

    }

}
