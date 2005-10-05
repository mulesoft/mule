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

import org.mule.ManagementContext;
import org.mule.registry.ComponentType;
import org.mule.registry.RegistryComponent;

import javax.jbi.management.AdminServiceMBean;
import javax.management.ObjectName;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class AdminService implements AdminServiceMBean {

	protected ManagementContext context;
	
	public AdminService(ManagementContext context) {
		this.context = context;
	}
	
	protected ObjectName[] getComponents(ComponentType type) {
		RegistryComponent[] engines = context.getRegistry().getComponents(type);
		ObjectName[] names = new ObjectName[engines.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = engines[i].getObjectName();
		}
		return names;
	}

	public ObjectName[] getBindingComponents() {
        return getComponents(ComponentType.JBI_BINDING_COMPONENT);
	}

    public ObjectName[] getEngineComponents() {
        return getComponents(ComponentType.JBI_ENGINE_COMPONENT);
	}

	public ObjectName getComponentByName(String name) {
		RegistryComponent comp = context.getRegistry().getComponent(name);
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
		RegistryComponent comp = context.getRegistry().getComponent(componentName);
		return comp.getType().equals(ComponentType.JBI_BINDING_COMPONENT);
	}

    public boolean isEngine(String componentName) {
		RegistryComponent comp = context.getRegistry().getComponent(componentName);
		return comp.getType().equals(ComponentType.JBI_ENGINE_COMPONENT);
	}
}
