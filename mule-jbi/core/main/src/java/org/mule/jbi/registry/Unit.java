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

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Unit extends Entry {
	
	void init() throws JBIException, IOException;

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
    
	Component getComponent();
	
	Assembly getAssembly();
	
	String deploy() throws JBIException, IOException;

	String undeploy() throws JBIException, IOException;
	
}
