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
package org.mule.jbi.registry;

import com.sun.java.xml.ns.jbi.ConnectionDocument.Connection;
import com.sun.java.xml.ns.jbi.ConnectionsDocument.Connections;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;
import com.sun.java.xml.ns.jbi.ServiceUnitDocument.ServiceUnit;
import org.mule.registry.Registry;
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.Unit;
import org.mule.registry.impl.AbstractAssembly;
import org.mule.registry.impl.AbstractUnit;
import org.mule.util.Utility;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ServiceUnitManager;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * TODO: each SU should be deployed independently
 *       an an error should be thrown only if the whole fails
 *       
 * TODO: should manage duplicate SUs and reployment 
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 *
 */
public class JbiAssembly extends AbstractAssembly  {

    public JbiAssembly(Registry registry) {
        super(registry);
    }

    public RegistryDescriptor getDescriptor() throws RegistryException {
        if(descriptor==null) {
            descriptor = new JbiDescriptor(this.getInstallRoot());
        }
        return descriptor;
    }

	public Unit getUnit(String name) {
		for (Iterator it = this.units.iterator(); it.hasNext();) {
			AbstractUnit u = (AbstractUnit) it.next();
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
	protected void checkDescriptor() throws RegistryException {
		super.checkDescriptor();
		// Check that it is a service assembly
		if (!getDescriptor().isServiceAssembly()) {
			throw new RegistryException("service-assembly should be set");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Assembly#deploy()
	 */
	public synchronized String deploy() throws RegistryException {
		if (!getCurrentState().equals(UNKNOWN)) {
			throw new RegistryException("Illegal status: " + getCurrentState());
		}
		try {
			Jbi jbi = (Jbi)getDescriptor().getConfiguration();
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
				RegistryComponent component = getRegistry().getComponent(componentName);
				if (component == null) {
					throw new JBIException("Service assembly requires a missing component: " + componentName);
				}
				// Check component is fully installed
				if (!component.getCurrentState().equals(RUNNING)) {
					throw new JBIException("Component is not started: " + componentName);
				}
				// Check that we can deploy onto it
				ServiceUnitManager mgr = ((Component)component.getComponent()).getServiceUnitManager();
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
				Utility.unzip(artifact, installDir);
				// Create Unit
				Unit unit = registry.createUnit(suName);
				unit.setAssembly(this);
				unit.setRegistryComponent(component);
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
			if (e instanceof RegistryException) {
				throw (RegistryException) e;
			} else {
				throw new RegistryException("Could not deploy assembly", e);
			}
		}
	}
}
