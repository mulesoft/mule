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
package org.mule.jbi;

import java.io.File;

import javax.jbi.JBIException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.framework.ComponentRegistry;
import org.mule.jbi.framework.EndpointRegistry;
import org.mule.jbi.routing.Router;

public class JbiContainer {

	private static final String DEFAULT_WORKING_DIR = ".mule-jbi";
	private static final String DEFAULT_JMX_DOMAIN = "mule-jbi";
	
	private static final Log LOGGER = LogFactory.getLog(JbiContainer.class);
	
	private MBeanServer mBeanServer;
	private TransactionManager transactionManager;
	private String jmxDomainName;
	private Router router;
	private File workingDir;
	private ComponentRegistry componentRegistry;
	private EndpointRegistry endpointRegistry;
	
	public JbiContainer() {
		this.workingDir = new File(DEFAULT_WORKING_DIR);
		this.jmxDomainName = DEFAULT_JMX_DOMAIN;
	}
	
	public void start() throws JBIException {
		if (this.workingDir == null) {
			throw new JBIException("Working directory must not be set to null");
		}
		if (!this.workingDir.isDirectory()) {
			if (!this.workingDir.mkdirs()) {
				throw new JBIException("Could not create working directory");
			}
		}
		this.endpointRegistry = new EndpointRegistry(this);
		this.componentRegistry = new ComponentRegistry(this);
		this.endpointRegistry.start();
		this.componentRegistry.start();
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
	
	public ObjectName createComponentMBeanName(String componentName, String type, String name) {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(this.jmxDomainName).append(':');
			sb.append("component=").append(validateString(componentName));
			sb.append(',');
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

	public ComponentRegistry getComponentRegistry() {
		return componentRegistry;
	}

	public EndpointRegistry getEndpointRegistry() {
		return endpointRegistry;
	}

}
