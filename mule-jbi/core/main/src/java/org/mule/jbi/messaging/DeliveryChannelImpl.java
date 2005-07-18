/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.messaging;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class DeliveryChannelImpl implements DeliveryChannel {

	private BlockingQueue queue;
	private JbiContainer container;
	private String component;
	private boolean closed;
	
	public DeliveryChannelImpl(JbiContainer container, String component) {
		this.container = container;
		this.component = component;
		// TODO: queue creation should be customized
		this.queue = new LinkedBlockingQueue();
	}
	
	public void close() throws MessagingException {
		this.container.getEndpoints().unregisterEndpoints(this.component);
		this.closed = true;
	}

	public MessageExchangeFactory createExchangeFactory() {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl(this);
		return mep;
	}

	public MessageExchangeFactory createExchangeFactory(QName interfaceName) {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl(this);
		mep.setInterfaceName(interfaceName);
		return mep;
	}

	public MessageExchangeFactory createExchangeFactoryForService(QName serviceName) {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl(this);
		mep.setService(serviceName);
		return mep;
	}

	public MessageExchangeFactory createExchangeFactory(ServiceEndpoint endpoint) {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl(this);
		mep.setEndpoint(endpoint);
		return mep;
	}

	public MessageExchange accept() throws MessagingException {
		if (this.closed) {
			throw new MessagingException("Channel is closed");
		}
		try {
			MessageExchange me = (MessageExchange) queue.take();
			if (me != null) {
				handleReceive(me);
			}
			return me;
		} catch (InterruptedException e) {
			throw new MessagingException(e);
		}
	}

	public MessageExchange accept(long timeout) throws MessagingException {
		if (this.closed) {
			throw new MessagingException("Channel is closed");
		}
		try {
			MessageExchange me = (MessageExchange) queue.poll(timeout, TimeUnit.MILLISECONDS);
			if (me != null) {
				handleReceive(me);
			}
			return me;
		} catch (InterruptedException e) {
			throw new MessagingException(e);
		}
	}

	public void send(MessageExchange exchange) throws MessagingException {
		if (this.closed) {
			throw new MessagingException("Channel is closed");
		}
		if (!(exchange instanceof MessageExchangeProxy)) {
			throw new MessagingException("exchange should be created with MessageExchangeFactory");
		}
		MessageExchangeProxy me = (MessageExchangeProxy) exchange;
		String target;
		if (me.getRole() == MessageExchange.Role.CONSUMER) {
			if (me.getConsumer() == null) {
				me.setConsumer(this.component);
				ServiceEndpoint se = this.container.getRouter().getTargetEndpoint(exchange);
				me.setEndpoint(se);
				target = ((AbstractServiceEndpoint) se).getComponent();
				me.setProvider(target);
			} else {
				target = me.getProvider();
			}
		} else {
			target = me.getConsumer();
		}
		me.handleSend(false);
		DeliveryChannelImpl ch = (DeliveryChannelImpl) this.container.getRegistry().getComponent(target).getChannel();
		ch.enqueue(me.getTwin());
	}

	public boolean sendSync(MessageExchange exchange) throws MessagingException {
		return sendSync(exchange, 0L);
	}

	public boolean sendSync(MessageExchange exchange, long timeout) throws MessagingException {
		// TODO: implement sync send
		throw new NotImplementedException();
	}
	
	private void handleReceive(MessageExchange exchange) throws MessagingException {
		if (!(exchange instanceof MessageExchangeProxy)) {
			throw new MessagingException("exchange should be created with MessageExchangeFactory");
		}
		MessageExchangeProxy me = (MessageExchangeProxy) exchange;
		me.handleAccept();
	}
	
	public void enqueue(MessageExchange exchange) {
		queue.add(exchange);
	}

	public boolean isClosed() {
		return closed;
	}

	public JbiContainer getContainer() {
		return container;
	}

}
