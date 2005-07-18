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

import java.io.IOException;

import javax.jbi.JBIException;
import javax.jbi.component.ServiceUnitManager;

import org.mule.jbi.registry.Assembly;
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Unit;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class UnitImpl extends AbstractEntry implements Unit {

	private String component;
	private String assembly;
	
	public UnitImpl() {
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#getComponent()
	 */
	public Component getComponent() {
		return getRegistry().getComponent(this.component);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#getAssemblies()
	 */
	public Assembly getAssembly() {
		return getRegistry().getAssembly(this.assembly);
	}

	public void setAssembly(Assembly assembly) {
		this.assembly = assembly.getName();
	}

	public void setComponent(Component component) {
		this.component = component.getName();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#deploy()
	 */
	public synchronized String deploy() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		String result = getServiceUnitManager().deploy(getName(), getInstallRoot());
		// TODO: analyse result
		((AbstractComponent) getComponent()).addUnit(this);
		((AssemblyImpl) getAssembly()).addUnit(this);
		setCurrentState(SHUTDOWN);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#start()
	 */
	public synchronized String start() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!getCurrentState().equals(RUNNING)) {
			if (getCurrentState().equals(SHUTDOWN)) {
				getServiceUnitManager().init(getName(), getInstallRoot());
				setCurrentState(STOPPED);
			}
			getServiceUnitManager().start(getName());
			setCurrentState(RUNNING);
		}
		// TODO
		return "";
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#stop()
	 */
	public synchronized String stop() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN) || getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!getCurrentState().equals(STOPPED)) {
			getServiceUnitManager().stop(getName());
			setCurrentState(STOPPED);
		}
		// TODO:
		return "";
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#shutDown()
	 */
	public synchronized String shutDown() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!getCurrentState().equals(SHUTDOWN)) {
			stop();
			getServiceUnitManager().shutDown(getName());
			setCurrentState(SHUTDOWN);
		}
		// TODO
		return "";
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Unit#undeploy()
	 */
	public synchronized String undeploy() throws JBIException, IOException {
		if (!getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		String result = getServiceUnitManager().undeploy(getName(), getInstallRoot());
		// TODO: analyse result
		((AbstractComponent) getComponent()).removeUnit(this);
		((AssemblyImpl) getAssembly()).removeUnit(this);
		setCurrentState(UNKNOWN);
		return result;
	}
	
	protected ServiceUnitManager getServiceUnitManager() {
		return getComponent().getComponent().getServiceUnitManager();
	}

}
