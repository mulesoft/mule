/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.activemq;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.mule.impl.RequestContext;
import org.mule.providers.jms.transformers.AbstractJmsTransformer;
import org.mule.providers.jms.transformers.JMSMessageToObject;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.AbstractMuleTestCase;

/**
 * <code>JmsTransformersTestCase</code> Tests the JMS transformer implementations.
 */

public class JmsTransformersTestCase extends AbstractMuleTestCase
{
    private static Session session = null;

    protected void suitePreSetUp() throws Exception
    {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));
        factory.setUseEmbeddedBroker(true);
        factory.setBrokerURL("vm://localhost");
        factory.start();

        session = factory.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    protected void suitePostTearDown() throws Exception
    {
        session.close();
        session = null;
    }

    public void testTransObjectMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        ObjectMessage oMsg = session.createObjectMessage();
        File f = new File("C:/testdata/tests.txt");
        oMsg.setObject(f);
        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object result = trans.transform(oMsg);
        assertTrue("Transformed object should be a file", result.getClass().equals(File.class));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        trans2.setReturnClass(ObjectMessage.class);
        Object result2 = trans2.transform(f);
        assertTrue("Transformed object should be an object message", result2 instanceof ObjectMessage);
    }

    public void testTransTextMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        String text = "This is a tests Text Message";
        TextMessage tMsg = session.createTextMessage();
        tMsg.setText(text);

        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object result = trans.transform(tMsg);
        assertTrue("Transformed object should be a string", text.equals(result.toString()));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        trans2.setReturnClass(TextMessage.class);
        Object result2 = trans2.transform(text);
        assertTrue("Transformed object should be an Text message", result2 instanceof TextMessage);
    }

    public void testTransMapMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        Properties p = new Properties();
        p.setProperty("Key1", "Value1");
        p.setProperty("Key2", "Value2");
        p.setProperty("Key3", "Value3");

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnClass(MapMessage.class);
        Object result2 = trans.transform(p);
        assertTrue("Transformed object should be a Map message", result2 instanceof MapMessage);

        MapMessage mMsg = (MapMessage)result2;
        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(Map.class);
        Object result = trans2.transform(mMsg);
        assertTrue("Transformed object should be a Map", result instanceof Map);
    }

    public void testTransByteMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnClass(BytesMessage.class);
        String text = "This is a tests Byte Message";
        Object result2 = trans.transform(text.getBytes());
        assertTrue("Transformed object should be a Bytes message", result2 instanceof BytesMessage);

        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(byte[].class);
        BytesMessage bMsg = (BytesMessage)result2;
        Object result = trans2.transform(bMsg);
        assertTrue("Transformed object should be a byte[]", result instanceof byte[]);
        String res = new String((byte[])result);
        assertTrue("source and result messages should be the same", text.equals(res));
    }

    /*
     * This class overrides getSession() to return the specified test Session;
     * otherwise we would need a full-fledged JMS connector with dispatchers etc.
     */
    public static class SessionEnabledObjectToJMSMessage extends ObjectToJMSMessage
    {
        private static final long serialVersionUID = -440672187466417761L;
        private final Session _session;

        public SessionEnabledObjectToJMSMessage(Session session)
        {
            super();
            _session = session;
        }

        // @Override
        protected Session getSession()
        {
            return _session;
        }
    }

}
