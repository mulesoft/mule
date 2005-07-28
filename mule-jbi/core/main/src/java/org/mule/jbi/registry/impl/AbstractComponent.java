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

import org.mule.jbi.JbiContainer;
import org.mule.jbi.framework.ClassLoaderFactory;
import org.mule.jbi.management.ComponentLifeCycle;
import org.mule.jbi.management.Directories;
import org.mule.jbi.messaging.DeliveryChannelImpl;
import org.mule.jbi.registry.Component;
import org.mule.jbi.registry.Library;
import org.mule.jbi.registry.Unit;
import org.mule.jbi.util.IOUtils;

import javax.jbi.JBIException;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.jbi.messaging.DeliveryChannel;
import javax.management.ObjectName;
import javax.management.StandardMBean;
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
public class AbstractComponent extends AbstractEntry implements Component {

	private transient javax.jbi.component.Component component;
	private transient ObjectName objectName;
	private transient DeliveryChannel channel;
	private List units;
	private List libraries;
	private String workspaceRoot;
	private List classPathElements;
	private String componentClassName;
	private boolean isClassLoaderParentFirst;
	private boolean isTransient;
	
	public AbstractComponent() {
		this.units = new ArrayList();
		this.libraries = new ArrayList();
	}
	
	protected void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readObject(in);
		in.defaultReadObject();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#getLibraries()
	 */
	public Library[] getLibraries() {
		Collection c = new ArrayList();
		for (Iterator it = this.libraries.iterator(); it.hasNext();) {
			String ref = (String) it.next();
			Library library = getRegistry().getLibrary(ref);
			c.add(library);
		}
		return (Library[]) c.toArray(new Library[c.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#getUnits()
	 */
	public Unit[] getUnits() {
		Collection c = new ArrayList();
		for (Iterator it = this.units.iterator(); it.hasNext();) {
			String ref = (String) it.next();
			String[] refs = ref.split("/");
			if (refs.length != 2) {
				throw new IllegalStateException("Malformed unit ref");
			}
			Unit unit = getRegistry().getAssembly(refs[0]).getUnit(refs[1]);
			c.add(unit);
		}
		return (Unit[]) c.toArray(new Unit[c.size()]);
	}
	
	public void addUnit(Unit unit) {
		this.units.add(unit.getAssembly().getName() + "/" + unit.getName());
	}
	
	public void removeUnit(Unit unit) {
		this.units.remove(unit.getAssembly().getName() + "/" + unit.getName());
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#getComponent()
	 */
	public javax.jbi.component.Component getComponent() {
		return this.component;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#getObjectName()
	 */
	public ObjectName getObjectName() {
		return this.objectName;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.mule.AbstractEntry#checkDescriptor()
	 */
	protected void checkDescriptor() throws JBIException {
		super.checkDescriptor();
		// Check that it is a service assembly
		if (!getDescriptor().isSetComponent()) {
			throw new JBIException("component should be set");
		}
	}
	
	public synchronized ObjectName initComponent() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN) && !getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.component.getClass().getClassLoader());
			JbiContainer container = getContainer();
			this.objectName = container.createMBeanName(getName(), "lifecycle", null);
			this.channel = new DeliveryChannelImpl(container, getName());
			ComponentLifeCycle lf = new ComponentLifeCycle(container, this);
			lf.init();
			if (container.getMBeanServer().isRegistered(this.objectName)) {
				container.getMBeanServer().unregisterMBean(this.objectName);
			}
			container.getMBeanServer().registerMBean(new StandardMBean(lf, ComponentLifeCycleMBean.class), objectName);
			setCurrentState(INITIALIZED);
			return objectName;
		} catch (Exception e) {
			throw new JBIException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}
	
	protected void createComponent() throws JBIException {
		try {
			ClassLoader loader = ClassLoaderFactory.getInstance().createComponentClassLoader(this);
			Class cl = Class.forName(this.componentClassName, true, loader);
			this.component = (javax.jbi.component.Component) cl.newInstance();
		} catch (Exception e) {
			throw new JBIException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#install()
	 */
	public synchronized void install() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (isTransient) {
			return;
		}
		// Check shared libraries
		com.sun.java.xml.ns.jbi.ComponentDocument.Component.SharedLibrary[] libs = getDescriptor().getComponent().getSharedLibraryArray();
		for (int i = 0; i < libs.length; i++) {
			String libName = libs[i].getDomNode().getFirstChild().getNodeValue();
			Library library = getRegistry().getLibrary(libName);
			if (library == null) {
				throw new JBIException("Component requires a missing shared library: " + libName);
			}
			library.addComponent(this);
		}
		// Get class path elements
		this.classPathElements = Arrays.asList(getDescriptor().getComponent().getComponentClassPath().getPathElementArray());
		// Class loader delegation
		this.isClassLoaderParentFirst = !com.sun.java.xml.ns.jbi.ComponentDocument.Component.ComponentClassLoaderDelegation.SELF_FIRST.equals(getDescriptor().getComponent().getComponentClassLoaderDelegation());
		// Get component class name
		this.componentClassName = getDescriptor().getComponent().getComponentClassName().getDomNode().getFirstChild().getNodeValue();
		// Create component
		createComponent();
		initComponent();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#restoreState(org.mule.jbi.JbiContainer)
	 */
	public synchronized void restoreState() throws JBIException, IOException {
		if (!getCurrentState().equals(UNKNOWN) && !getCurrentState().equals(INITIALIZED)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (!isTransient) {
			createComponent();
			initComponent();
		}
		if (getStateAtShutdown().equals(RUNNING)) {
			start();
		}
		Unit[] units = getUnits();
		for (int i = 0; i < units.length; i++) {
			getComponent().getServiceUnitManager().deploy(units[i].getName(), units[i].getInstallRoot());
			if (units[i].getStateAtShutdown().equals(RUNNING)) {
				units[i].start();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#saveAndShutdown()
	 */
	public synchronized void saveAndShutdown() throws JBIException, IOException {
		setStateAtShutdown(getCurrentState());
		Unit[] units = getUnits();
		for (int i = 0; i < units.length; i++) {
			units[i].setStateAtShutdown(units[i].getCurrentState());
		}
		shutDown();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#start()
	 */
	public synchronized void start() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (getCurrentState().equals(RUNNING)) {
			return;
		}
		this.component.getLifeCycle().start();
		setCurrentState(RUNNING);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#stop()
	 */
	public synchronized void stop() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN) || getCurrentState().equals(SHUTDOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (getCurrentState().equals(STOPPED)) {
			return;
		}
		this.component.getLifeCycle().stop();
		setCurrentState(STOPPED);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#shutDown()
	 */
	public synchronized void shutDown() throws JBIException, IOException {
		if (getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (getCurrentState().equals(SHUTDOWN)) {
			return;
		}
		stop();
		// TODO: unregister mbean
		this.component.getLifeCycle().shutDown();
		setCurrentState(SHUTDOWN);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#uninstall()
	 */
	public synchronized void uninstall() throws JBIException, IOException {
		if (!getCurrentState().equals(SHUTDOWN) && !getCurrentState().equals(UNKNOWN)) {
			throw new JBIException("Illegal status: " + getCurrentState());
		}
		if (this.units.size() > 0) {
			throw new JBIException("Component has service units deployed");
		}
		Library[] libraries = getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			libraries[i].removeComponent(this);
		}
		// Remove directories
		Directories.deleteDir(getInstallRoot());
		Directories.deleteDir(getWorkspaceRoot());
		// Remove component from registry
		getRegistry().removeComponent(this);
		setCurrentState(UNKNOWN);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#getWorkspaceRoot()
	 */
	public String getWorkspaceRoot() {
		return this.workspaceRoot;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#setWorkspaceRoot(java.lang.String)
	 */
	public void setWorkspaceRoot(String workspaceRoot) {
		this.workspaceRoot = workspaceRoot;
	}

	public DeliveryChannel getChannel() {
		return this.channel;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#getClassPathElements()
	 */
	public List getClassPathElements() {
		return this.classPathElements;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#setClassPathElements(java.util.List)
	 */
	public void setClassPathElements(List classPathElements) {
		this.classPathElements = classPathElements;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Component#isClassLoaderParentFirst()
	 */
	public boolean isClassLoaderParentFirst() {
		return this.isClassLoaderParentFirst;
	}

	public void setComponent(javax.jbi.component.Component component) {
		this.component = component;
	}

	public boolean isTransient() {
		return isTransient;
	}

	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

}
