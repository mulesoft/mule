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

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlOptions;
import org.mule.jbi.ComponentRegistry;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.componentRegistry.RegistryDocument;
import org.mule.jbi.componentRegistry.EntryDocument.Entry;

public class ComponentRegistryImpl extends AbstractJbiService implements ComponentRegistry {

	public static final String REGISTRY_FILE = "registry.xml";
	
	private static final Log LOGGER = LogFactory.getLog(ComponentRegistryImpl.class);
	
	private File regFile;
	private RegistryDocument registry;
	private Map components;
	private Map libraries;
	
	public ComponentRegistryImpl(JbiContainer container) {
		super(container);
		this.components = new HashMap();
		this.libraries = new HashMap();
	}
	
	public void start() throws JBIException {
		this.regFile = new File(this.container.getWorkingDir(), REGISTRY_FILE);
		if (this.regFile.isFile()) {
			try {
				this.registry = RegistryDocument.Factory.parse(this.regFile);
				if (!"1.0".equals(this.registry.getRegistry().getVersion().toString())) {
					throw new JBIException("Invalid version of registry");
				}
				loadComponents();
			} catch (JBIException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.warn("Could not parse registry, will create a new one", e);
			}
		}
		if (this.registry == null) {
			XmlOptions options = new XmlOptions();
			options.setUseDefaultNamespace();
			this.registry = RegistryDocument.Factory.newInstance(options);
			this.registry.addNewRegistry().setVersion(new BigDecimal("1.0"));
			saveRegistry();
		}
	}
	
	public void stop() throws JBIException {
	}
	
	public ComponentInfo[] getComponents() {
		Collection col = this.components.values();
		return (ComponentInfo[]) col.toArray(new ComponentInfo[col.size()]);
	}
	
	public ComponentInfo registerTransientEngineComponent(String name, Component component) throws JBIException {
		Entry entry = Entry.Factory.newInstance();
		entry.addNewName().setStringValue(name);
		entry.setType(org.mule.jbi.componentRegistry.EntryDocument.Entry.Type.SERVICE_ENGINE);
		entry.setState(org.mule.jbi.componentRegistry.EntryDocument.Entry.State.UNKNOWN);
		ComponentInfo info = new ComponentInfo(this.container, entry);
		info.setComponent(component);
		component.getLifeCycle().init(info.getContext());
		component.getLifeCycle().start();
		this.components.put(name, info);
		return info;
	}
	
	public ComponentInfo getComponent(String name) {
		return (ComponentInfo) this.components.get(name);
	}
	
	public ObjectName getComponentName(String name) {
		ComponentInfo info = (ComponentInfo) this.components.get(name);
		if (info != null) {
			return info.getObjectName();
		}
		return null;
	}
	
	public ObjectName[] getEngineComponentNames() {
		List engines = new ArrayList();
		for (Iterator it = this.components.values().iterator(); it.hasNext();) {
			ComponentInfo info = (ComponentInfo) it.next();
			if (info.isEngine()) {
				engines.add(info.getObjectName());
			}
		}
		return (ObjectName[]) engines.toArray(new ObjectName[engines.size()]);
	}
	
	public ObjectName[] getBindingComponentNames() {
		List engines = new ArrayList();
		for (Iterator it = this.components.values().iterator(); it.hasNext();) {
			ComponentInfo info = (ComponentInfo) it.next();
			if (!info.isEngine()) {
				engines.add(info.getObjectName());
			}
		}
		return (ObjectName[]) engines.toArray(new ObjectName[engines.size()]);
	}
	
	public ComponentInfo registerComponent(String name, boolean isEngine) throws JBIException {
		Entry entry = this.registry.getRegistry().addNewEntry();
		entry.addNewName().setStringValue(name);
		if (isEngine) {
			entry.setType(org.mule.jbi.componentRegistry.EntryDocument.Entry.Type.SERVICE_ENGINE);
		} else {
			entry.setType(org.mule.jbi.componentRegistry.EntryDocument.Entry.Type.BINDING_COMPONENT);
		}
		entry.setState(org.mule.jbi.componentRegistry.EntryDocument.Entry.State.UNKNOWN);
		ComponentInfo info = new ComponentInfo(this.container, entry);
		this.components.put(name, info);
		saveRegistry();
		return info;
	}
	
	public SharedLibraryInfo registerSharedLibrary(String name) throws JBIException {
		Entry entry = this.registry.getRegistry().addNewEntry();
		entry.addNewName().setStringValue(name);
		entry.setType(org.mule.jbi.componentRegistry.EntryDocument.Entry.Type.SHARED_LIBRARY);
		entry.setState(org.mule.jbi.componentRegistry.EntryDocument.Entry.State.UNKNOWN);
		SharedLibraryInfo info = new SharedLibraryInfo(entry);
		this.libraries.put(name, info);
		saveRegistry();
		return info;
	}
	
	public void unregisterSharedLibrary(String sharedLibName) throws JBIException {
		SharedLibraryInfo lib = getSharedLibrary(sharedLibName);
		if (lib == null) {
			throw new JBIException("Could not uninstall shared library '" + sharedLibName + "': not installed");
		}
		if (lib.hasComponents()) {
			throw new JBIException("Could not uninstall shared library '" + sharedLibName + "': has components");
		}
		this.libraries.remove(sharedLibName);
		this.registry.getRegistry().getDomNode().removeChild(lib.getEntry().getDomNode());
		saveRegistry();
	}

	public SharedLibraryInfo getSharedLibrary(String name) {
		return (SharedLibraryInfo) this.libraries.get(name);
	}
	
	void saveRegistry() throws JBIException {
		try {
			this.registry.save(this.regFile);
		} catch (Exception e) {
			throw new JBIException("Could not save component registry", e);
		}
	}
	
	protected void loadComponents() throws JBIException {
		Entry[] entries = this.registry.getRegistry().getEntryArray();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getType() == org.mule.jbi.componentRegistry.EntryDocument.Entry.Type.SHARED_LIBRARY) {
				SharedLibraryInfo info = new SharedLibraryInfo(entries[i]);
				this.libraries.put(entries[i].getName().getStringValue(), info);
			} else {
				ComponentInfo info = new ComponentInfo(this.container, entries[i]);
				this.components.put(entries[i].getName().getStringValue(), info);
			}
		}
	}

}
