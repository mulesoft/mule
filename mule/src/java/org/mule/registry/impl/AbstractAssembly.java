/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Id$
 * $Revision$
 * $Date$
 */
package org.mule.registry.impl;

import org.mule.registry.Assembly;
import org.mule.registry.Registry;
import org.mule.registry.RegistryDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.Unit;
import org.mule.util.FileUtils;

import java.io.File;
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
public abstract class AbstractAssembly extends AbstractEntry implements Assembly {

    protected List units;
    protected boolean isTransient;
    protected RegistryDescriptor descriptor;

    protected AbstractAssembly(Registry registry) {
        super(registry);
        this.units = new ArrayList();
    }

    public Unit getUnit(String name) {
        for (Iterator it = this.units.iterator(); it.hasNext();) {
            AbstractUnit u = (AbstractUnit) it.next();
            if (u.getName().equals(name)) {
                return u;
            }
        }
        return null;
    }

    public void addUnit(Unit unit) {
        this.units.add(unit);
    }

    public void removeUnit(Unit unit) {
        this.units.remove(unit);
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#getUnits()
     */
    public Unit[] getUnits() {
        Collection c = this.units;
        return (Unit[]) c.toArray(new Unit[c.size()]);
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.mule.AbstractEntry#checkDescriptor()
     */
    protected void checkDescriptor() throws RegistryException {
        super.checkDescriptor();
        // Check that it is a service assembly
        if (!getDescriptor().isServiceAssembly()) {
            throw new RegistryException("service-assembly should be set");
        }
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#start()
     */
    public synchronized String start() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (!getCurrentState().equals(RUNNING)) {
            Unit[] units = getUnits();
            for (int i = 0; i < units.length; i++) {
                units[i].start();
            }
            setCurrentState(RUNNING);
        }
        // TODO
        return "";
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#stop()
     */
    public synchronized String stop() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN) || getCurrentState().equals(SHUTDOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (!getCurrentState().equals(STOPPED)) {
            Unit[] units = getUnits();
            for (int i = 0; i < units.length; i++) {
                units[i].stop();
            }
            setCurrentState(STOPPED);
        }
        // TODO
        return "";
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#shutDown()
     */
    public synchronized String shutDown() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (!getCurrentState().equals(SHUTDOWN)) {
            stop();
            Unit[] units = getUnits();
            for (int i = 0; i < units.length; i++) {
                units[i].shutDown();
            }
            setCurrentState(SHUTDOWN);
        }
        // TODO
        return "";
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#undeploy()
     */
    public synchronized String undeploy() throws RegistryException {
        if (!getCurrentState().equals(SHUTDOWN) && !getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        Unit[] units = getUnits();
        for (int i = 0; i < units.length; i++) {
            units[i].undeploy();
            // TODO: read output from undeploy() to analyse result
        }
        FileUtils.deleteTree(new File(getInstallRoot()));
        getRegistry().removeAssembly(this);
        setCurrentState(UNKNOWN);
        // TODO: return info
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#isTransient()
     */
    public boolean isTransient() {
        return isTransient;
    }

    public void setTransient(boolean isTransient) {
        this.isTransient = isTransient;
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#restoreState()
     */
    public void restoreState() throws RegistryException {
        Unit[] units = getUnits();
        for (int i = 0; i < units.length; i++) {
            units[i].init();
            if (units[i].getStateAtShutdown().equals(Unit.RUNNING)) {
                units[i].start();
            } else if (units[i].getStateAtShutdown().equals(Unit.SHUTDOWN)) {
                units[i].shutDown();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Assembly#saveAndShutdown()
     */
    public void saveAndShutdown() throws RegistryException {
        Unit[] units = getUnits();
        for (int i = 0; i < units.length; i++) {
            units[i].setStateAtShutdown(units[i].getCurrentState());
            units[i].shutDown();
        }
    }

    public void setDescriptor(RegistryDescriptor descriptor) {
        this.descriptor = descriptor;
    }

}
