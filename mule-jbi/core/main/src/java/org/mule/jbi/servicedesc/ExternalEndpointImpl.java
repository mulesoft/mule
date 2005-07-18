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
package org.mule.jbi.servicedesc;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class ExternalEndpointImpl extends AbstractServiceEndpoint {

	private ServiceEndpoint endpoint;

	public ServiceEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(ServiceEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public String getEndpointName() {
		return this.endpoint.getEndpointName();
	}

	public QName[] getInterfaces() {
		return this.endpoint.getInterfaces();
	}

	public QName getServiceName() {
		return this.endpoint.getServiceName();
	}
	
}
