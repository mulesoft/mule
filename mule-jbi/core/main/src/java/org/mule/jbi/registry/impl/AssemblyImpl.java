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

import com.sun.java.xml.ns.jbi.ConnectionDocument.Connection;
import com.sun.java.xml.ns.jbi.ConnectionsDocument.Connections;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;
import com.sun.java.xml.ns.jbi.ServiceUnitDocument.ServiceUnit;
import org.mule.jbi.registry.Assembly;
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Unit;
import org.mule.jbi.util.IOUtils;

import javax.jbi.JBIException;
import javax.jbi.component.ServiceUnitManager;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: each SU should be deployed independently
 *       an an error should be thrown only if the whole fails
 *       
 * TODO: should manage duplicate SUs and reployment 
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 *
 */
public class AssemblyImpl extends AbstractEntry implements Assembly {

	private List units;
	private boolean isTransient;
	
	public AssemblyImpl() {
		this.units = new ArrayList();
	}
	
	public Unit getUnit(String name) {
		for (Iterator it = this.units.iterator(); it.hasNext();) {
			UnitImpl u = (UnitImpl) it.next();
			if (u.getName().equals(name)) {
				return u;
			}
		}
		return null;
	}
	
	public void addUnit(Unit unit) {
		this.units.add(unit);
	}
	
	public void removeUnit(Unit unit) {
		this.units.remove(unit);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#getUnits()
	 */
	public Unit[] getUnits() {
		Collection c = this.units;
		return (Unit[]) c.toArray(new Unit[c.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.mule.AbstractEntry#checkDescriptor()
	 */
	protected void checkDescriptor() throws JBIException {
		super.checkDescriptor();
		// Check that it is a service assembly
		if (!getDescriptor().isSetServiceAssembly()) {
			throw new JBIException("service-assembly should be set");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#deploy()
	 */
	public synchronized String deploy() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		try {
			Jbi jbi = getDescriptor();
			// Deploy service units
			ServiceUnit[] sua = jbi.getServiceAssembly().getServiceUnitArray();
			this.units = new ArrayList();
			for (int i = 0; i < sua.length; i++) {
				String suName = sua[i].getIdentification().getName();
				String artifactName = sua[i].getTarget().getArtifactsZip();
				String componentName = sua[i].getTarget().getComponentName();
				File artifact = new File(getInstallRoot(), artifactName);
				File installDir = new File(getInstallRoot(), componentName + File.separator + suName);
				// Check that artifact exists
				if (!artifact.isFile()) {
					throw new JBIException("Artifact file not found: " + sua[i].getTarget().getArtifactsZip());
				}
				// Check that component exists
				Component component = getRegistry().getComponent(componentName);
				if (component == null) {
					throw new JBIException("Service assembly requires a missing component: " + componentName);
				}
				// Check component is fully installed
				if (!component.getCurrentState().equals(RUNNING)) {
					throw new JBIException("Component is not started: " + componentName);
				}
				// Check that we can deploy onto it
				ServiceUnitManager mgr = component.getComponent().getServiceUnitManager();
				if (mgr == null) {
					throw new JBIException("Component does not accept deployments: " + componentName);
				}
				// Check for duplicate SU
				Unit[] compUnits = component.getUnits();
				for (int j = 0; j < compUnits.length; j++) {
					if (compUnits[i].getName().equals(suName)) {
						throw new JBIException("Service unit already installed on component: " + suName);
					}
				}
				// Unzip artifact
				IOUtils.unzip(artifact, installDir);
				// Create Unit
				UnitImpl unit = new UnitImpl();
				unit.setName(suName);
				unit.setAssembly(this);
				unit.setComponent(component);
				unit.setInstallRoot(installDir.getAbsolutePath());
				// Deploy this unit
				String result = unit.deploy();
				// TODO: analyse result
			}
			// Deploy connections
			if (jbi.getServiceAssembly().isSetConnections()) {
				Connections connections = jbi.getServiceAssembly().getConnections();
				Connection[] cns = connections.getConnectionArray();
				for (int i = 0; i < cns.length; i++) {
					QName  consItf = cns[i].getConsumer().getInterfaceName();
					QName  consSer = cns[i].getConsumer().getServiceName();
					String consEP  = cns[i].getConsumer().getEndpointName().getStringValue();
					QName  provSer = cns[i].getProvider().getServiceName();
					String provEP  = cns[i].getProvider().getEndpointName().getStringValue();
					// TODO: deploy connection info
				}
			}
			// Finish
			setCurrentState(SHUTDOWN);
			// TODO return info
			return null;
		} catch (Exception e) {
			// If we failed, undeploy
			undeploy();
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else if (e instanceof IOException) {
				throw (IOException) e;
			} else {
				throw new JBIException("Could not deploy assembly", e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#start()
	 */
	public synchronized String start() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!getCurrentState().equals(RUNNING)) {
			Unit[] units = getUnits();
			for (int i = 0; i < units.length; i++) {
				units[i].start();
			}
			setCurrentState(RUNNING);
		}
		// TODO
		return "";
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#stop()
	 */
	public synchronized String stop() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN) || getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!getCurrentState().equals(STOPPED)) {
			Unit[] units = getUnits();
			for (int i = 0; i < units.length; i++) {
				units[i].stop();
			}
			setCurrentState(STOPPED);
		}
		// TODO
		return "";
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#shutDown()
	 */
	public synchronized String shutDown() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!getCurrentState().equals(SHUTDOWN)) {
			stop();
			Unit[] units = getUnits();
			for (int i = 0; i < units.length; i++) {
				units[i].shutDown();
			}
			setCurrentState(SHUTDOWN);
		}
		// TODO
		return "";
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#undeploy()
	 */
	public synchronized String undeploy() throws JBIException, IOException {
		if (!getCurrentState().equals(SHUTDOWN) && !getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		Unit[] units = getUnits();
		for (int i = 0; i < units.length; i++) {
			String result = units[i].undeploy();
			// TODO: analyse result
		}
		IOUtils.deleteFile(new File(getInstallRoot()));
		getRegistry().removeAssembly(this);
		setCurrentState(UNKNOWN);
		// TODO: return info
		return null;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#isTransient()
	 */
	public boolean isTransient() {
		return isTransient;
	}

	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#restoreState()
	 */
	public void restoreState() throws JBIException, IOException {
		Unit[] units = getUnits();
		for (int i = 0; i < units.length; i++) {
			units[i].init();
			if (units[i].getStateAtShutdown().equals(Unit.RUNNING)) {
				units[i].start();
			} else if (units[i].getStateAtShutdown().equals(Unit.SHUTDOWN)) {
				units[i].shutDown();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#saveAndShutdown()
	 */
	public void saveAndShutdown() throws JBIException, IOException {
		Unit[] units = getUnits();
		for (int i = 0; i < units.length; i++) {
			units[i].setStateAtShutdown(units[i].getCurrentState());
			units[i].shutDown();
		}
	}

}
