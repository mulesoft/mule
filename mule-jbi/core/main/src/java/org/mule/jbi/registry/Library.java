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

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Library extends Entry {

	Component[] getComponents();
	
	List getClassPathElements();
	
	boolean isClassLoaderParentFirst();
	
	void addComponent(Component component);
	
	void removeComponent(Component component);
	
	void install() throws JBIException, IOException;

	void uninstall() throws JBIException, IOException;
	
}
