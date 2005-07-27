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

import EDU.oswego.cs.dl.util.concurrent.WaitFreeQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.components.AbstractComponent;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class DeliveryChannelImpl implements DeliveryChannel {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

	private WaitFreeQueue queue;
	private JbiContainer container;
	private AbstractComponent component;
    private String componentName;
	private boolean closed;
	
	public DeliveryChannelImpl(JbiContainer container, String component) {
		this.container = container;
		this.component = (AbstractComponent)container.getRegistry().getComponent(component).getComponent();
        this.componentName = component;
		// TODO: queue creation should be customized
		this.queue = new WaitFreeQueue();
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
			MessageExchange me = (MessageExchange) queue.poll(timeout);
			if (me != null) {
				handleReceive(me);
			}
			return me;
		} catch (InterruptedException e) {
			throw new MessagingException(e);
		}
	}
    public void receive(MessageExchange me) throws MessagingException, SystemException, InvalidTransactionException {
        if (me.isTransacted()) {
            TransactionManager mgr = (TransactionManager) component.getContext().getTransactionManager();
            Transaction tx = (Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);
            mgr.resume(tx);
        }

        if(component instanceof MessageListener) {
            ((MessageListener)component).onMessage(me);
        } else {
            enqueue(me);
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
                queue.put(exchange);
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
