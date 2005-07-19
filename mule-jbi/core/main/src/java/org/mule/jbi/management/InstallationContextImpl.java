package org.mule.jbi.management;

import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Logger;

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

import org.mule.jbi.JbiContainer;
import org.mule.jbi.registry.Component;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class InstallationContextImpl implements InstallationContext, ComponentContext, MBeanNames {

	private Bootstrap bootstrap;
	private Component component;
	private JbiContainer container; 
	
	public InstallationContextImpl(Component component, Bootstrap bootstrap) {
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
		} catch (IOException e) {
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
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public DeliveryChannel getDeliveryChannel() throws MessagingException {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public ServiceEndpoint getEndpoint(QName service, String name) {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public ServiceEndpoint[] getEndpoints(QName interfaceName) {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
		throw new IllegalStateException("Illegal call in an installation context");
	}

	public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
		throw new IllegalStateException("Illegal call in an installation context");
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
