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
package org.mule.jbi.management;

import org.mule.jbi.JbiContainer;
import org.mule.registry.RegistryComponent;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.InstallationContext;
import javax.jbi.management.LifeCycleMBean;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Logger;

public class InstallationContextImpl implements InstallationContext, ComponentContext, MBeanNames {

	private Bootstrap bootstrap;
	private RegistryComponent component;
	private JbiContainer container; 
	
	public InstallationContextImpl(RegistryComponent component, Bootstrap bootstrap) {
		this.component = component;
		this.bootstrap = bootstrap;
		this.container = JbiContainer.Factory.getInstance();
	}
	
	public DocumentFragment getInstallationDescriptorExtension() {
		return null;
	}

	public synchronized String getInstallRoot() {
		return this.component.getInstallRoot();
	}

	public synchronized ObjectName install() throws JBIException {
		try {
			this.bootstrap.onInstall();
			this.component.install();
			return this.component.getObjectName();
		} catch (Exception e) {
			throw new JBIException(e);
		} finally {
			this.bootstrap.cleanUp();
		}
	}

	public synchronized boolean isInstalled() {
		try {
			return !this.component.getCurrentState().equals(LifeCycleMBean.UNKNOWN);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void uninstall() throws JBIException {
		try {
			this.component.uninstall();
		} catch (Exception e) {
			throw new JBIException(e);
		}
	}

	public synchronized ObjectName getInstallerConfigurationMBean() throws JBIException {
		if (this.bootstrap != null) {
			return this.bootstrap.getExtensionMBeanName();
		}
		return null;
	}

	public String getComponentClassName() {
		return this.component.getComponent().getClass().getName();
	}

	public List getClassPathElements() {
		return this.component.getClassPathElements();
	}

	public String getComponentName() {
		return this.component.getName();
	}

	public ComponentContext getContext() {
		return this;
	}

	public boolean isInstall() {
		return true;
	}

	public void setClassPathElements(List classPathElements) {
		this.component.setClassPathElements(classPathElements);
	}

	public ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public DeliveryChannel getDeliveryChannel() throws MessagingException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public ServiceEndpoint getEndpoint(QName service, String name) {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public ServiceEndpoint[] getEndpoints(QName interfaceName) {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
		throw new IllegalStateException("Illegal call in an installation container");
	}

	public MBeanNames getMBeanNames() {
		return this;
	}

	public MBeanServer getMBeanServer() {
		return this.container.getMBeanServer();
	}

	public InitialContext getNamingContext() {
		return this.container.getNamingContext();
	}

	public Object getTransactionManager() {
		return this.container.getTransactionManager();
	}

	public String getWorkspaceRoot() {
		return this.component.getWorkspaceRoot();
	}

	public ObjectName createCustomComponentMBeanName(String customName) {
		return this.container.createMBeanName(getComponentName(), "custom", customName);
	}

	public String getJmxDomainName() {
		return this.container.getJmxDomainName();
	}
}
