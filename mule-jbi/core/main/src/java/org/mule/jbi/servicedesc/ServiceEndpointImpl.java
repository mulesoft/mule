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
package org.mule.jbi.servicedesc;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;

public class ServiceEndpointImpl implements ServiceEndpoint {

	private String endpointName;
	private QName[] interfaces;
	private QName serviceName;
	
	public DocumentFragment getAsReference(QName operationName) {
		// TODO Auto-generated method stub
		return null;
	}

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
