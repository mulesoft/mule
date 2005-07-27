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

import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Library;
import org.mule.jbi.util.IOUtils;

import javax.jbi.JBIException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class LibraryImpl extends AbstractEntry implements Library {

	private List components;
	private List classPathElements;
	private boolean isClassLoaderParentFirst;
	
	public LibraryImpl() {
		this.components = new ArrayList();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Library#getReferringComponents()
	 */
	public Component[] getComponents() {
		Collection c = new ArrayList();
		for (Iterator it = this.components.iterator(); it.hasNext();) {
			String ref = (String) it.next();
			Component comp = getRegistry().getComponent(ref);
			c.add(comp);
		}
		return (Component[]) c.toArray(new Component[c.size()]);
	}
	
	public void addComponent(Component component) {
		this.components.add(component.getName());
	}
	
	public void removeComponent(Component component) {
		this.components.remove(component.getName());
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.mule.AbstractEntry#checkDescriptor()
	 */
	protected void checkDescriptor() throws JBIException {
		super.checkDescriptor();
		// Check that it is a service assembly
		if (!getDescriptor().isSetSharedLibrary()) {
			throw new JBIException("shared library should be set");
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#start()
	 */
	public synchronized void install() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		// Get class path elements
		this.classPathElements = Arrays.asList(getDescriptor().getSharedLibrary().getSharedLibraryClassPath().getPathElementArray());
		// Class loader delegation
		this.isClassLoaderParentFirst = com.sun.java.xml.ns.jbi.JbiDocument.Jbi.SharedLibrary.ClassLoaderDelegation.PARENT_FIRST.equals(getDescriptor().getSharedLibrary().getClassLoaderDelegation());
		// Set current state
		setCurrentState(SHUTDOWN);
	}

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#start()
	 */
	public synchronized void uninstall() throws JBIException, IOException {
		if (!getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		IOUtils.deleteFile(new File(getInstallRoot()));
		getRegistry().removeLibrary(this);
		setCurrentState(UNKNOWN);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Library#getClassPathElements()
	 */
	public List getClassPathElements() {
		return this.classPathElements;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Library#isClassLoaderParentFirst()
	 */
	public boolean isClassLoaderParentFirst() {
		return this.isClassLoaderParentFirst;
	}

}
