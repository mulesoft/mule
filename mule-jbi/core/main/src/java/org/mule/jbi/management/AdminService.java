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

import javax.jbi.management.AdminServiceMBean;
import javax.management.ObjectName;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.framework.ComponentInfo;

public class AdminService implements AdminServiceMBean {

	private JbiContainer container;
	
	public AdminService(JbiContainer container) {
		this.container = container;
	}
	
	public ObjectName[] getEngineComponents() {
		return this.container.getComponentRegistry().getEngineComponentNames();
	}

	public ObjectName[] getBindingComponents() {
		return this.container.getComponentRegistry().getBindingComponentNames();
	}

	public ObjectName getComponentByName(String name) {
		return this.container.getComponentRegistry().getComponentName(name);
	}

	public String getSystemInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectName getSystemService(String serviceName) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectName[] getSystemServices() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isBinding(String componentName) {
		ComponentInfo cci = this.container.getComponentRegistry().getComponent(componentName);
		if (cci != null) {
			return !cci.isEngine();
		}
		return false;
	}

	public boolean isEngine(String componentName) {
		ComponentInfo cci = this.container.getComponentRegistry().getComponent(componentName);
		if (cci != null) {
			return cci.isEngine();
		}
		return false;
	}

}
