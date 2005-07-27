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
package org.mule.jbi.registry.impl;

import com.sun.java.xml.ns.jbi.JbiDocument;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.registry.Entry;
import org.mule.jbi.registry.Registry;
import org.mule.jbi.registry.RegistryIO;

import javax.jbi.JBIException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public abstract class AbstractEntry implements Entry, Serializable {

	public static final String JBI_DESCRIPTOR = "META-INF/jbi.xml";
	
	private transient Jbi descriptor;
	private transient String currentState;
	private String name;
	private String installRoot;
	private String stateAtShutdown;
	
	public AbstractEntry() {
		this.currentState = UNKNOWN;
		this.stateAtShutdown = UNKNOWN;
	}

	protected void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.currentState = UNKNOWN;
	}
	
	public Jbi getDescriptor() throws JBIException {
		try {
			if (this.descriptor == null) {
				// Install root must be set
				if (this.installRoot == null) {
					throw new IllegalStateException("installRoot must be set");
				}
				File jbiFile = new File(this.installRoot, JBI_DESCRIPTOR);
				if (!jbiFile.isFile()) {
					throw new FileNotFoundException(jbiFile.getAbsolutePath());
				}
				this.descriptor = JbiDocument.Factory.parse(jbiFile).getJbi();
				checkDescriptor();
			}
			return this.descriptor;
		} catch (JBIException e) {
			throw (JBIException) e;
		} catch (Exception e) {
			throw new JBIException(e);
		}
	}
	
	public void setDescriptor(Jbi descriptor) throws JBIException {
		if (this.descriptor != null || !getCurrentState().equals(UNKNOWN)) {
			throw new IllegalStateException();
		}
		this.descriptor = descriptor;
		checkDescriptor();
	}
	
	/**
	 * Check that the loaded jbi descriptor is valid.
	 * @throws Exception
	 */
	protected void checkDescriptor() throws JBIException {
		// Check version number
		if (this.descriptor.getVersion().doubleValue() != 1.0) {
			throw new JBIException("version attribute should be '1.0'");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Entry#getName()
	 */
	public String getName() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Entry#getInstallRoot()
	 */
	public String getInstallRoot() {
		return this.installRoot;
	}

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#getCurrentState()
	 */
	public synchronized String getCurrentState() {
		return this.currentState;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Entry#getStatusAtShutdown()
	 */
	public String getStateAtShutdown() {
		return this.stateAtShutdown;
	}

	public void setCurrentState(String currentState) throws IOException {
		this.currentState = currentState;
		RegistryIO.save(getRegistry());
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Entry#setInstallRoot(java.lang.String)
	 */
	public void setInstallRoot(String installRoot) {
		this.installRoot = installRoot;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStateAtShutdown(String statusAtShutdown) {
		this.stateAtShutdown = statusAtShutdown;
	}

	public JbiContainer getContainer() {
		JbiContainer container = JbiContainer.Factory.getInstance();
		if (container == null) {
			throw new IllegalStateException("JbiContainer instance not defined");
		}
		return container;
	}
	
	public Registry getRegistry() {
		return getContainer().getRegistry();
	}

}
