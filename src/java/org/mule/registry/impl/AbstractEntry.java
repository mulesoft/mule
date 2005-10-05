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
package org.mule.registry.impl;

import org.mule.registry.Registry;
import org.mule.registry.Entry;
import org.mule.registry.RegistryException;

import java.io.IOException;
import java.io.Serializable;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public abstract class AbstractEntry implements Entry, Serializable {


	protected transient String currentState;
	protected String name;
	protected String installRoot;
	protected String stateAtShutdown;
    protected transient Registry registry;

    protected AbstractEntry(Registry registry) {
		this.currentState = UNKNOWN;
		this.stateAtShutdown = UNKNOWN;
        this.registry = registry;
	}

	protected void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.currentState = UNKNOWN;
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

	public void setCurrentState(String currentState) throws RegistryException {
		this.currentState = currentState;
		getRegistry().save();
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

	public Registry getRegistry() {
		return registry;
	}

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    protected void checkDescriptor() throws RegistryException {

    }

}
