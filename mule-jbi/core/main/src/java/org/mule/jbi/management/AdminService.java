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
import org.mule.jbi.registry.Binding;
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Engine;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class AdminService implements AdminServiceMBean {

	protected JbiContainer container;
	
	public AdminService(JbiContainer container) {
		this.container = container;
	}
	
	public ObjectName[] getEngineComponents() {
		Engine[] engines = this.container.getRegistry().getEngines();
		ObjectName[] names = new ObjectName[engines.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = engines[i].getObjectName();
		}
		return names;
	}

	public ObjectName[] getBindingComponents() {
		Binding[] bindings = this.container.getRegistry().getBindings();
		ObjectName[] names = new ObjectName[bindings.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = bindings[i].getObjectName();
		}
		return names;
	}

	public ObjectName getComponentByName(String name) {
		Component comp = this.container.getRegistry().getComponent(name);
		if (comp != null) {
			return comp.getObjectName();
		}
		return null;
	}

	public String getSystemInfo() {
		return "Mule JBI version 0.1";
	}

	public ObjectName getSystemService(String serviceName) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectName[] getSystemServices() {
		// TODO Auto-generated method stub
		return new ObjectName[0];
	}

	public boolean isBinding(String componentName) {
		Component comp = this.container.getRegistry().getComponent(componentName);
		return comp instanceof Binding;
	}

	public boolean isEngine(String componentName) {
		Component comp = this.container.getRegistry().getComponent(componentName);
		return comp instanceof Engine;
	}

}
