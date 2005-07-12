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

import javax.xml.namespace.QName;

public class InternalEndpointImpl extends AbstractServiceEndpoint {

	private String endpointName;
	private QName[] interfaces;
	private QName serviceName;
	
	public String getEndpointName() {
		return endpointName;
	}

	public void setEndpointName(String endpointName) {
		this.endpointName = endpointName;
	}

	public QName[] getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(QName[] interfaces) {
		this.interfaces = interfaces;
	}

	public QName getServiceName() {
		return serviceName;
	}

	public void setServiceName(QName serviceName) {
		this.serviceName = serviceName;
	}

}
