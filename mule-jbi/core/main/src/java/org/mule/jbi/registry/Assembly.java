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

import javax.jbi.JBIException;

import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Assembly extends Entry {

	void restoreState() throws JBIException, IOException;

	void saveAndShutdown() throws JBIException, IOException;
	
	boolean isTransient();
	
    /**
     * Start the item.
     * 
     * @exception javax.jbi.JBIException if the item fails to start.
     * @throws IOException 
     */
    String start() throws JBIException, IOException;

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception javax.jbi.JBIException if the item fails to stop.
     * @throws IOException 
     */
    String stop() throws JBIException, IOException;

    /**
     * Shut down the item. The releases resources, preparatory to 
     * uninstallation.
     *
     * @exception javax.jbi.JBIException if the item fails to shut down.
     * @throws IOException 
     */
    String shutDown() throws JBIException, IOException;

    /**
     * Return the Unit of the given name.
     * @param name the name of the unit
     * @return the Unit or <code>null</code> if not found
     */
	Unit getUnit(String name);
	
	/**
	 * Get all units of this Assembly
	 * @return the units of this Assembly
	 */
	Unit[] getUnits();
	
	/**
	 * Return the descriptor for this component.
	 * @return
	 */
	Jbi getDescriptor() throws JBIException;
	
	void setDescriptor(Jbi descriptor) throws JBIException;

	String deploy() throws JBIException, IOException;
	
	String undeploy() throws JBIException, IOException;
	
}
