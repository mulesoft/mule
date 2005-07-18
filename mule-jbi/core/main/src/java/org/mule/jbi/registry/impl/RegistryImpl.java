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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;

import org.mule.jbi.registry.Assembly;
import org.mule.jbi.registry.Binding;
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Engine;
import org.mule.jbi.registry.Library;
import org.mule.jbi.registry.Registry;
import org.mule.jbi.registry.RegistryIO;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class RegistryImpl implements Registry {
	
	private List engines;
	private List bindings;
	private List libraries;
	private List assemblies;
	private transient Map componentsMap;
	private transient Map librariesMap;
	private transient Map assembliesMap;
	private transient File store;
	public RegistryImpl() {
		this.engines = new ArrayList();
		this.bindings = new ArrayList();
		this.libraries = new ArrayList();
		this.assemblies = new ArrayList();
		this.componentsMap = new HashMap();
		this.librariesMap = new HashMap();
		this.assembliesMap = new HashMap();
	}
	
	public File getStore() {
		return this.store;
	}


	public void setStore(File store) {
		this.store = store;
	}
	

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getComponents()
	 */
	public synchronized Component[] getComponents() {
		Collection c = new ArrayList();
		c.addAll(this.engines);
		c.addAll(this.bindings);
		return (Component[]) c.toArray(new Component[c.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getComponent(java.lang.String)
	 */
	public synchronized Component getComponent(String name) {
		return (Component) this.componentsMap.get(name);
	}
	
	public synchronized void removeComponent(Component component) {
		this.componentsMap.remove(component.getName());
		this.engines.remove(component);
		this.bindings.remove(component);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getEngines()
	 */
	public synchronized Engine[] getEngines() {
		Collection c = this.engines;
		return (Engine[]) c.toArray(new Engine[c.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getEngine(java.lang.String)
	 */
	public synchronized Engine getEngine(String name) {
		return (Engine) this.componentsMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addEngine(java.lang.String)
	 */
	public synchronized Engine addEngine(String name) throws JBIException {
		if (getComponent(name) != null) {
			throw new JBIException("Component already registered: " + name);
		}
		EngineImpl e = new EngineImpl();
		e.setName(name);
		this.engines.add(e);
		this.componentsMap.put(name, e);
		return e;
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addTransientEngine(java.lang.String, javax.jbi.component.Component)
	 */
	public synchronized Engine addTransientEngine(String name, javax.jbi.component.Component engine) throws JBIException, IOException {
		Component c = getComponent(name);
		if (c == null) {
			EngineImpl e = new EngineImpl();
			e.setName(name);
			e.setTransientComponent(true);
			e.setComponent(engine);
			this.engines.add(e);
			this.componentsMap.put(name, e);
			return e;
		} else {
			if (!(c instanceof EngineImpl) || !c.isTransientComponent()) {
				throw new JBIException("A non-transient or non-engine component is already registered: " + name);
			}
			EngineImpl e = (EngineImpl) c;
			e.setComponent(engine);
			return e;
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getBindings()
	 */
	public synchronized Binding[] getBindings() {
		Collection c = this.bindings;
		return (Binding[]) c.toArray(new Binding[c.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getBinding(java.lang.String)
	 */
	public synchronized Binding getBinding(String name) {
		return (Binding) this.componentsMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addBinding(java.lang.String)
	 */
	public synchronized Binding addBinding(String name) throws JBIException {
		if (getComponent(name) != null) {
			throw new JBIException("Component already registered: " + name);
		}
		BindingImpl b = new BindingImpl();
		b.setName(name);
		this.bindings.add(b);
		this.componentsMap.put(name, b);
		return b;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addTransientBinding(java.lang.String, org.mule.jbi.registry.Component)
	 */
	public synchronized Binding addTransientBinding(String name, javax.jbi.component.Component binding) throws JBIException, IOException {
		Component c = getComponent(name);
		if (c == null) {
			BindingImpl b = new BindingImpl();
			b.setName(name);
			b.setTransientComponent(true);
			b.setComponent(binding);
			this.bindings.add(b);
			this.componentsMap.put(name, b);
			return b;
		} else {
			if (!(c instanceof BindingImpl) || !c.isTransientComponent()) {
				throw new JBIException("A non-transient or non-binding component is already registered: " + name);
			}
			BindingImpl b = (BindingImpl) c;
			b.setComponent(binding);
			return b;
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getLibraries()
	 */
	public synchronized Library[] getLibraries() {
		Collection c = this.libraries;
		return (Library[]) c.toArray(new Library[c.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getLibrary(java.lang.String)
	 */
	public synchronized Library getLibrary(String name) {
		return (Library) this.librariesMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addLibrary(java.lang.String)
	 */
	public synchronized Library addLibrary(String name) throws JBIException {
		if (getLibrary(name) != null) {
			throw new JBIException("Library already registered: " + name);
		}
		LibraryImpl l = new LibraryImpl();
		l.setName(name);
		this.libraries.add(l);
		this.librariesMap.put(name, l);
		return l;
	}
	
	public synchronized void removeLibrary(Library library) {
		this.librariesMap.remove(library.getName());
		this.libraries.remove(library);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getAssemblies()
	 */
	public synchronized Assembly[] getAssemblies() {
		Collection c = this.assemblies;
		return (Assembly[]) c.toArray(new Assembly[c.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getAssembly(java.lang.String)
	 */
	public synchronized Assembly getAssembly(String name) {
		return (Assembly) this.assembliesMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addAssembly(java.lang.String)
	 */
	public synchronized Assembly addAssembly(String name) {
		if (getAssembly(name) != null) {
			return null;
		}
		AssemblyImpl a = new AssemblyImpl();
		a.setName(name);
		this.assemblies.add(a);
		this.assembliesMap.put(name, a);
		return a;
	}

	public synchronized void removeAssembly(Assembly assembly) {
		this.assembliesMap.remove(assembly.getName());
		this.assemblies.remove(assembly);
	}

	public void initialize() {
		this.componentsMap = new HashMap();
		for (Iterator it = this.engines.iterator(); it.hasNext();) {
			EngineImpl e = (EngineImpl) it.next();
			this.componentsMap.put(e.getName(), e);
		}
		for (Iterator it = this.bindings.iterator(); it.hasNext();) {
			BindingImpl b = (BindingImpl) it.next();
			this.componentsMap.put(b.getName(), b);
		}
		this.librariesMap = new HashMap();
		for (Iterator it = this.libraries.iterator(); it.hasNext();) {
			LibraryImpl l = (LibraryImpl) it.next();
			this.librariesMap.put(l.getName(), l);
		}
		this.assembliesMap = new HashMap();
		for (Iterator it = this.assemblies.iterator(); it.hasNext();) {
			AssemblyImpl a = (AssemblyImpl) it.next();
			this.assembliesMap.put(a.getName(), a);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#start()
	 */
	public synchronized void start() throws JBIException {
		try {
			Component[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				components[i].restoreState();
			}
		} catch (Exception e) {
			throw new JBIException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#shutDown()
	 */
	public synchronized void shutDown() throws JBIException {
		try {
			Component[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				components[i].saveAndShutdown();
			}
			RegistryIO.save(this);
		} catch (IOException e) {
			throw new JBIException(e);
		}
	}
	
}
