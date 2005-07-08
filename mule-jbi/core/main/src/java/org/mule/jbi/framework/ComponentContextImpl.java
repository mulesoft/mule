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

import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.mule.jbi.servicedesc.ServiceEndpointImpl;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class ComponentContextImpl implements ComponentContext, MBeanNames {

	private ComponentInfo info;
	
	public ComponentContextImpl(ComponentInfo info) {
		this.info = info;
	}
	
	public ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
		ServiceEndpointImpl se = new ServiceEndpointImpl();
		se.setServiceName(serviceName);
		se.setEndpointName(endpointName);
		this.info.container.getEndpointRegistry().registerInternalEndpoint(this.info.name, se);
		return se;
	}

	public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
		this.info.container.getEndpointRegistry().unregisterInternalEndpoint(this.info.name, endpoint);
	}

	public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		this.info.container.getEndpointRegistry().registerExternalEndpoint(this.info.name, externalEndpoint);
	}

	public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		this.info.container.getEndpointRegistry().unregisterExternalEndpoint(this.info.name, externalEndpoint);
	}

	public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getComponentName() {
		return this.info.name;
	}

	public DeliveryChannel getDeliveryChannel() throws MessagingException {
		return this.info.channel;
	}

	public ServiceEndpoint getEndpoint(QName service, String name) {
		return this.info.container.getEndpointRegistry().getEndpoint(service, name);
	}

	public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
		// TODO Auto-generated method stub
		return null;
	}

	public ServiceEndpoint[] getEndpoints(QName interfaceName) {
		return this.info.container.getEndpointRegistry().getEndpoints(interfaceName);
	}

	public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
		return this.info.container.getEndpointRegistry().getEndpointsForService(serviceName);
	}

	public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
		return this.info.container.getEndpointRegistry().getExternalEndpoints(interfaceName);
	}

	public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
		return this.info.container.getEndpointRegistry().getExternalEndpointsForService(serviceName);
	}

	public String getInstallRoot() {
		return this.info.installRoot;
	}

	public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
		StringBuffer sb = new StringBuffer();
		sb.append("org.mule.jbi.components.");
		sb.append(this.info.name);
		if (suffix.length() > 0) {
			sb.append(".");
			sb.append(suffix);
		}
		return Logger.getLogger(sb.toString(), resourceBundleName);
	}

	public MBeanNames getMBeanNames() {
		return this;
	}

	public MBeanServer getMBeanServer() {
		return this.info.container.getMBeanServer();
	}

	public InitialContext getNamingContext() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getTransactionManager() {
		return this.info.container.getTransactionManager();
	}

	public String getWorkspaceRoot() {
		return this.info.workspaceRoot;
	}

	public ObjectName createCustomComponentMBeanName(String customName) {
		return this.info.container.createComponentMBeanName(this.info.name, "custom", customName);
	}

	public String getJmxDomainName() {
		return this.info.container.getJmxDomainName();
	}

}
