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
		// TODO deactivate all endpoints
		this.closed = true;
	}

	public MessageExchangeFactory createExchangeFactory() {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl();
		return mep;
	}

	public MessageExchangeFactory createExchangeFactory(QName interfaceName) {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl();
		mep.setInterfaceName(interfaceName);
		return mep;
	}

	public MessageExchangeFactory createExchangeFactoryForService(QName serviceName) {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl();
		mep.setService(serviceName);
		return mep;
	}

	public MessageExchangeFactory createExchangeFactory(ServiceEndpoint endpoint) {
		MessageExchangeFactoryImpl mep = new MessageExchangeFactoryImpl();
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
				target = this.container.getRouter().getTargetComponent(exchange);
				me.setProvider(target);
			} else {
				target = me.getProvider();
			}
		} else {
			target = me.getConsumer();
		}
		me.handleSend(false);
		DeliveryChannelImpl ch = (DeliveryChannelImpl) this.container.getComponentRegistry().getComponent(target).getContext().getDeliveryChannel();
		ch.enqueue(me.getTwin());
	}

	public boolean sendSync(MessageExchange exchange) throws MessagingException {
		return sendSync(exchange, 0L);
	}

	public boolean sendSync(MessageExchange exchange, long timeout) throws MessagingException {
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
				target = this.container.getRouter().getTargetComponent(exchange);
				me.setProvider(target);
			} else {
				target = me.getProvider();
			}
		} else {
			target = me.getConsumer();
		}
		me.handleSend(false);
		DeliveryChannelImpl ch = (DeliveryChannelImpl) this.container.getComponentRegistry().getComponent(target).getContext().getDeliveryChannel();
		ch.enqueue(me.getTwin());
		return false;
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

}
