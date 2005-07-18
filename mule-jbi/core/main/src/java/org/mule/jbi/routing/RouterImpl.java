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
import org.mule.jbi.registry.Component;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class RouterImpl  implements Router {

	public RouterImpl() {
	}
	
	public ServiceEndpoint getTargetEndpoint(MessageExchange me) throws MessagingException {
		JbiContainer container = JbiContainer.Factory.getInstance();
		ServiceEndpoint se = me.getEndpoint();
		QName service = me.getService();
		QName interf = me.getInterfaceName();
		if (se != null) {
			se = container.getEndpoints().getEndpoint(se.getServiceName(), se.getEndpointName());
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
			endpoints = container.getEndpoints().getInternalEndpointsForService(service);
			se = selectEndpoint(me, endpoints);
		}
		if (se == null && interf != null) {
			ServiceEndpoint[] endpoints;
			// Test internal interfaces
			endpoints = container.getEndpoints().getInternalEndpoints(interf);
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
		JbiContainer container = JbiContainer.Factory.getInstance();
		List result = new ArrayList();
		for (int i = 0; i < endpoints.length; i++) {
			AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoints[i];
			String compName = se.getComponent();
			Component compInfo = container.getRegistry().getComponent(compName);
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
