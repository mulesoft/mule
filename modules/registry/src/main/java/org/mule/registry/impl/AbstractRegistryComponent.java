/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.registry.ClassLoaderFactory;
import org.mule.registry.ComponentType;
import org.mule.registry.Library;
import org.mule.registry.Registry;
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.Unit;

import javax.management.ObjectName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractRegistryComponent extends AbstractEntry implements RegistryComponent {

    protected ComponentType type;
    protected String name;
    protected transient ObjectName objectName;
    protected List units;
    protected List libraries;
    protected String workspaceRoot;
    protected List classPathElements;
    protected String componentClassName;
    protected boolean isClassLoaderParentFirst;
    protected boolean isTransient;
    protected Object component;
    protected RegistryDescriptor descriptor;

    protected AbstractRegistryComponent(String name, ComponentType type,  Registry registry) {
        super(registry);
        this.type = type;
        this.name = name;
        this.units = new ArrayList();
        this.libraries = new ArrayList();
    }

    public ComponentType getType() {
        return type;
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
     * @see org.mule.jbi.registry.Component#getObjectName()
     */
    public ObjectName getObjectName() {
        return this.objectName;
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.mule.AbstractEntry#checkDescriptor()
     */
    protected void checkDescriptor() throws RegistryException {
        super.checkDescriptor();
        // Check that it is a service assembly
        if (!getDescriptor().isComponent()) {
            throw new RegistryException("component should be set");
        }
    }

    protected void createComponent() throws RegistryException {
        try {
            ClassLoader loader = ClassLoaderFactory.getInstance().createComponentClassLoader(this);
            Class cl = Class.forName(this.componentClassName, true, loader);
            this.component = cl.newInstance();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Component#install()
     */
    public synchronized void install() throws RegistryException {
        if (!getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (isTransient) {
            return;
        }

        try {
            doInstall();
        } catch (Exception e) {
            throw new RegistryException(e);
        }

        // Create component
        createComponent();
        try {
            objectName = initComponent();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
    }

    protected abstract void doInstall() throws Exception;



    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Component#restoreState(org.mule.jbi.JbiContainer)
     */
    public final synchronized void restoreState() throws RegistryException {
        if (!getCurrentState().equals(UNKNOWN) && !getCurrentState().equals(INITIALIZED)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        try {
            if (!isTransient) {
                createComponent();
                initComponent();
            }
            if (getStateAtShutdown().equals(RUNNING)) {
                start();
            }
            doRestoreState();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
    }

    protected abstract void doRestoreState() throws Exception;


    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Component#saveAndShutdown()
     */
    public synchronized void saveAndShutdown() throws RegistryException  {
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
    public final synchronized void start() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (getCurrentState().equals(RUNNING)) {
            return;
        }
        try {
            doStart();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        setCurrentState(RUNNING);
    }

    protected abstract void doStart() throws Exception;

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Component#stop()
     */
    public final synchronized void stop() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN) || getCurrentState().equals(SHUTDOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (getCurrentState().equals(STOPPED)) {
            return;
        }
        try {
            doStop();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        setCurrentState(STOPPED);
    }

    protected abstract void doStop() throws Exception;

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Component#shutDown()
     */
    public final synchronized void shutDown() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (getCurrentState().equals(SHUTDOWN)) {
            return;
        }
        stop();
        // TODO: unregister mbean
        try {
            doShutDown();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        setCurrentState(SHUTDOWN);
    }

    protected abstract void doShutDown() throws Exception;

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Component#uninstall()
     */
    public synchronized void uninstall() throws RegistryException {
        if (!getCurrentState().equals(SHUTDOWN) && !getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (this.units.size() > 0) {
            throw new RegistryException("Component has service units deployed");
        }
        Library[] libraries = getLibraries();
        for (int i = 0; i < libraries.length; i++) {
            libraries[i].removeComponent(this);
        }
        // Remove directories
        registry.getManagementContext().deleteDir(getInstallRoot());
        registry.getManagementContext().deleteDir(getWorkspaceRoot());
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

    public void setComponent(Object component) {
        this.component = component;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public void setTransient(boolean isTransient) {
        this.isTransient = isTransient;
    }

    /**
     * Return the component implementation.
     *
     * @return
     */
    public Object getComponent() {
        return component;
    }

    /**
     * Return the descriptor for this component.
     *
     * @return
     */
    public RegistryDescriptor getDescriptor() throws RegistryException {
        return descriptor;
    }

    public void setDescriptor(RegistryDescriptor descriptor) throws RegistryException {
        this.descriptor = descriptor;
    }
}
