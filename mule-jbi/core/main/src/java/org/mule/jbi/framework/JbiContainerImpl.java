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

import java.io.File;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.management.AdminServiceMBean;
import javax.jbi.management.DeploymentServiceMBean;
import javax.jbi.management.InstallationServiceMBean;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.Endpoints;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.Messaging;
import org.mule.jbi.Router;
import org.mule.jbi.management.AdminService;
import org.mule.jbi.management.DeploymentService;
import org.mule.jbi.management.Directories;
import org.mule.jbi.management.InstallationService;
import org.mule.jbi.registry.Registry;
import org.mule.jbi.registry.RegistryIO;
import org.mule.jbi.routing.RouterImpl;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class JbiContainerImpl implements JbiContainer {

	private static final String DEFAULT_WORKING_DIR = ".mule-jbi";
	private static final String DEFAULT_JMX_DOMAIN = "mule-jbi";
	
	private static final Log LOGGER = LogFactory.getLog(JbiContainer.class);
	
	private MBeanServer mBeanServer;
	private TransactionManager transactionManager;
	private String jmxDomainName;
	private Router router;
	private Registry registry;
	private Endpoints endpoints;
	private Messaging messaging;
	private File workingDir;
	
	public JbiContainerImpl() {
		this.workingDir = new File(DEFAULT_WORKING_DIR);
		this.jmxDomainName = DEFAULT_JMX_DOMAIN;
	}
	
	public MBeanServer getMBeanServer() {
		return this.mBeanServer;
	}

	public void setMBeanServer(MBeanServer mBeanServer) {
		this.mBeanServer = mBeanServer;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	public String getJmxDomainName() {
		return this.jmxDomainName;
	}

	public void setJmxDomainName(String jmxDomainName) {
		this.jmxDomainName = jmxDomainName;
	}

	public Router getRouter() {
		return this.router;
	}

	public void setRouter(Router router) {
		this.router = router;
	}
	
	public InitialContext getNamingContext() {
		// TODO: handle naming context
		return null;
	}
	
	public File getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}
	
	public ObjectName createMBeanName(String componentName, String type, String name) {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(this.jmxDomainName).append(':');
			if (componentName != null) {
				sb.append("component=").append(validateString(componentName));
				sb.append(',');
			}
			sb.append("type=").append(validateString(type));
			if (name != null) {
				sb.append(',');
				sb.append("name=").append(validateString(name));
			}
			return new ObjectName(sb.toString());
		} catch (MalformedObjectNameException e) {
			LOGGER.error("Could not create component mbean name", e);
			return null;
		}
	}

    private String validateString(String str) {
    	str = str.replace(':', '_');
    	str = str.replace('/', '_');
    	str = str.replace('\\', '_');
    	return str;
    }

	public Registry getRegistry() {
		return this.registry;
	}

	public Endpoints getEndpoints() {
		return this.endpoints;
	}

	public Messaging getMessaging() {
		return this.messaging;
	}
	
	public void initialize() throws JBIException {
		try {
			JbiContainer.Factory.setInstance(this);
			Directories.createDirectories(this.workingDir);
			if (this.mBeanServer == null) {
				LOGGER.debug("Creating MBeanServer");
				List l = MBeanServerFactory.findMBeanServer(null);
				if (l != null && l.size() > 0) {
					this.mBeanServer = (MBeanServer) l.get(0);
				} else {
					this.mBeanServer = MBeanServerFactory.createMBeanServer();
				}
			}
			this.endpoints = new EndpointsImpl();
			File regStore = new File(this.workingDir, "/registry.xml");
			if (regStore.isFile()) {
				this.registry = RegistryIO.load(regStore);
			} else {
				this.registry = RegistryIO.create(regStore);
			}
			if (this.router == null) {
				this.router = new RouterImpl();
			}
		} catch (Exception e) {
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
		}
	}
	
	public void start() throws JBIException {
		try {
			LOGGER.info("Starting JBI");
			if (this.registry == null) {
				initialize();
			}
			AdminService admin = new AdminService(this);
			registerMBean(new StandardMBean(admin, AdminServiceMBean.class), createMBeanName(null, "service", "admin"));
			InstallationService install = new InstallationService(this);
			registerMBean(new StandardMBean(install, InstallationServiceMBean.class), createMBeanName(null, "service", "install"));
			DeploymentService deploy = new DeploymentService(this);
			registerMBean(new StandardMBean(deploy, DeploymentServiceMBean.class), createMBeanName(null, "service", "deploy"));
			this.registry.start();
		} catch (Exception e) {
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
		}
	}
	
	public void shutDown() throws JBIException {
		try {
			unregisterMBean(createMBeanName(null, "service", "admin"));
			unregisterMBean(createMBeanName(null, "service", "install"));
			unregisterMBean(createMBeanName(null, "service", "deploy"));
			this.registry.shutDown();
			JbiContainer.Factory.setInstance(null);
		} catch (Exception e) {
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
		}
	}
	
	private void unregisterMBean(ObjectName name) throws JMException {
		if (this.mBeanServer.isRegistered(name)) {
			this.mBeanServer.unregisterMBean(name);
		}
	}
	
	private void registerMBean(Object mbean, ObjectName name) throws JMException {
		unregisterMBean(name);
		this.mBeanServer.registerMBean(mbean, name);
	}

}
