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

import org.mule.jbi.componentRegistry.EntryDocument.Entry;

public class SharedLibraryInfo {

	private Entry entry;
	private Set components;
	
	public SharedLibraryInfo(Entry entry) {
		this.entry = entry; 
		this.components = new HashSet();
	}

	public Entry getEntry() {
		return this.entry;
	}

	public void addComponent(String name) {
		this.components.add(name);
	}
	
	public void removeComponent(String name) {
		this.components.remove(name);
	}
	
	public boolean hasComponents() {
		return this.components.size() > 0;
	}
	
}
