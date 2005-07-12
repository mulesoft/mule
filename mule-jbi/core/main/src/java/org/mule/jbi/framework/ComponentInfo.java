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
package org.mule.jbi.framework;

import java.util.HashSet;
import java.util.Set;

import javax.jbi.component.Component;
import javax.jbi.messaging.DeliveryChannel;
import javax.management.ObjectName;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.componentRegistry.EntryDocument.Entry;
import org.mule.jbi.messaging.DeliveryChannelImpl;

public class ComponentInfo {

	JbiContainer container;
	Entry entry;
	Set libraries;
	ComponentContextImpl context;
	String name;
	ObjectName objectName;
	Component component;
	String installRoot;
	String workspaceRoot;
	DeliveryChannel channel;

	public ComponentInfo(JbiContainer container, Entry entry) {
		this.container = container;
		this.entry = entry;
		this.libraries = new HashSet();
		this.name = entry.getName().getStringValue();
	}

	public Component getComponent() {
		return this.component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}

	public ComponentContextImpl getContext() {
		if (this.context == null) {
			this.context = new ComponentContextImpl(this);
			this.channel = new DeliveryChannelImpl(this.container, this.name);
		}
		return this.context;
	}

	public String getName() {
		return name;
	}
	
	public boolean isEngine() {
		return this.entry.getType() == org.mule.jbi.componentRegistry.EntryDocument.Entry.Type.SERVICE_ENGINE;
	}

	public ObjectName getObjectName() {
		return objectName;
	}

	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}

	public Entry getEntry() {
		return entry;
	}

	public String getInstallRoot() {
		return installRoot;
	}

	public void setInstallRoot(String installRoot) {
		this.installRoot = installRoot;
	}

}
