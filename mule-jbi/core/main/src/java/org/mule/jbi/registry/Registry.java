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

import java.io.File;
import java.io.IOException;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Registry {

	File getStore();
	
	Component[] getComponents();
	
	Component getComponent(String name);
	
	/**
	 * Remove a registered component from the list.
	 * Internal use only.
	 * 
	 * @param component the component to remove
	 */
	void removeComponent(Component component);
	
	/**
	 * Return all engines. 
	 * 
	 * @return
	 */
	Engine[] getEngines();
	
	/**
	 * Return the existing engine with this name.
	 * 
	 * @param name
	 * @return the engine or <code>null</code> if not found
	 */
	Engine getEngine(String name);
	
	/**
	 * Create a new engine with the given name.
	 * 
	 * @param name
	 * @return the newly engine or <code>null</code> if already exists
	 */
	Engine addEngine(String name) throws JBIException;
	
	Engine addTransientEngine(String name, javax.jbi.component.Component engine) throws JBIException, IOException;
	
	Engine addTransientEngine(String name, javax.jbi.component.Component engine, Bootstrap bootsrap) throws JBIException, IOException;
	
	Binding[] getBindings();
	
	Binding getBinding(String name);
	
	Binding addBinding(String name) throws JBIException;
	
	Binding addTransientBinding(String name, javax.jbi.component.Component binding) throws JBIException, IOException;
	
	Binding addTransientBinding(String name, javax.jbi.component.Component binding, Bootstrap bootstrap) throws JBIException, IOException;
	
	Library[] getLibraries();
	
	Library getLibrary(String name);
	
	Library addLibrary(String name) throws JBIException;
	
	void removeLibrary(Library library);
	
	Assembly[] getAssemblies();
	
	Assembly getAssembly(String name);
	
	Assembly addAssembly(String name);
	
	void removeAssembly(Assembly assembly);
	
	void addTransientUnit(String suName, Component component, String installRoot) throws JBIException, IOException;
	
	void start() throws JBIException;
	
	void shutDown() throws JBIException;
	
}
