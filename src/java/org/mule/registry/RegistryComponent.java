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
package org.mule.registry;

import javax.management.ObjectName;

import java.util.List;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface RegistryComponent extends Entry {

    ComponentType getType();


	void restoreState() throws RegistryException;

	void saveAndShutdown() throws RegistryException;
	
    /**
     * Start the item.
     * 
     * @exception RegistryException if the item fails to start.
     */
    void start() throws RegistryException;

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception RegistryException if the item fails to stop.
     */
    void stop() throws RegistryException;

    /**
     * Shut down the item. The releases resources, preparatory to 
     * uninstallation.
     *
     * @exception RegistryException if the item fails to shut down.
     */
    void shutDown() throws RegistryException;

	/**
	 * Retrieves the libraries that this component uses.
	 * @return
	 */
	Library[] getLibraries();
	
	/**
	 * Units deployed to this component
	 * @return
	 */
	Unit[] getUnits();
	
	/**
	 * 
	 */
	List getClassPathElements();
	
	void setClassPathElements(List elements);

	boolean isClassLoaderParentFirst();
	
	boolean isTransient();

    void setTransient(boolean isTransient);

	/**
	 * Return the component implementation.
	 * @return
	 */
	Object getComponent();

    void setComponent(Object component);
	/**
	 * Return the descriptor for this component.
	 * @return
	 */
	RegistryDescriptor getDescriptor() throws RegistryException;
	
	void setDescriptor(RegistryDescriptor descriptor) throws RegistryException;

	/**
	 * Return the ObjectName under which the lifecycle mbean is registered.
	 * @return
	 */
	ObjectName getObjectName();
	
	/**
	 * Return the private component workspace
	 * @return
	 */
	String getWorkspaceRoot();
	
	void setWorkspaceRoot(String workspaceRoot);

	/**
	 * Install this component.
	 * 
	 * @throws RegistryException
	 */
	void install() throws RegistryException;

	/**
	 * Uninstall this component.
	 * 
	 * @throws RegistryException
	 */
	void uninstall() throws RegistryException;

    Registry getRegistry();

    ObjectName initComponent() throws Exception;

    void addUnit(Unit unit);

    void removeUnit(Unit unit);
}
