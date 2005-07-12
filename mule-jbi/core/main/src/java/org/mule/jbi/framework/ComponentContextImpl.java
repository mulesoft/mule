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

import org.mule.jbi.servicedesc.AbstractServiceEndpoint;
import org.mule.jbi.servicedesc.DynamicEndpointImpl;
import org.mule.jbi.servicedesc.ExternalEndpointImpl;
import org.mule.jbi.servicedesc.InternalEndpointImpl;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class ComponentContextImpl implements ComponentContext, MBeanNames {

	private ComponentInfo info;
	
	public ComponentContextImpl(ComponentInfo info) {
		this.info = info;
	}
	
	public ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
		InternalEndpointImpl se = new InternalEndpointImpl();
		se.setServiceName(serviceName);
		se.setEndpointName(endpointName);
		se.setComponent(this.info.name);
		se.setActive(true);
		this.info.container.getEndpointRegistry().registerInternalEndpoint(se);
		return se;
	}

	public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
		this.info.container.getEndpointRegistry().unregisterInternalEndpoint(endpoint);
	}

	public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		ExternalEndpointImpl se = new ExternalEndpointImpl();
		se.setEndpoint(externalEndpoint);
		se.setComponent(this.info.name);
		se.setActive(true);
		this.info.container.getEndpointRegistry().registerExternalEndpoint(se);
	}

	public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		this.info.container.getEndpointRegistry().unregisterExternalEndpoint(externalEndpoint);
	}

	public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
		ComponentInfo[] components = this.info.container.getComponentRegistry().getComponents();
		for (int i = 0; i < components.length; i++) {
			ServiceEndpoint se = components[i].component.resolveEndpointReference(epr);
			if (se != null) {
				return new DynamicEndpointImpl(components[i].name, se);
			}
		}
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
		// Translate endpoint
		ServiceEndpoint se = this.info.container.getEndpointRegistry().getEndpoint(endpoint.getServiceName(), endpoint.getEndpointName());
		// If endpoint is registered
		if (se instanceof AbstractServiceEndpoint) {
			// Query for the component
			String component = ((AbstractServiceEndpoint) se).getComponent();
			ComponentInfo info = this.info.container.getComponentRegistry().getComponent(component);
			if (info != null) {
				// Delegate to the component
				return info.component.getServiceDescription(endpoint);
			}
		}
		return null;
	}

	public ServiceEndpoint[] getEndpoints(QName interfaceName) {
		return this.info.container.getEndpointRegistry().getInternalEndpoints(interfaceName);
	}

	public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
		return this.info.container.getEndpointRegistry().getInternalEndpointsForService(serviceName);
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
		return this.info.container.getNamingContext();
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
