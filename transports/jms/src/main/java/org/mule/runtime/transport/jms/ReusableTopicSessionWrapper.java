/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReusableTopicSessionWrapper implements TopicSession
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private TopicSession delegateSession;

    ReusableTopicSessionWrapper(TopicSession delegateSession)
    {
        this.delegateSession = delegateSession;
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException
    {
        return delegateSession.createTopic(topicName);
    }

    @Override
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException
    {
        return delegateSession.createSubscriber(topic);
    }

    @Override
    public TopicSubscriber createSubscriber(Topic topic, String messageSelector, boolean noLocal) throws JMSException
    {
        return delegateSession.createSubscriber(topic, messageSelector, noLocal);
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException
    {
        return delegateSession.createDurableSubscriber(topic, name);
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException
    {
        return delegateSession.createDurableSubscriber(topic, name, messageSelector, noLocal);
    }

    @Override
    public TopicPublisher createPublisher(Topic topic) throws JMSException
    {
        return delegateSession.createPublisher(topic);
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException
    {
        return delegateSession.createTemporaryTopic();
    }

    @Override
    public void unsubscribe(String name) throws JMSException
    {
        delegateSession.unsubscribe(name);
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException
    {
        return delegateSession.createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException
    {
        return delegateSession.createMapMessage();
    }

    @Override
    public Message createMessage() throws JMSException
    {
        return delegateSession.createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException
    {
        return delegateSession.createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException
    {
        return delegateSession.createObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException
    {
        return delegateSession.createStreamMessage();
    }

    @Override
    public TextMessage createTextMessage() throws JMSException
    {
        return delegateSession.createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) throws JMSException
    {
        return delegateSession.createTextMessage(text);
    }

    @Override
    public boolean getTransacted() throws JMSException
    {
        return delegateSession.getTransacted();
    }

    @Override
    public int getAcknowledgeMode() throws JMSException
    {
        return delegateSession.getAcknowledgeMode();
    }

    @Override
    public void commit() throws JMSException
    {
        delegateSession.commit();
    }

    @Override
    public void rollback() throws JMSException
    {
        delegateSession.rollback();
    }

    @Override
    public void close() throws JMSException
    {
        //Do nothing, reuse it
    }

    @Override
    public void recover() throws JMSException
    {
        delegateSession.recover();
    }

    @Override
    public MessageListener getMessageListener() throws JMSException
    {
        return delegateSession.getMessageListener();
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException
    {
        delegateSession.setMessageListener(listener);
    }

    @Override
    public void run()
    {
        delegateSession.run();
    }

    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException
    {
        return delegateSession.createProducer(destination);
    }

    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException
    {
        return delegateSession.createConsumer(destination);
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException
    {
        return delegateSession.createConsumer(destination, messageSelector);
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal) throws JMSException
    {
        return delegateSession.createConsumer(destination, messageSelector, NoLocal);
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException
    {
        return delegateSession.createQueue(queueName);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException
    {
        return delegateSession.createBrowser(queue);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException
    {
        return delegateSession.createBrowser(queue, messageSelector);
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException
    {
        return delegateSession.createTemporaryQueue();
    }
}
