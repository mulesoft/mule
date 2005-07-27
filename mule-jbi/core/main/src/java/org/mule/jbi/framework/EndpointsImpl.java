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
package org.mule.jbi.framework;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.mule.jbi.Endpoints;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.registry.Component;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;
import org.mule.jbi.servicedesc.ExternalEndpointImpl;
import org.mule.jbi.servicedesc.InternalEndpointImpl;
import org.w3c.dom.Document;

import javax.jbi.JBIException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class EndpointsImpl implements Endpoints {

	private List internalEndpoints;
	private List externalEndpoints;
	
	public EndpointsImpl() {
		this.internalEndpoints = new CopyOnWriteArrayList();
		this.externalEndpoints = new CopyOnWriteArrayList();
	}
	
	
	public void registerInternalEndpoint(ServiceEndpoint endpoint) throws JBIException {
		if (getEndpoint(this.internalEndpoints, endpoint.getServiceName(), endpoint.getEndpointName()) != null) {
			throw new JBIException("Endpoint is already registered");
		}
		if (!(endpoint instanceof InternalEndpointImpl)) {
			throw new IllegalArgumentException("endpoint should be a " + InternalEndpointImpl.class.getName());
		}
		AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoint;
		se.setActive(true);
		this.internalEndpoints.add(se);
	}

	public void unregisterInternalEndpoint(ServiceEndpoint endpoint) throws JBIException {
		if (getEndpoint(this.internalEndpoints, endpoint.getServiceName(), endpoint.getEndpointName()) == null) {
			throw new JBIException("Endpoint is not registered");
		}
		if (!(endpoint instanceof InternalEndpointImpl)) {
			throw new IllegalArgumentException("endpoint should be a " + InternalEndpointImpl.class.getName());
		}
		AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoint;
		se.setActive(false);
		this.internalEndpoints.remove(se);
	}

	public void registerExternalEndpoint(ServiceEndpoint endpoint) throws JBIException {
		if (getEndpoint(this.externalEndpoints, endpoint.getServiceName(), endpoint.getEndpointName()) == null) {
			throw new JBIException("Endpoint is already registered");
		}
		if (!(endpoint instanceof ExternalEndpointImpl)) {
			throw new IllegalArgumentException("endpoint should be a " + ExternalEndpointImpl.class.getName());
		}
		AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoint;
		se.setActive(true);
		this.externalEndpoints.add(se);
	}

	public void unregisterExternalEndpoint(ServiceEndpoint endpoint) throws JBIException {
		if (getEndpoint(this.externalEndpoints, endpoint.getServiceName(), endpoint.getEndpointName()) == null) {
			throw new JBIException("Endpoint is not registered");
		}
		if (!(endpoint instanceof ExternalEndpointImpl)) {
			throw new IllegalArgumentException("endpoint should be a " + ExternalEndpointImpl.class.getName());
		}
		AbstractServiceEndpoint se = (AbstractServiceEndpoint) endpoint;
		se.setActive(false);
		this.externalEndpoints.remove(se);
	}
	
	public ServiceEndpoint getEndpoint(QName service, String name) {
		for (Iterator it = this.internalEndpoints.iterator(); it.hasNext();) {
			ServiceEndpoint se = (ServiceEndpoint) it.next();
			if (se.getServiceName().equals(service) &&
				se.getEndpointName().equals(name)) {
				return se;
			}
		}
		return null;
	}
	
	protected ServiceEndpoint getEndpoint(List endpoints, QName service, String name) {
		for (Iterator it = endpoints.iterator(); it.hasNext();) {
			ServiceEndpoint se = (ServiceEndpoint) it.next();
			if (se.getServiceName().equals(service) &&
				se.getEndpointName().equals(name)) {
				return se;
			}
		}
		return null;
	}

	public ServiceEndpoint[] getInternalEndpoints(QName interfaceName) {
		return getEndpoints(this.internalEndpoints, interfaceName);
	}
	
	public ServiceEndpoint[] getInternalEndpointsForService(QName serviceName) {
		return getEndpointsForService(this.internalEndpoints, serviceName);
	}
	
	public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
		return getEndpoints(this.externalEndpoints, interfaceName);
	}
	
	public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
		return getEndpointsForService(this.externalEndpoints, serviceName);
	}
	
	protected ServiceEndpoint[] getEndpoints(List endpoints, QName interfaceName) {
		List ses = new ArrayList();
		for (Iterator it = endpoints.iterator(); it.hasNext();) {
			ServiceEndpoint se = (ServiceEndpoint) it.next();
			QName[] itfs = se.getInterfaces();
			if (itfs == null && se instanceof InternalEndpointImpl) {
				InternalEndpointImpl ie = (InternalEndpointImpl) se;
				Component component = JbiContainer.Factory.getInstance().getRegistry().getComponent(ie.getComponent());
				Document doc = component.getComponent().getServiceDescription(ie);
				ie.parseWsdl(doc);
				itfs = se.getInterfaces();
			}
			if (itfs != null) {
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

	protected ServiceEndpoint[] getEndpointsForService(List endpoints, QName serviceName) {
		List ses = new ArrayList();
		for (Iterator it = endpoints.iterator(); it.hasNext();) {
			ServiceEndpoint se = (ServiceEndpoint) it.next();
			if (se.getServiceName().equals(serviceName)) {
				ses.add(se);
			}
		}
		return (ServiceEndpoint[]) ses.toArray(new ServiceEndpoint[ses.size()]);
	}

	public void unregisterEndpoints(String component) {
		unregisterEndpoints(this.internalEndpoints, component);
		unregisterEndpoints(this.externalEndpoints, component);
	}

	protected void unregisterEndpoints(List endpoints, String component) {
		// Caution: this code relies on the fact that
		// the iterator for a CopyOnWriteArrayList support
		// concurrent modifications.
		// We can not use the Iterator.remove method as it is not
		// supported on the CopyOnWrite iterator.
		for (Iterator it = endpoints.iterator(); it.hasNext();) {
			AbstractServiceEndpoint se = (AbstractServiceEndpoint) it.next();
			if (component.equals(se.getComponent())) {
				endpoints.remove(se);
			}
		}
	}

}
