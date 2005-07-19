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

import java.io.IOException;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.messaging.DeliveryChannel;
import javax.management.ObjectName;

import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Component extends Entry {

	void restoreState() throws JBIException, IOException;

	void saveAndShutdown() throws JBIException, IOException;
	
    /**
     * Start the item.
     * 
     * @exception javax.jbi.JBIException if the item fails to start.
     * @throws IOException 
     */
    void start() throws JBIException, IOException;

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception javax.jbi.JBIException if the item fails to stop.
     * @throws IOException 
     */
    void stop() throws JBIException, IOException;

    /**
     * Shut down the item. The releases resources, preparatory to 
     * uninstallation.
     *
     * @exception javax.jbi.JBIException if the item fails to shut down.
     * @throws IOException 
     */
    void shutDown() throws JBIException, IOException;

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
	
	/**
	 * Return the jbi component implementation.
	 * @return
	 */
	javax.jbi.component.Component getComponent();
	
	/**
	 * Return the descriptor for this component.
	 * @return
	 */
	Jbi getDescriptor() throws JBIException;
	
	void setDescriptor(Jbi descriptor) throws JBIException;

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
	 * Return the delivery channel for this component.
	 * @return
	 */
	DeliveryChannel getChannel();
	
	/**
	 * Install this component.
	 * 
	 * @throws JBIException
	 * @throws IOException
	 */
	void install() throws JBIException, IOException;

	/**
	 * Uninstall this component.
	 * 
	 * @throws JBIException
	 * @throws IOException
	 */
	void uninstall() throws JBIException, IOException;

}
