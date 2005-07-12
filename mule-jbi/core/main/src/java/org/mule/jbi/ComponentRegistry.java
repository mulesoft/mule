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
package org.mule.jbi;

import javax.jbi.JBIException;
import javax.management.ObjectName;

import org.mule.jbi.framework.ComponentInfo;
import org.mule.jbi.framework.SharedLibraryInfo;

public interface ComponentRegistry extends LifeCycle {

	ComponentInfo getComponent(String name);
	
	ComponentInfo[] getComponents();
	
	SharedLibraryInfo getSharedLibrary(String name);
	
	ObjectName getComponentName(String name);
	
	ObjectName[] getEngineComponentNames();
	
	ObjectName[] getBindingComponentNames();
	
	ComponentInfo registerComponent(String name, boolean isEngine) throws JBIException;
	
	SharedLibraryInfo registerSharedLibrary(String name) throws JBIException;
	
	void unregisterSharedLibrary(String sharedLibName) throws JBIException;
	
}
