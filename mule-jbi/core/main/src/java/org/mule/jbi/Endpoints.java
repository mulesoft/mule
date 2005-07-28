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
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

/**
 * 
 * This interface acts as a registry for all endpoints.
 * It features endpoints registration / unregistration and queries.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Endpoints {

	void registerInternalEndpoint(ServiceEndpoint endpoint) throws JBIException;

	void unregisterInternalEndpoint(ServiceEndpoint endpoint) throws JBIException;

	void registerExternalEndpoint(ServiceEndpoint endpoint) throws JBIException;

	void unregisterExternalEndpoint(ServiceEndpoint endpoint) throws JBIException;
	
	ServiceEndpoint[] getInternalEndpoints(QName interfaceName);
	
	ServiceEndpoint[] getInternalEndpointsForService(QName serviceName);
	
	ServiceEndpoint[] getExternalEndpoints(QName interfaceName);
	
	ServiceEndpoint[] getExternalEndpointsForService(QName serviceName);
	
	ServiceEndpoint getEndpoint(QName service, String name);

	/**
	 * Remove all endpoints registered by a component.
	 * 
	 * @param component the component name
	 */
	void unregisterEndpoints(String component);
}
