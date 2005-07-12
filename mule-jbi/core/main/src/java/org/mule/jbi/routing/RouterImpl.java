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
package org.mule.jbi.routing;

import java.util.ArrayList;
import java.util.List;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.Router;
import org.mule.jbi.framework.AbstractJbiService;
import org.mule.jbi.framework.ComponentInfo;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;

public class RouterImpl extends AbstractJbiService implements Router {

	public RouterImpl(JbiContainer container) {
		super(container);
	}
	
	public ServiceEndpoint getTargetEndpoint(MessageExchange me) throws MessagingException {
		ServiceEndpoint se = me.getEndpoint();
		QName service = me.getService();
		QName interf = me.getInterfaceName();
		if (se != null) {
			se = this.container.getEndpointRegistry().getEndpoint(se.getServiceName(), se.getEndpointName());
			if (se == null) {
				throw new MessagingException("Could not find activated endpoint");
			}
			se = selectEndpoint(me, new ServiceEndpoint[] { se });
			if (se == null) {
				throw new MessagingException("Component refused exchange");
			}
		}
		if (se == null && service != null) {
			ServiceEndpoint[] endpoints;
			// Test internal services
			endpoints = this.container.getEndpointRegistry().getInternalEndpointsForService(service);
			se = selectEndpoint(me, endpoints);
		}
		if (se == null && interf != null) {
			ServiceEndpoint[] endpoints;
			// Test internal interfaces
			endpoints = this.container.getEndpointRegistry().getInternalEndpoints(interf);
			se = selectEndpoint(me, endpoints);
		}
		if (se == null) {
			throw new MessagingException("No target specified");
		}
		return se;
	}
	
	protected ServiceEndpoint selectEndpoint(MessageExchange me, ServiceEndpoint[] endpoints) {
		endpoints = getPossibleEndpoints(me, endpoints);
		if (endpoints.length > 0) {
			return chooseEndpoint(me, endpoints);
		}
		return null;
	}

	protected ServiceEndpoint chooseEndpoint(MessageExchange me, ServiceEndpoint[] endpoints) {
		return endpoints[0];
	}

	protected ServiceEndpoint[] getPossibleEndpoints(MessageExchange me, ServiceEndpoint[] endpoints) {
		List result = new ArrayList();
		for (int i = 0; i < endpoints.length; i++) {
			AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoints[i];
			String compName = se.getComponent();
			ComponentInfo compInfo = this.container.getComponentRegistry().getComponent(compName);
			if (me.getRole() == MessageExchange.Role.CONSUMER) {
				if (compInfo.getComponent().isExchangeWithConsumerOkay(se, me)) {
					result.add(se);
				}
			} else {
				if (compInfo.getComponent().isExchangeWithProviderOkay(se, me)) {
					result.add(se);
				}
			}
		}
		return (ServiceEndpoint[]) result.toArray(new ServiceEndpoint[result.size()]);
	}
	
}
