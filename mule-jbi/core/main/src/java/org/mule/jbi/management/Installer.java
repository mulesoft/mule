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

import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.ManagementContext;
import org.mule.jbi.JbiContainer;
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.InstallationContext;
import javax.jbi.management.InstallerMBean;
import javax.jbi.management.LifeCycleMBean;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class Installer implements InstallerMBean, InstallationContext, ComponentContext, MBeanNames {

	private transient Log logger = LogFactory.getLog(getClass()); 
	
	private JbiContainer container;
	private File installRoot;
	private Jbi jbi;
	private Bootstrap bootstrap;
	private RegistryComponent component;
	private boolean install;
    private ManagementContext context;

	public Installer(ManagementContext context, RegistryComponent component) throws Exception {
		this.context = context;
		this.component = component;
		this.installRoot = new File(this.component.getInstallRoot());
		this.jbi = (Jbi)component.getDescriptor().getConfiguration();
		this.install = true;
        if(jbi.getComponent().getComponentClassPath()!=null) {
            List l = Arrays.asList(jbi.getComponent().getComponentClassPath().getPathElementArray());
            this.component.setClassPathElements(l);
        }
	}
	
	protected ClassLoader createBootstrapClassLoader() throws Exception {
		if (this.jbi.getComponent().isSetBootstrapClassLoaderDelegation()) {
			// TODO: use this
		}
        List path = new ArrayList();
        if(jbi.getComponent().getComponentClassPath()!=null) {
            path = Arrays.asList(jbi.getComponent().getBootstrapClassPath().getPathElementArray());
        }
		return createClassLoader(path);
	}
	
	protected Bootstrap createBootstrap() throws Exception {
		ClassLoader loader = createBootstrapClassLoader();
		String bootstrapClassname = this.jbi.getComponent().getBootstrapClassName();
        if(bootstrapClassname==null) return null;
		Class clazz = Class.forName(bootstrapClassname, true, loader);
		Bootstrap bs = (Bootstrap) clazz.newInstance();
		return bs;
	}
	
	protected ClassLoader createComponentClassLoader() throws Exception {
		if (this.jbi.getComponent().isSetComponentClassLoaderDelegation()) {
			// TODO: use this
		}
		return createClassLoader(this.component.getClassPathElements());
	}
	
	protected ClassLoader createClassLoader(List classPath) throws Exception {
		List bsCpUrls = new ArrayList();
		for (Iterator it = classPath.iterator(); it.hasNext();) {
			String cpElement = (String) it.next();
			bsCpUrls.add(new File(this.installRoot, cpElement).toURL());
		}
		ClassLoader loader = new URLClassLoader((URL[]) bsCpUrls.toArray(new URL[bsCpUrls.size()]));
		return loader;
	}
	
	protected Component createComponent() throws Exception {
		ClassLoader loader = createComponentClassLoader();
		Class cl = Class.forName(getComponentClassName(), true, loader);
		Component c = (Component) cl.newInstance();
		return c;
	}
	
	public void init() throws Exception {
		boolean success = false;
		try {
			this.bootstrap = createBootstrap();
            if(bootstrap!=null) {
			    this.bootstrap.init(this);
            }
			success = true;
		} finally {
			if (!success && this.bootstrap != null) {
				this.bootstrap.cleanUp();
			}
		}
	}
	
	public DocumentFragment getInstallationDescriptorExtension() {
		Node node = Installer.this.jbi.getComponent().getDomNode();
		DocumentFragment d = node.getOwnerDocument().createDocumentFragment();
		NodeList l = node.getChildNodes();
		for (int i = 0; i < l.getLength(); i++) {
			Node c = l.item(i);
			String uri = Installer.this.jbi.getDomNode().getNamespaceURI();
			if (!uri.equals(c.getNamespaceURI())) {
				d.appendChild(c.cloneNode(true));
			}
		}
		return d;
	}

	public synchronized String getInstallRoot() {
		return installRoot.getAbsolutePath();
	}

	public synchronized ObjectName install() throws JBIException {
		try {
			this.bootstrap.onInstall();
			this.component.install();
			return this.component.getObjectName();
		} catch (Error e) {
			logger.info("An error occured during install", e);
			throw e;
		} catch (Exception e) {
			logger.info("An error occured during install", e);
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
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
		} catch (RegistryException e) {
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
		return this.jbi.getComponent().getComponentClassName().getDomNode().getFirstChild().getNodeValue();
	}

	public List getClassPathElements() {
		return this.component.getClassPathElements();
	}

	public String getComponentName() {
		return this.jbi.getComponent().getIdentification().getName();
	}

	public ComponentContext getContext() {
		return this;
	}

	public boolean isInstall() {
		return this.install;
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
