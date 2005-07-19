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
package org.mule.jbi;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.ObjectName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class TestComponent implements Component, ComponentLifeCycle {

	private ComponentContext context;
	private DeliveryChannel channel;
	
	public ComponentLifeCycle getLifeCycle() {
		return this;
	}

	public ServiceUnitManager getServiceUnitManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public Document getServiceDescription(ServiceEndpoint endpoint) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isExchangeWithConsumerOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
		return true;
	}

	public boolean isExchangeWithProviderOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
		return true;
	}

	public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectName getExtensionMBeanName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void init(ComponentContext context) throws JBIException {
		this.context = context;
		this.channel = context.getDeliveryChannel();
	}

	public void shutDown() throws JBIException {
		// TODO Auto-generated method stub

	}

	public void start() throws JBIException {
		// TODO Auto-generated method stub

	}

	public void stop() throws JBIException {
		// TODO Auto-generated method stub

	}

	public DeliveryChannel getChannel() {
		return channel;
	}

	public ComponentContext getContext() {
		return context;
	}

}
