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
package org.mule.jbi.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.mule.jbi.JbiContainer;

public class EndpointRegistry {

	private JbiContainer container;
	private Map internalEndpoints;
	private Map externalEndpoints;
	
	public EndpointRegistry(JbiContainer container) {
		this.container = container;
	}

	public void start() throws JBIException {
		this.internalEndpoints = new HashMap();
		this.externalEndpoints = new HashMap();
	}
	
	public void registerInternalEndpoint(String component, ServiceEndpoint endpoint) throws JBIException {
		List endpoints = (List) this.internalEndpoints.get(component);
		if (endpoints == null) {
			endpoints = new ArrayList();
			this.internalEndpoints.put(component, endpoints);
		}
		endpoints.add(endpoint);
	}

	public void unregisterInternalEndpoint(String component, ServiceEndpoint endpoint) throws JBIException {
		List endpoints = (List) this.internalEndpoints.get(component);
		if (endpoints != null) {
			if (endpoints.remove(endpoint)) {
				return;
			}
		}
		throw new JBIException("Endpoint is not registered");
	}

	public void registerExternalEndpoint(String component, ServiceEndpoint endpoint) throws JBIException {
		List endpoints = (List) this.externalEndpoints.get(component);
		if (endpoints == null) {
			endpoints = new ArrayList();
			this.externalEndpoints.put(component, endpoints);
		}
		endpoints.add(endpoint);
	}

	public void unregisterExternalEndpoint(String component, ServiceEndpoint endpoint) throws JBIException {
		List endpoints = (List) this.externalEndpoints.get(component);
		if (endpoints != null) {
			if (endpoints.remove(endpoint)) {
				return;
			}
		}
		throw new JBIException("Endpoint is not registered");
	}
	
	public String getComponentFor(ServiceEndpoint se) {
		for (Iterator it = this.internalEndpoints.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			List endpoints = (List) entry.getValue();
			for (Iterator it2 = endpoints.iterator(); it2.hasNext();) {
				ServiceEndpoint se2 = (ServiceEndpoint) it2.next();
				if (se.equals(se2)) {
					return (String) entry.getKey();
				}
			}
		}
		return null;
	}

	public ServiceEndpoint getEndpoint(QName service, String name) {
		for (Iterator it = this.internalEndpoints.values().iterator(); it.hasNext();) {
			List endpoints = (List) it.next();
			for (Iterator it2 = endpoints.iterator(); it2.hasNext();) {
				ServiceEndpoint se = (ServiceEndpoint) it2.next();
				if (se.getServiceName().equals(service) &&
					se.getEndpointName().equals(name)) {
					return se;
				}
			}
		}
		return null;
	}

	public ServiceEndpoint[] getEndpoints(QName interfaceName) {
		List ses = new ArrayList();
		for (Iterator it = this.internalEndpoints.values().iterator(); it.hasNext();) {
			List endpoints = (List) it.next();
			for (Iterator it2 = endpoints.iterator(); it2.hasNext();) {
				ServiceEndpoint se = (ServiceEndpoint) it2.next();
				QName[] itfs = se.getInterfaces();
				for (int i = 0; i < itfs.length; i++) {
					if (itfs[i].equals(interfaceName)) {
						ses.add(se);
						break;
					}
				}
			}
		}
		return (ServiceEndpoint[]) ses.toArray(new ServiceEndpoint[ses.size()]);
	}

	public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
		List ses = new ArrayList();
		for (Iterator it = this.internalEndpoints.values().iterator(); it.hasNext();) {
			List endpoints = (List) it.next();
			for (Iterator it2 = endpoints.iterator(); it2.hasNext();) {
				ServiceEndpoint se = (ServiceEndpoint) it2.next();
				if (se.getServiceName().equals(serviceName)) {
					ses.add(se);
				}
			}
		}
		return (ServiceEndpoint[]) ses.toArray(new ServiceEndpoint[ses.size()]);
	}

	public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
		List ses = new ArrayList();
		for (Iterator it = this.externalEndpoints.values().iterator(); it.hasNext();) {
			List endpoints = (List) it.next();
			for (Iterator it2 = endpoints.iterator(); it2.hasNext();) {
				ServiceEndpoint se = (ServiceEndpoint) it2.next();
				QName[] itfs = se.getInterfaces();
				for (int i = 0; i < itfs.length; i++) {
					if (itfs[i].equals(interfaceName)) {
						ses.add(se);
						break;
					}
				}
			}
		}
		return (ServiceEndpoint[]) ses.toArray(new ServiceEndpoint[ses.size()]);
	}

	public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
		List ses = new ArrayList();
		for (Iterator it = this.externalEndpoints.values().iterator(); it.hasNext();) {
			List endpoints = (List) it.next();
			for (Iterator it2 = endpoints.iterator(); it2.hasNext();) {
				ServiceEndpoint se = (ServiceEndpoint) it2.next();
				if (se.getServiceName().equals(serviceName)) {
					ses.add(se);
				}
			}
		}
		return (ServiceEndpoint[]) ses.toArray(new ServiceEndpoint[ses.size()]);
	}

}
