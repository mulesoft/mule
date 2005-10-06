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

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.mule.jbi.Endpoints;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;
import org.mule.jbi.servicedesc.ExternalEndpointImpl;
import org.mule.jbi.servicedesc.InternalEndpointImpl;
import org.mule.registry.RegistryComponent;
import org.w3c.dom.Document;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * The <code>EndpointsImpl</code> is the default 
 * implementation of the <code>Endpoints</code> interface.
 * 
 * It features endpoints registration / unregistration and queries.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class EndpointsImpl implements Endpoints {

	/**
	 * List of internal endpoints
	 */
	private List internalEndpoints;
	
	/**
	 * List of external endpoints
	 */
	private List externalEndpoints;
	
	/**
	 * Default constructor
	 */
	public EndpointsImpl() {
		this.internalEndpoints = new CopyOnWriteArrayList();
		this.externalEndpoints = new CopyOnWriteArrayList();
	}
	
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#registerInternalEndpoint(javax.jbi.servicedesc.ServiceEndpoint)
	 */
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

	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#unregisterInternalEndpoint(javax.jbi.servicedesc.ServiceEndpoint)
	 */
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

	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#registerExternalEndpoint(javax.jbi.servicedesc.ServiceEndpoint)
	 */
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

	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#unregisterExternalEndpoint(javax.jbi.servicedesc.ServiceEndpoint)
	 */
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
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#getEndpoint(javax.xml.namespace.QName, java.lang.String)
	 */
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
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#getInternalEndpoints(javax.xml.namespace.QName)
	 */
	public ServiceEndpoint[] getInternalEndpoints(QName interfaceName) {
		return getEndpoints(this.internalEndpoints, interfaceName);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#getInternalEndpointsForService(javax.xml.namespace.QName)
	 */
	public ServiceEndpoint[] getInternalEndpointsForService(QName serviceName) {
		return getEndpointsForService(this.internalEndpoints, serviceName);
	}
	
	public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
		return getEndpoints(this.externalEndpoints, interfaceName);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#getExternalEndpointsForService(javax.xml.namespace.QName)
	 */
	public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
		return getEndpointsForService(this.externalEndpoints, serviceName);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.Endpoints#unregisterEndpoints(java.lang.String)
	 */
	public void unregisterEndpoints(String component) {
		unregisterEndpoints(this.internalEndpoints, component);
		unregisterEndpoints(this.externalEndpoints, component);
	}

	/**
	 * Search within the given list of endpoints,
	 * for one that matches the service and name.
	 * 
	 * @param endpoints the endpoints list to look in
	 * @param service the qualified name of the service
	 * @param name the name of the endpoint
	 * @return the matching endpoint or <code>null</code> if not found
	 */
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

	/**
	 * Search within the given list of endpoints,
	 * for those that match the given interface.
	 * 
	 * @param endpoints the endpoints list to look in
	 * @param interfaceName the qualified name of the interface
	 * @return an array of endpoints (can not be <code>null</code>
	 */
	protected ServiceEndpoint[] getEndpoints(List endpoints, QName interfaceName) {
		List ses = new ArrayList();
		for (Iterator it = endpoints.iterator(); it.hasNext();) {
			ServiceEndpoint se = (ServiceEndpoint) it.next();
			QName[] itfs = se.getInterfaces();
			if (itfs == null && se instanceof InternalEndpointImpl) {
				InternalEndpointImpl ie = (InternalEndpointImpl) se;
				RegistryComponent component = JbiContainer.Factory.getInstance().getRegistry().getComponent(ie.getComponent());
				Document doc = ((Component)component.getComponent()).getServiceDescription(ie);
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

	/**
	 * Search within the given list of endpoints,
	 * for those that match the given service.
	 * 
	 * @param endpoints the endpoints list to look in
	 * @param serviceName the qualified name of the service
	 * @return an array of endpoints (can not be <code>null</code>
	 */
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

	/**
	 * Remove endpoints registered by the given component from the list.
	 * 
	 * @param endpoints the list to remove endpoints from
	 * @param component the component name
	 */
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
