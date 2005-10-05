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
*
*/
package org.mule.jbi.nmr;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;
import org.mule.registry.RegistryComponent;

import javax.jbi.component.Component;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class InternalMessageRouter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private JbiContainer container;
    private InternalRouter router;
    private List endpointSelectors = new CopyOnWriteArrayList();

    public InternalMessageRouter(JbiContainer container ) {
        this(container, new DirectRouter(container.getRegistry()));
    }

    public InternalMessageRouter(JbiContainer container, InternalRouter router) {
        this.container = container;
        this.router = router;
        addEndpointSelector(new FirstEndpointSelector());
    }

    public JbiContainer getContainer() {
        return container;
    }

    public InternalRouter getRouter() {
        return router;
    }

    public void send(MessageExchange me) throws MessagingException {
        router.route(me);
    }

	public ServiceEndpoint getTargetEndpoint(MessageExchange me) throws MessagingException {
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
			ServiceEndpoint endpoint;
            for (Iterator iterator = endpointSelectors.iterator(); iterator.hasNext();) {
                EndpointSelector endpointSelector = (EndpointSelector) iterator.next();
                endpoint = endpointSelector.select(endpoints, me);
                if(endpoint!=null) {
                    logger.debug("Selected endpoint: " + endpoint + " using selector: " + endpointSelector);
                    return endpoint;
                } else {
                    logger.debug("No endpoint selected using selector: " + endpointSelector);
                }
            }
		}
		return null;
	}

	protected ServiceEndpoint[] getPossibleEndpoints(MessageExchange me, ServiceEndpoint[] endpoints) {
		List result = new ArrayList();

		for (int i = 0; i < endpoints.length; i++) {
			AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoints[i];
			String compName = se.getComponent();
			RegistryComponent compInfo = container.getRegistry().getComponent(compName);
			if (me.getRole() == MessageExchange.Role.CONSUMER) {
				if (((Component)compInfo.getComponent()).isExchangeWithConsumerOkay(se, me)) {
					result.add(se);
				}
			} else {
				if (((Component)compInfo.getComponent()).isExchangeWithProviderOkay(se, me)) {
					result.add(se);
				}
			}
		}
		return (ServiceEndpoint[]) result.toArray(new ServiceEndpoint[result.size()]);
	}

    public List getEndpointSelectors() {
        return endpointSelectors;
    }

    public void setEndpointSelectors(List endpointSelectors) {
        this.endpointSelectors = endpointSelectors;
    }

    public void addEndpointSelector(EndpointSelector selector) {
        this.endpointSelectors.add(0, selector);
    }

    public void removeEndpointSelector(EndpointSelector selector) {
        this.endpointSelectors.remove(selector);
    }
}
