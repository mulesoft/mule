/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.tools;

import org.mule.util.IOUtils;
import org.mule.util.Utility;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

/**
 * <code>JmsTestUtils</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmsTestUtils
{
    public static final String UBERMQ_JMS_PROPERTIES = "ubermq-jndi-connection.properties";
    public static final String OPEN_JMS_PROPERTIES = "openjms-jndi-connection.properties";
    public static final String JORAM_JMS_PROPERTIES = "joram-jndi-connection.properties";
    public static final String SPIRIT_WAVE_JMS_PROPERTIES = "spiritwave-jndi-connection.properties";
    public static final String ACTIVE_MQ_JMS_PROPERTIES = "activemq-jndi-connection.properties";

    public static final String JMS_PROPERTIES = "mule-jms-provider.properties";

    public static final String JMS_PROPERTIES_PROPERTY = "org.mule.test.jms.properties";
    public static final String DEFAULT_JNDI_CONECTION_NAME_PROPERTY = "connectionFactoryJNDIName";
    public static final String JNDI_QUEUE_CONECTION_NAME_PROPERTY = "QueueConnectionFactoryJNDIName";
    public static final String JNDI_TOPIC_CONECTION_NAME_PROPERTY = "TopicConnectionFactoryJNDIName";
    public static final String JNDI_XAQUEUE_CONECTION_NAME_PROPERTY = "XAQueueConnectionFactoryJNDIName";
    public static final String JNDI_XATOPIC_CONECTION_NAME_PROPERTY = "XATopicConnectionFactoryJNDIName";

    public static Properties getJmsProperties() throws IOException
    {
        InputStream is = IOUtils.getResourceAsStream(JMS_PROPERTIES, JmsTestUtils.class);

        String jmsProps = OPEN_JMS_PROPERTIES;
        if (is != null) {
            Properties p = new Properties();
            p.load(is);
            jmsProps = p.getProperty("jms.provider.properties", OPEN_JMS_PROPERTIES);
            is.close();
        }
        return getJmsProperties(jmsProps);
    }

    public static Properties getJmsProperties(String propertyFile) throws IOException
    {
        InputStream is = IOUtils.getResourceAsStream(propertyFile, JmsTestUtils.class);

        Properties p = new Properties();
        p.load(is);
        is.close();

        fixProviderUrl(p);
        return p;
    }

    public static void fixProviderUrl(Properties props) throws IOException
    {
        String providerUrl = props.getProperty(Context.PROVIDER_URL);
        if (providerUrl != null && !providerUrl.startsWith("file:") && providerUrl.indexOf(':') < 0) {
            String path = Utility.getResourcePath(providerUrl, JmsTestUtils.class);
            if(path==null) throw new FileNotFoundException(providerUrl);
            providerUrl = "file:" + File.separator + path;
            System.out.println("Setting provider url to: " + providerUrl);
            props.setProperty(Context.PROVIDER_URL, providerUrl);
        }
    }

    public static QueueConnection getQueueConnection() throws IOException, NamingException, JMSException
    {
        return getQueueConnection(getJmsProperties());
    }

    public static XAQueueConnection getXAQueueConnection() throws IOException, NamingException, JMSException
    {
        return getXAQueueConnection(getJmsProperties());
    }

    public static XAQueueConnection getXAQueueConnection(Properties props) throws IOException, NamingException,
            JMSException
    {
        String cnnFactoryName = props.getProperty(JNDI_XAQUEUE_CONECTION_NAME_PROPERTY);
        if (cnnFactoryName == null) {
            throw new IOException("You must set the property " + JNDI_XAQUEUE_CONECTION_NAME_PROPERTY
                    + "in the JNDI property file");
        }
        Context ctx = new InitialContext(props);
        XAQueueConnectionFactory qcf = (XAQueueConnectionFactory) lookupObject(ctx, cnnFactoryName);
        XAQueueConnection cnn;
        String username = (String) props.get("username");

        if (username != null) {
            String password = (String) props.get("password");
            cnn = qcf.createXAQueueConnection(username, password);
        } else {
            cnn = qcf.createXAQueueConnection();
        }
        cnn.start();
        return cnn;
    }

    public static QueueConnection getQueueConnection(Properties props) throws IOException, NamingException,
            JMSException
    {
        fixProviderUrl(props);
        String cnnFactoryName = props.getProperty(DEFAULT_JNDI_CONECTION_NAME_PROPERTY);
        if (cnnFactoryName == null) {
            throw new IOException("You must set the property " + DEFAULT_JNDI_CONECTION_NAME_PROPERTY
                    + "in the JNDI property file");
        }
        Context ctx = new InitialContext(props);
        Object obj = lookupObject(ctx, cnnFactoryName);
        QueueConnectionFactory qcf = (QueueConnectionFactory) obj;
        QueueConnection cnn;
        String username = (String) props.get("username");

        if (username != null) {
            String password = (String) props.get("password");
            cnn = qcf.createQueueConnection(username, password);
        } else {
            cnn = qcf.createQueueConnection();
        }
        cnn.start();
        return cnn;
    }

    public static TopicConnection getTopicConnection() throws IOException, NamingException, JMSException
    {
        return getTopicConnection(getJmsProperties());
    }

    public static TopicConnection getTopicConnection(Properties props) throws IOException, NamingException,
            JMSException
    {
        fixProviderUrl(props);
        String cnnFactoryName = props.getProperty(JNDI_TOPIC_CONECTION_NAME_PROPERTY);
        if (cnnFactoryName == null) {
            throw new IOException("You must set the property " + DEFAULT_JNDI_CONECTION_NAME_PROPERTY
                    + "in the JNDI property file");
        }
        Context ctx = new InitialContext(props);

        TopicConnectionFactory tcf = (TopicConnectionFactory) lookupObject(ctx, cnnFactoryName);
        TopicConnection cnn;
        String username = (String) props.get("username");

        if (username != null) {
            String password = (String) props.get("password");
            cnn = tcf.createTopicConnection(username, password);
        } else {
            cnn = tcf.createTopicConnection();
        }
        cnn.start();
        return cnn;
    }

    public static Object lookupObject(Context context, String reference) throws NamingException {
        Object ref = context.lookup(reference);
        if(ref instanceof Reference) {
            String className = ((Reference)ref).getClassName();
            try {

                ref = ClassHelper.loadClass(className, JmsTestUtils.class).newInstance();
            } catch (Exception e) {
                throw new NamingException("Failed to instanciate class: " + className + ". Exception was: " + e.toString());
            }
        }
        return ref;
    }

    public static XATopicConnection getXATopicConnection() throws IOException, NamingException, JMSException
    {
        Properties props = getJmsProperties();
        String cnnFactoryName = props.getProperty(JNDI_XATOPIC_CONECTION_NAME_PROPERTY);
        if (cnnFactoryName == null) {
            throw new IOException("You must set the property " + JNDI_XAQUEUE_CONECTION_NAME_PROPERTY
                    + "in the JNDI property file");
        }
        Context ctx = new InitialContext(props);
        XATopicConnectionFactory tcf = (XATopicConnectionFactory) lookupObject(ctx, cnnFactoryName);
        XATopicConnection cnn;
        String username = (String) props.get("username");

        if (username != null) {
            String password = (String) props.get("password");
            cnn = tcf.createXATopicConnection(username, password);
        } else {
            cnn = tcf.createXATopicConnection();
        }
        cnn.start();
        return cnn;
    }

    public static Session getSession(Connection cnn) throws JMSException
    {
        Session session;
        if (cnn instanceof QueueConnection) {
            session = ((QueueConnection) cnn).createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        } else {
            session = ((TopicConnection) cnn).createTopicSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        }
        return session;
    }

    public static XASession getXASession(XAConnection cnn) throws JMSException
    {
        XASession session;
        if (cnn instanceof XAQueueConnection) {
            session = ((XAQueueConnection) cnn).createXAQueueSession();
        } else {
            session = ((XATopicConnection) cnn).createXATopicSession();
        }
        return session;
    }

    public static void drainQueue(QueueConnection cnn, String queue) throws Exception
    {
        QueueSession session = cnn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue q = session.createQueue(queue);
        QueueReceiver receiver = session.createReceiver(q);
        // Add a delay so that activeMQ can fetch messages from the broker
        // Thread.sleep(5000);
        Message msg = null;
        while ((msg = receiver.receive(1000)) != null) {
            System.out.println("Removing message: " + msg);
            msg.acknowledge();
        }
        receiver.close();
        session.close();
    }

    public static void drainTopic(TopicConnection cnn, String topic) throws Exception
    {
        TopicSession session = cnn.createTopicSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Topic t = session.createTopic(topic);

        TopicSubscriber subscriber = session.createSubscriber(t);
        Message msg = subscriber.receiveNoWait();
        while (msg != null) {
            try {
                msg.acknowledge();
            } catch (JMSException e) {

            }
            msg = subscriber.receiveNoWait();
        }
        subscriber.close();
        session.close();
    }

    public static TextMessage getTextMessage(Connection cnn, String message) throws Exception
    {
        return getSession(cnn).createTextMessage(message);
    }

    public static void queueSend(QueueConnection cnn,
                                 String queueName,
                                 String payload,
                                 boolean transacted,
                                 int ack,
                                 String replyTo) throws JMSException
    {
        QueueSession session = cnn.createQueueSession(transacted, ack);
        Queue queue = session.createQueue(queueName);
        QueueSender sender = session.createSender(queue);
        TextMessage msg = session.createTextMessage();
        msg.setText(payload);
        msg.setJMSDeliveryMode(ack);
        if (replyTo != null) {
            msg.setJMSReplyTo(session.createQueue(replyTo));
        }

        sender.send(msg);
        sender.close();
        session.close();
    }

    public static void topicPublish(TopicConnection cnn, String topicName, String payload, boolean transacted, int ack)
            throws JMSException
    {
        topicPublish(cnn, topicName, payload, transacted, ack, null);
    }

    public static void topicPublish(TopicConnection cnn,
                                    String topicName,
                                    String payload,
                                    boolean transacted,
                                    int ack,
                                    String replyTo) throws JMSException
    {
        TopicSession session = cnn.createTopicSession(transacted, ack);
        Topic topic = session.createTopic(topicName);
        TopicPublisher publisher = session.createPublisher(topic);
        TextMessage msg = session.createTextMessage();
        msg.setText(payload);
        msg.setJMSDeliveryMode(ack);
        if (replyTo != null) {
            msg.setJMSReplyTo(session.createTopic(replyTo));
        }
        publisher.publish(msg);
        publisher.close();
        session.close();
    }

    public static Message queueReceiver(QueueConnection cnn, String queueName, long timeout) throws JMSException
    {
        QueueSession session = cnn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);
        QueueReceiver receiver = session.createReceiver(queue);
        Message msg = receiver.receive(timeout);
        if (msg != null)
            msg.acknowledge();
        receiver.close();
        session.close();
        return msg;
    }

    public static Message topicSubscribe(TopicConnection cnn, String topicName, long timeout) throws JMSException
    {
        TopicSubscriber receiver = getTopicSubscriber(cnn, topicName);
        Message msg = receiver.receive(timeout);
        if (msg != null)
            msg.acknowledge();
        receiver.close();
        return msg;
    }

    public static TopicSubscriber getTopicSubscriber(TopicConnection cnn, String topicName) throws JMSException
    {
        TopicSession session = cnn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);
        TopicSubscriber receiver = session.createSubscriber(topic);
        return receiver;
    }

    public static QueueReceiver getQueueReceiver(QueueConnection cnn, String queueName) throws JMSException
    {
        QueueSession session = cnn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);
        QueueReceiver receiver = session.createReceiver(queue);
        return receiver;
    }
}
