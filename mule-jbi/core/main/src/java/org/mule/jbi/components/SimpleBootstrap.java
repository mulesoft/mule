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
package org.mule.jbi.components;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.InstallationContext;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleBootstrap implements Bootstrap {

	protected Log logger = LogFactory.getLog(getClass());
	protected InstallationContext installContext;
	protected ObjectName mbeanName;
	
	
	/* (non-Javadoc)
	 * @see javax.jbi.component.Bootstrap#init(javax.jbi.component.InstallationContext)
	 */
	public final void init(InstallationContext installContext) throws JBIException {
		try {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Enter init");
			}
			if (installContext == null) {
				throw new JBIException("null installationContext");
			}
			this.installContext = installContext;
			doInit();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Exit init");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling init", e);
		}
	}
	
	/**
	 * Placeholder for custom component bootstrap init.
	 * Default implementation will register a custom MBean
	 * if existing.
	 * @throws Exception if an error occurs
	 */
	protected void doInit() throws Exception {
		Object mbean = getExtensionMBean();
		if (mbean != null) {
			this.mbeanName = this.installContext.getContext().getMBeanNames().createCustomComponentMBeanName("bootstrap");
			MBeanServer server = this.installContext.getContext().getMBeanServer();
			if (server == null) {
				throw new JBIException("null mBeanServer");
			}
			if (server.isRegistered(this.mbeanName)) {
				server.unregisterMBean(this.mbeanName);
			}
			server.registerMBean(mbean, this.mbeanName);
		}
	}
	
	/**
	 * 
	 * @return the bootstrap custom MBean.
	 * @throws Exception if an error occurs
	 */
	protected Object getExtensionMBean() throws Exception {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.Bootstrap#cleanUp()
	 */
	public final void cleanUp() throws JBIException {
		try {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Enter cleanUp");
			}
			doCleanUp();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Exit cleanUp");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling cleanUp", e);
		}
	}
	
	protected void doCleanUp() throws Exception {
		if (this.mbeanName != null) {
			MBeanServer server = this.installContext.getContext().getMBeanServer();
			if (server == null) {
				throw new JBIException("null mBeanServer");
			}
			if (server.isRegistered(this.mbeanName)) {
				server.unregisterMBean(this.mbeanName);
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.Bootstrap#getExtensionMBeanName()
	 */
	public final ObjectName getExtensionMBeanName() {
		return this.mbeanName;
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.Bootstrap#onInstall()
	 */
	public final void onInstall() throws JBIException {
		try {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Enter onInstall");
			}
			doInstall();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Exit onInstall");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling onInstall", e);
		}
	}
	
	protected void doInstall() throws Exception {
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.Bootstrap#onUninstall()
	 */
	public final void onUninstall() throws JBIException {
		try {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Enter onUninstall");
			}
			doUninstall();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Exit onUninstall");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling onUninstall", e);
		}
	}

	protected void doUninstall() throws Exception {
	}

}
