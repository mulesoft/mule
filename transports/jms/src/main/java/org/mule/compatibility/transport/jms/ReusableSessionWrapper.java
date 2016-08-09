/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

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
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReusableSessionWrapper implements Session {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private Session delegateSession;

  public ReusableSessionWrapper(Session delegateSession) {
    this.delegateSession = delegateSession;
  }

  public BytesMessage createBytesMessage() throws JMSException {
    return delegateSession.createBytesMessage();
  }

  public MapMessage createMapMessage() throws JMSException {
    return delegateSession.createMapMessage();
  }

  public Message createMessage() throws JMSException {
    return delegateSession.createMessage();
  }

  public ObjectMessage createObjectMessage() throws JMSException {
    return delegateSession.createObjectMessage();
  }

  public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
    return delegateSession.createObjectMessage(object);
  }

  public StreamMessage createStreamMessage() throws JMSException {
    return delegateSession.createStreamMessage();
  }

  public TextMessage createTextMessage() throws JMSException {
    return delegateSession.createTextMessage();
  }

  public TextMessage createTextMessage(String text) throws JMSException {
    return delegateSession.createTextMessage(text);
  }

  public boolean getTransacted() throws JMSException {
    return delegateSession.getTransacted();
  }

  public int getAcknowledgeMode() throws JMSException {
    return delegateSession.getAcknowledgeMode();
  }

  public void commit() throws JMSException {
    delegateSession.commit();
  }

  public void rollback() throws JMSException {
    delegateSession.rollback();
  }

  public void close() throws JMSException {
    // Do nothing, reuse it
  }

  public void recover() throws JMSException {
    delegateSession.recover();
  }

  public MessageListener getMessageListener() throws JMSException {
    return delegateSession.getMessageListener();
  }

  public void setMessageListener(MessageListener listener) throws JMSException {
    delegateSession.setMessageListener(listener);
  }

  public void run() {
    delegateSession.run();
  }

  public MessageProducer createProducer(Destination destination) throws JMSException {
    return delegateSession.createProducer(destination);
  }

  public MessageConsumer createConsumer(Destination destination) throws JMSException {
    return delegateSession.createConsumer(destination);
  }

  public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
    return delegateSession.createConsumer(destination, messageSelector);
  }

  public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal) throws JMSException {
    return delegateSession.createConsumer(destination, messageSelector, NoLocal);
  }

  public Queue createQueue(String queueName) throws JMSException {
    return delegateSession.createQueue(queueName);
  }

  public Topic createTopic(String topicName) throws JMSException {
    return delegateSession.createTopic(topicName);
  }

  public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
    return delegateSession.createDurableSubscriber(topic, name);
  }

  public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal)
      throws JMSException {
    return delegateSession.createDurableSubscriber(topic, name, messageSelector, noLocal);
  }

  public QueueBrowser createBrowser(Queue queue) throws JMSException {
    return delegateSession.createBrowser(queue);
  }

  public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
    return delegateSession.createBrowser(queue, messageSelector);
  }

  public TemporaryQueue createTemporaryQueue() throws JMSException {
    return delegateSession.createTemporaryQueue();
  }

  public TemporaryTopic createTemporaryTopic() throws JMSException {
    return delegateSession.createTemporaryTopic();
  }

  public void unsubscribe(String name) throws JMSException {
    delegateSession.unsubscribe(name);
  }
}
