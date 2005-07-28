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

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class DeliveryChannelImpl implements DeliveryChannel {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

	private JbiContainer container;
    private String componentName;
	private boolean closed;
	
	public DeliveryChannelImpl(JbiContainer container, String component) {
		this.container = container;
        this.componentName = component;
	}
	
	public void close() throws MessagingException {
		this.container.getEndpoints().unregisterEndpoints(this.componentName);
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
			QueueSession qs = container.getQueueSession();
			Queue queue = qs.getQueue(componentName);
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
			QueueSession qs = container.getQueueSession();
			Queue queue = qs.getQueue(componentName);
			MessageExchange me = (MessageExchange) queue.poll(timeout);
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
				me.setConsumer(this.componentName);
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
        container.getRouter().send(me);

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
				me.setConsumer(this.componentName);
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
		me.handleSend(true);
		DeliveryChannelImpl ch = (DeliveryChannelImpl) this.container.getRegistry().getComponent(target).getChannel();
		ch.enqueue(me.getTwin());
		try {
			synchronized (exchange) {
				exchange.wait(timeout);
				if (me.getSyncState() == MessageExchangeProxy.SYNC_STATE_SYNC_RECEIVED) {
					me.handleAccept();
					return true;
				} else {
					return false;
				}
			}
		} catch (InterruptedException e) {
			throw new MessagingException(e);
		}
	}
	
	private void handleReceive(MessageExchange exchange) throws MessagingException {
		if (!(exchange instanceof MessageExchangeProxy)) {
			throw new MessagingException("exchange should be created with MessageExchangeFactory");
		}
		MessageExchangeProxy me = (MessageExchangeProxy) exchange;
	    me.handleAccept();
	}
	
	public void enqueue(MessageExchange exchange) throws MessagingException {
		if (!(exchange instanceof MessageExchangeProxy)) {
			throw new MessagingException("exchange should be created with MessageExchangeFactory");
		}
		MessageExchangeProxy me = (MessageExchangeProxy) exchange;
		if (me.getSyncState() == MessageExchangeProxy.SYNC_STATE_SYNC_SENT) {
			synchronized (me) {
				me.setSyncState(MessageExchangeProxy.SYNC_STATE_SYNC_RECEIVED);
				me.notify();
			}
		} else {
            try {
    			QueueSession qs = container.getQueueSession();
    			Queue queue = qs.getQueue(componentName);
                queue.put(me);
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }
	}

	public boolean isClosed() {
		return closed;
	}

	public JbiContainer getContainer() {
		return container;
	}

}
