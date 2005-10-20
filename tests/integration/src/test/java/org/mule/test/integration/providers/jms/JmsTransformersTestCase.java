/* 

 * $Header$

 * $Revision$

 * $Date$

 * ------------------------------------------------------------------------------------------------------

 * 

 * Copyright (c) SymphonySoft Limited. All rights reserved.

 * http://www.symphonysoft.com

 * 

 * The software in this package is published under the terms of the BSD

 * style license a copy of which has been included with this distribution in

 * the LICENSE.txt file. 

 *

 */

package org.mule.test.integration.providers.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.RequestContext;
import org.mule.providers.jms.transformers.AbstractJmsTransformer;
import org.mule.providers.jms.transformers.JMSMessageToObject;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;

import javax.jms.*;
import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * <code>JmsTransformersTestCase</code> Tests the JMS transformer impls.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JmsTransformersTestCase extends AbstractMuleTestCase
{

    private static Session session = null;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(JmsTransformersTestCase.class);

    protected void doSetUp() throws Exception
    {
        if (session == null) {
            session = JmsTestUtils.getSession(JmsTestUtils.getQueueConnection());
        }
    }

    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
        super.doTearDown();
    }

    public void testTransObjectMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));
        logger.debug("testTransObjectMessage()");
        ObjectMessage oMsg = session.createObjectMessage();
        File f = new File("C:/testdata/tests.txt");
        oMsg.setObject(f);
        logger.debug("created object Message");
        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object result = trans.transform(oMsg, session);
        logger.debug("transformed object is of type " + result.getClass().getName());
        assertTrue("Transformed object should be a file", result.getClass().equals(File.class));
        AbstractJmsTransformer trans2 = new ObjectToJMSMessage();
        trans2.setReturnClass(ObjectMessage.class);
        Object result2 = trans2.transform(f, session);
        logger.debug("transformed object is of type " + result2.getClass().getName());
        assertTrue("Transformed object should be an object message", result2 instanceof ObjectMessage);
    }

    public void testTransTextMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));
        String text = "This is a tests Text Message";
        logger.debug("testTransTextMessage()");
        TextMessage tMsg = session.createTextMessage();
        tMsg.setText(text);
        logger.debug("created Text Message: " + tMsg);
        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object result = trans.transform(tMsg, session);
        logger.debug("transformed object is of type " + result.getClass().getName());
        assertTrue("Transformed object should be a string", text.equals(result.toString()));
        AbstractJmsTransformer trans2 = new ObjectToJMSMessage();
        trans2.setReturnClass(TextMessage.class);
        Object result2 = trans2.transform(text, session);
        logger.debug("transformed object is of type " + result2.getClass().getName());
        assertTrue("Transformed object should be an Text message", result2 instanceof TextMessage);
    }

    public void testTransMapMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));
        logger.debug("testTransMApMessage()");
        Properties p = new Properties();
        p.setProperty("Key1", "Value1");
        p.setProperty("Key2", "Value2");
        p.setProperty("Key3", "Value3");
        AbstractJmsTransformer trans = new ObjectToJMSMessage();
        trans.setReturnClass(MapMessage.class);
        Object result2 = trans.transform(p, session);
        logger.debug("transformed object is of type " + result2.getClass().getName());
        assertTrue("Transformed object should be a Map message", result2 instanceof MapMessage);
        MapMessage mMsg = (MapMessage) result2;
        logger.debug("created Map Message: " + mMsg);
        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(Map.class);
        Object result = trans2.transform(mMsg, session);
        logger.debug("transformed object is of type " + result.getClass().getName());
        assertTrue("Transformed object should be a MAP", result instanceof Map);
    }

    public void testTransByteMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));
        logger.debug("testTransByteMessage()");
        String text = "This is a tests Byte Message";
        AbstractJmsTransformer trans = new ObjectToJMSMessage();
        trans.setReturnClass(BytesMessage.class);
        Object result2 = trans.transform(text.getBytes(), session);
        logger.debug("transformed object is of type " + result2.getClass().getName());
        assertTrue("Transformed object should be a Bytes message", result2 instanceof BytesMessage);
        BytesMessage bMsg = (BytesMessage) result2;
        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(byte[].class);
        Object result = trans2.transform(bMsg, session);
        logger.debug("transformed object is of type " + result.getClass().getName());
        assertTrue("Transformed object should be a byte[]", result instanceof byte[]);
        String res = new String((byte[]) result);
        logger.debug("result message is " + res);
        assertTrue("source and result messages should be the same", text.equals(res));
    }
}
