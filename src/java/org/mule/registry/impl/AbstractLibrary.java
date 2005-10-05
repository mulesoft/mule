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
package org.mule.registry.impl;

import org.mule.registry.Library;
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.Registry;
import org.mule.util.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public abstract class AbstractLibrary extends AbstractEntry implements Library {

	protected List components;
	protected List classPathElements;
	protected boolean isClassLoaderParentFirst;
    protected RegistryDescriptor descriptor;

	protected AbstractLibrary(Registry registry) {
        super(registry);
		this.components = new ArrayList();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Library#getReferringComponents()
	 */
	public RegistryComponent[] getComponents() {
		Collection c = new ArrayList();
		for (Iterator it = this.components.iterator(); it.hasNext();) {
			String ref = (String) it.next();
			RegistryComponent comp = getRegistry().getComponent(ref);
			c.add(comp);
		}
		return (RegistryComponent[]) c.toArray(new RegistryComponent[c.size()]);
	}
	
	public void addComponent(RegistryComponent component) {
		this.components.add(component.getName());
	}
	
	public void removeComponent(RegistryComponent component) {
		this.components.remove(component.getName());
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.mule.AbstractEntry#checkDescriptor()
	 */
	protected void checkDescriptor() throws RegistryException {
		super.checkDescriptor();
		// Check that it is a service assembly
		if (!getDescriptor().isSharedLibrary()) {
			throw new RegistryException("shared library should be set");
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#start()
	 */
	public synchronized void install() throws RegistryException {
		if (!getCurrentState().equals(UNKNOWN)) {
			throw new RegistryException("Illegal status: " + getCurrentState());
		}
        try {
            doInstall();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        // Set current state
		setCurrentState(SHUTDOWN);
	}

	/* (non-Javadoc)
	 * @see javax.jbi.management.LifeCycleMBean#start()
	 */
	public synchronized void uninstall() throws RegistryException {
		if (!getCurrentState().equals(SHUTDOWN)) {
			throw new RegistryException("Illegal status: " + getCurrentState());
		}
        try {
            doUninstall();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        Utility.deleteTree(new File(getInstallRoot()));
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

    public void setDescriptor(RegistryDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    protected abstract void doInstall() throws Exception;

    protected abstract void doUninstall() throws Exception;
}
