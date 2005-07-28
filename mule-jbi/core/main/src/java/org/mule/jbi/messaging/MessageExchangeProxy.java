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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.util.Set;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public abstract class MessageExchangeProxy implements MessageExchange {

	public static final int SYNC_STATE_ASYNC = 0;
	public static final int SYNC_STATE_SYNC_SENT = 1;
	public static final int SYNC_STATE_SYNC_RECEIVED = 2;
	
	protected static final int CAN_SET_IN_MSG 		= 0x00000001;
	protected static final int CAN_SET_OUT_MSG 		= 0x00000002;
	protected static final int CAN_SET_FAULT_MSG 	= 0x00000004;
	protected static final int CAN_PROVIDER 		= 0x00000008;
	protected static final int CAN_CONSUMER 		= 0x00000000;
	protected static final int CAN_SEND 			= 0x00000010;
	protected static final int CAN_SEND_SYNC		= 0x00000020;
	protected static final int CAN_STATUS_ACTIVE	= 0x00000040;
	protected static final int CAN_STATUS_DONE		= 0x00000080;
	protected static final int CAN_STATUS_ERROR		= 0x00000100;
	protected static final int CAN_OWNER			= 0x00000200;
	
	protected static final int STATES_CANS       = 0;
	protected static final int STATES_NEXT_MSG   = 1;
	protected static final int STATES_NEXT_ERROR = 2;
	protected static final int STATES_NEXT_DONE  = 3;
	
	protected MessageExchangeImpl me;
	protected int state;
	protected MessageExchangeProxy twin;
	protected int[][] states;
	protected int syncState;
	
	public MessageExchangeProxy(int[][] states) {
		this.state = 0;
		this.states = states;
	}
	
	protected boolean can(int c) {
		return (this.states[state][STATES_CANS] & c) == c;
	}
	
	public MessageExchangeProxy getTwin() {
		return this.twin;
	}

	public Fault createFault() throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.createFault();
	}

	public NormalizedMessage createMessage() throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.createMessage();
	}

	public boolean equals(Object obj) {
		if (obj instanceof MessageExchangeProxy) {
			return me.equals(((MessageExchangeProxy) obj).me);
		}
		return false;
	}

	public ServiceEndpoint getEndpoint() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getEndpoint();
	}

	public Exception getError() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getError();
	}

	public String getExchangeId() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getExchangeId();
	}

	public Fault getFault() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getFault();
	}

	public NormalizedMessage getInMessage() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return getMessage(MessageExchangeImpl.IN);
	}

	public QName getInterfaceName() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getInterfaceName();
	}

	public NormalizedMessage getMessage(String name) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		if (name == null) {
			throw new IllegalArgumentException("name should not be null");
		}
		name = name.toLowerCase();
		return me.getMessage(name);
	}

	public QName getOperation() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getOperation();
	}

	public NormalizedMessage getOutMessage() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return getMessage(MessageExchangeImpl.OUT);
	}

	public Object getProperty(String name) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getProperty(name);
	}

	public Set getPropertyNames() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getPropertyNames();
	}

	public Role getRole() {
		if (!can(CAN_OWNER)) {
			//throw new IllegalStateException("component is not owner");
		}
		return can(CAN_PROVIDER) ? Role.PROVIDER : Role.CONSUMER;
	}

	public QName getService() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.getService();
	}

	public ExchangeStatus getStatus() {
		return me.getStatus();
	}

	public boolean isTransacted() {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		return me.isTransacted();
	}

	public int hashCode() {
		return me.hashCode();
	}
	
	public void setEndpoint(ServiceEndpoint endpoint) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setEndpoint(endpoint);
	}

	public void setError(Exception error) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setError(error);
	}

	public void setFault(Fault fault) throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setFault(fault);
	}

	public void setInMessage(NormalizedMessage in) throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		setMessage(in, MessageExchangeImpl.IN);
	}

	public void setInterfaceName(QName interfaceName) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setInterfaceName(interfaceName);
	}

	public void setMessage(NormalizedMessage msg, String name) throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		if (msg == null) {
			throw new IllegalArgumentException("message should not be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("name should not be null");
		}
		name = name.toLowerCase();
		if (MessageExchangeImpl.IN.equals(name) && !can(CAN_SET_IN_MSG)) {
			throw new MessagingException("In not supported");
		}
		if (MessageExchangeImpl.OUT.equals(name) && !can(CAN_SET_OUT_MSG)) {
			throw new MessagingException("Out not supported");
		}
		if (MessageExchangeImpl.FAULT.equals(name) && !can(CAN_SET_FAULT_MSG)) {
			throw new MessagingException("Fault not supported");
		}
		if (MessageExchangeImpl.FAULT.equals(name) && !(msg instanceof Fault)) {
			throw new MessagingException("Setting fault, but message is not a fault");
		}
		if (me.getMessage(name) != null) {
			throw new MessagingException("Can not set the message since it has already been set");
		}
		me.setMessage(msg, name);
	}

	public void setOperation(QName name) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setOperation(name);
	}

	public void setOutMessage(NormalizedMessage out) throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		setMessage(out, MessageExchangeImpl.OUT);
	}

	public void setProperty(String name, Object obj) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setProperty(name, obj);
	}

	public void setService(QName service) {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setService(service);
	}

	public void setStatus(ExchangeStatus status) throws MessagingException {
		if (!can(CAN_OWNER)) {
			throw new IllegalStateException("component is not owner");
		}
		me.setStatus(status);
	}

	public void handleSend(boolean sync) throws MessagingException {
		// Check if send / sendSync is legal
		if (sync) {
			if (!can(CAN_SEND_SYNC)) {
				throw new MessagingException("illegal call to sendSync");
			}
		} else {
			if (!can(CAN_SEND)) {
				throw new MessagingException("illegal call to send");
			}
		}
		this.syncState = sync ? 1 : 0;
		// Check status
		ExchangeStatus status = getStatus();
		if (status == ExchangeStatus.ACTIVE && !can(CAN_STATUS_ACTIVE)) {
			throw new MessagingException("illegal exchange status: active");
		}
		if (status == ExchangeStatus.DONE && !can(CAN_STATUS_DONE)) {
			throw new MessagingException("illegal exchange status: done");
		}
		if (status == ExchangeStatus.ERROR && !can(CAN_STATUS_ERROR)) {
			throw new MessagingException("illegal exchange status: error");
		}
		// Check message
		// Change state
		if (status == ExchangeStatus.ACTIVE) {
			this.state = this.states[this.state][STATES_NEXT_MSG];
		} else if (status == ExchangeStatus.ERROR) {
			this.state = this.states[this.state][STATES_NEXT_ERROR];
		} else if (status == ExchangeStatus.DONE) {
			this.state = this.states[this.state][STATES_NEXT_DONE];
		} else {
			throw new IllegalStateException("unknown status");
		}
		if (this.state < 0 || this.state >= this.states.length) {
			throw new IllegalStateException("next state is illegal");
		}
		// Suspend tx
		this.me.suspendTx();
	}

	public void handleAccept() throws MessagingException {
		// Change state
		ExchangeStatus status = getStatus();
		if (status == ExchangeStatus.ACTIVE) {
			this.state = this.states[this.state][STATES_NEXT_MSG];
		} else if (status == ExchangeStatus.ERROR) {
			this.state = this.states[this.state][STATES_NEXT_ERROR];
		} else if (status == ExchangeStatus.DONE) {
			this.state = this.states[this.state][STATES_NEXT_DONE];
		} else {
			throw new IllegalStateException("unknown status");
		}
		if (this.state < 0 || this.state >= this.states.length) {
			throw new IllegalStateException("next state is illegal");
		}
		// Resume tx
		this.me.resumeTx();
	}

	public void setTwin(MessageExchangeProxy twin) {
		this.twin = twin;
	}

	public String getConsumer() {
		return this.me.getConsumer();
	}

	public void setConsumer(String consumer) {
		this.me.setConsumer(consumer);
	}

	public String getProvider() {
		return this.me.getProvider();
	}

	public void setProvider(String provider) {
		this.me.setProvider(provider);
	}

	public int getSyncState() {
		return this.syncState;
	}

	public void setSyncState(int syncState) {
		this.syncState = syncState;
	}

}
