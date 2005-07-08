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
package org.mule.jbi.routing;

import java.util.ArrayList;
import java.util.List;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.framework.ComponentInfo;

public class Router {

	private JbiContainer container;
	
	public Router(JbiContainer container) {
		this.container = container;
	}
	
	public String getTargetComponent(MessageExchange me) throws MessagingException {
		ServiceEndpoint se = me.getEndpoint();
		QName service = me.getService();
		QName interf = me.getInterfaceName();
		if (se != null) {
			se = this.container.getEndpointRegistry().getEndpoint(se.getServiceName(), se.getEndpointName());
			if (se == null) {
				throw new MessagingException("Could not find activated endpoint");
			}
			List components = getPossibleComponents(me, new ServiceEndpoint[] { se });
			if (components.size() == 0) {
				throw new MessagingException("Component refused exchange");
			}
			return chooseComponent(me, components);
		}
		if (service != null) {
			ServiceEndpoint[] endpoints;
			List components;
			// Test internal services
			endpoints = this.container.getEndpointRegistry().getEndpointsForService(service);
			components = getPossibleComponents(me, endpoints);
			if (components.size() > 0) {
				return chooseComponent(me, components);
			}
			// Test external services
			endpoints = this.container.getEndpointRegistry().getExternalEndpointsForService(service);
			components = getPossibleComponents(me, endpoints);
			if (components.size() > 0) {
				return chooseComponent(me, components);
			}
		}
		if (interf != null) {
			ServiceEndpoint[] endpoints;
			List components;
			// Test internal interfaces
			endpoints = this.container.getEndpointRegistry().getEndpoints(interf);
			components = getPossibleComponents(me, endpoints);
			if (components.size() > 0) {
				return chooseComponent(me, components);
			}
			// Test external interfaces
			endpoints = this.container.getEndpointRegistry().getExternalEndpoints(interf);
			components = getPossibleComponents(me, endpoints);
			if (components.size() > 0) {
				return chooseComponent(me, components);
			}
		}
		throw new MessagingException("No target specified");
	}
	
	private String chooseComponent(MessageExchange me, List components) {
		return (String) components.get(0);
	}

	protected List getPossibleComponents(MessageExchange me, ServiceEndpoint[] endpoints) {
		List components = new ArrayList();
		for (int i = 0; i < endpoints.length; i++) {
			ServiceEndpoint se = endpoints[i];
			String compName = this.container.getEndpointRegistry().getComponentFor(se);
			ComponentInfo compInfo = this.container.getComponentRegistry().getComponent(compName);
			if (me.getRole() == MessageExchange.Role.CONSUMER) {
				if (compInfo.getComponent().isExchangeWithConsumerOkay(se, me)) {
					components.add(compName);
				}
			} else {
				if (compInfo.getComponent().isExchangeWithProviderOkay(se, me)) {
					components.add(compName);
				}
			}
		}
		return components;
	}
	
}
