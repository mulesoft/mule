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
 */
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
import javax.jms.XASession;
import javax.transaction.xa.XAResource;


/**
 * @author henks
 */
public class AQSessionDelegate implements Session, XASession {

	private Session session;
	
	private XAResource xaResource;
	
	public AQSessionDelegate(Session session) {
		this.session = session;
	}
	
	public AQSessionDelegate(Session session, XAResource xares) {
		this.session = session;
		this.xaResource = xares;
	}
	
	public Session getSession() {
		return session;
	}
	
	public void setSession(Session session) {
		this.session = session;
	}
	
	public XAResource getXAResource() {
		return xaResource;
	}
	
	public void setXAResource(XAResource xaResource) {
		this.xaResource = xaResource;
	}
	
	public void close() throws JMSException {
		session.close();
	}
	
	public void commit() throws JMSException {
		
		session.commit();
	}
	public QueueBrowser createBrowser(Queue arg0) throws JMSException {
		return session.createBrowser(arg0);
	}
	public QueueBrowser createBrowser(Queue arg0, String arg1)
			throws JMSException {
		return session.createBrowser(arg0, arg1);
	}
	public BytesMessage createBytesMessage() throws JMSException {
		return session.createBytesMessage();
	}
	public MessageConsumer createConsumer(Destination arg0) throws JMSException {
		return session.createConsumer(arg0);
	}
	public MessageConsumer createConsumer(Destination arg0, String arg1)
			throws JMSException {
		return session.createConsumer(arg0, arg1);
	}
	public MessageConsumer createConsumer(Destination arg0, String arg1,
			boolean arg2) throws JMSException {
		return session.createConsumer(arg0, arg1, arg2);
	}
	public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1)
			throws JMSException {
		return session.createDurableSubscriber(arg0, arg1);
	}
	public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1,
			String arg2, boolean arg3) throws JMSException {
		return session.createDurableSubscriber(arg0, arg1, arg2, arg3);
	}
	public MapMessage createMapMessage() throws JMSException {
		return session.createMapMessage();
	}
	public Message createMessage() throws JMSException {
		return session.createMessage();
	}
	public ObjectMessage createObjectMessage() throws JMSException {
		return session.createObjectMessage();
	}
	public ObjectMessage createObjectMessage(Serializable arg0)
			throws JMSException {
		return session.createObjectMessage(arg0);
	}
	public MessageProducer createProducer(Destination arg0) throws JMSException {
		return session.createProducer(arg0);
	}
	public Queue createQueue(String arg0) throws JMSException {
		return session.createQueue(arg0);
	}
	public StreamMessage createStreamMessage() throws JMSException {
		return session.createStreamMessage();
	}
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return session.createTemporaryQueue();
	}
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return session.createTemporaryTopic();
	}
	public TextMessage createTextMessage() throws JMSException {
		return session.createTextMessage();
	}
	public TextMessage createTextMessage(String arg0) throws JMSException {
		return session.createTextMessage(arg0);
	}
	public Topic createTopic(String arg0) throws JMSException {
		return session.createTopic(arg0);
	}
	public boolean equals(Object obj) {
		return session.equals(obj);
	}
	public int getAcknowledgeMode() throws JMSException {
		return session.getAcknowledgeMode();
	}
	public MessageListener getMessageListener() throws JMSException {
		return session.getMessageListener();
	}
	public boolean getTransacted() throws JMSException {
		return session.getTransacted();
	}
	public int hashCode() {
		return session.hashCode();
	}
	public void recover() throws JMSException {
		session.recover();
	}
	public void rollback() throws JMSException {
		session.rollback();
	}
	public void run() {
		session.run();
	}
	public void setMessageListener(MessageListener arg0) throws JMSException {
		session.setMessageListener(arg0);
	}
	public String toString() {
		return session.toString();
	}
	public void unsubscribe(String arg0) throws JMSException {
		session.unsubscribe(arg0);
	}
	
	
}

