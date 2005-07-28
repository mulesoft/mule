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

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import org.mule.jbi.JbiContainer;
import org.mule.util.UUID;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class MessageExchangeImpl implements MessageExchange, Serializable, MessageExchangeConstants {

	private String exchangeId;
	private ExchangeStatus status;
	private Exception error;
	private Map messages;
	private Map properties;
	private ServiceEndpoint endpoint;
	private QName service;
	private QName interfaceName;
	private QName operation;
	private String consumer;
	private String provider;
	private Transaction transaction;
	
	public MessageExchangeImpl() {
		this.status =  ExchangeStatus.ACTIVE;
		this.messages = new HashMap();
		this.properties = new HashMap();
		this.exchangeId = new UUID().getUUID();
	}
	
	public URI getPattern() {
		throw new IllegalStateException();
	}
	
	public String getExchangeId() {
		return this.exchangeId;
	}

	public ExchangeStatus getStatus() {
		return this.status;
	}

	public void setStatus(ExchangeStatus status) throws MessagingException {
		this.status = status;
	}

	public void setError(Exception error) {
		this.error = error;
	}

	public Exception getError() {
		return this.error;
	}

	public Fault getFault() {
		return (Fault) this.messages.get(FAULT);
	}

	public void setFault(Fault fault) throws MessagingException {
		setMessage(fault, FAULT);
	}
	
	public NormalizedMessage createMessage() throws MessagingException {
		return new NormalizedMessageImpl();
	}

	public Fault createFault() throws MessagingException {
		return new FaultImpl();
	}

	public NormalizedMessage getMessage(String name) {
		return (NormalizedMessage) this.messages.get(name);
	}

	public void setMessage(NormalizedMessage msg, String name) throws MessagingException {
		this.messages.put(name, msg);
	}

	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	public void setProperty(String name, Object obj) {
		if (JTA_TRANSACTION_PROPERTY_NAME.equals(name)) {
			if (!(obj instanceof Transaction)) {
				throw new IllegalArgumentException("should be a javax.transaction.Transaction");
			}
			setTransactionContext((Transaction) obj);
		}
		this.properties.put(name, obj);
	}

	public void setEndpoint(ServiceEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public void setService(QName service) {
		this.service = service;
	}

	public void setInterfaceName(QName interfaceName) {
		this.interfaceName = interfaceName;
	}

	public void setOperation(QName name) {
		this.operation = name;
	}

	public ServiceEndpoint getEndpoint() {
		return this.endpoint;
	}

	public QName getInterfaceName() {
		return this.interfaceName;
	}

	public QName getService() {
		return this.service;
	}

	public QName getOperation() {
		return this.operation;
	}

	public boolean isTransacted() {
		return this.transaction != null;
	}

	public Role getRole() {
		return null;
	}

	public Set getPropertyNames() {
		return this.properties.keySet();
	}

	public String getConsumer() {
		return consumer;
	}

	public void setConsumer(String consumer) {
		this.consumer = consumer;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
	
	void setTransactionContext(Transaction tx) {
		this.transaction = tx;
	}
	
	Transaction getTransactionContext() {
		return this.transaction;
	}
	
	void resumeTx() throws MessagingException {
		if (this.transaction != null) {
			TransactionManager mgr = JbiContainer.Factory.getInstance().getTransactionManager();
			try {
				mgr.resume(this.transaction);
			} catch (Exception e) {
				throw new MessagingException("Can not resume transaction", e);
			}
		}
	}

	void suspendTx() throws MessagingException {
		if (this.transaction != null) {
			TransactionManager mgr = JbiContainer.Factory.getInstance().getTransactionManager();
			try {
				if (mgr.getTransaction() == this.transaction) {
					mgr.suspend();
				}
			} catch (Exception e) {
				throw new MessagingException("Can not suspend transaction", e);
			}
		}
	}

}
