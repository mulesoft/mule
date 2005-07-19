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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
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

	public void parseWsdl(Document doc) {
		try {
			Set interfaces = new HashSet();
			Definition def = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, doc);
			Service service = def.getService(this.serviceName);
			Port port = service.getPort(this.endpointName);
			interfaces.add(port.getBinding().getQName());
			this.interfaces = (QName[]) interfaces.toArray(new QName[interfaces.size()]);
		} catch (Exception e) {
			this.interfaces = new QName[0];
		}
	}

}
