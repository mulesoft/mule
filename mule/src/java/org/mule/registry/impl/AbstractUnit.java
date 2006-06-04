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
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryException;
import org.mule.registry.Unit;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public abstract class AbstractUnit extends AbstractEntry implements Unit {

    private String assembly;

    protected AbstractUnit(Registry registry) {
        super(registry);
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#getAssemblies()
     */
    public Assembly getAssembly() {
        return getRegistry().getAssembly(this.assembly);
    }

    public void setAssembly(Assembly assembly) {
        this.assembly = assembly.getName();
    }

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#deploy()
     */
    public final synchronized String deploy() throws RegistryException {
        if (!getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        String result = null;
        try {
            result = doDeploy();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        // TODO: analyse result
        getRegistryComponent().addUnit(this);
        ((AbstractAssembly) getAssembly()).addUnit(this);
        setCurrentState(STOPPED);
        return result;
    }

    public abstract String doDeploy() throws Exception;

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#init()
     */
    public final synchronized void init() throws RegistryException {
        if (!getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        try {
            doInit();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        setCurrentState(STOPPED);
    }

    protected abstract void doInit() throws Exception;
    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#start()
     */
    public final synchronized void start() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (!getCurrentState().equals(RUNNING)) {
            try {
                doStart();
            } catch (Exception e) {
                throw new RegistryException(e);
            }
            setCurrentState(RUNNING);
        }
    }

    protected abstract void doStart() throws Exception;
    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#stop()
     */
    public final synchronized void stop() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN) || getCurrentState().equals(SHUTDOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (!getCurrentState().equals(STOPPED)) {
            try {
                doStop();
            } catch (Exception e) {
                throw new RegistryException(e);
            }
            setCurrentState(STOPPED);
        }
    }

    protected abstract void doStop() throws Exception;

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#shutDown()
     */
    public final synchronized void shutDown() throws RegistryException {
        if (getCurrentState().equals(UNKNOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        if (!getCurrentState().equals(SHUTDOWN)) {
            stop();
            try {
                doShutDown();
            } catch (Exception e) {
                throw new RegistryException(e);
            }
            setCurrentState(SHUTDOWN);
        }
    }

    protected abstract void doShutDown() throws Exception;

    /* (non-Javadoc)
     * @see org.mule.jbi.registry.Unit#undeploy()
     */
    public synchronized String undeploy() throws RegistryException {
        if (!getCurrentState().equals(SHUTDOWN)) {
            throw new RegistryException("Illegal status: " + getCurrentState());
        }
        String result = null;
        try {
            result = doUndeploy();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        // TODO: analyse result
        getRegistryComponent().removeUnit(this);
        ((AbstractAssembly) getAssembly()).removeUnit(this);
        setCurrentState(UNKNOWN);
        return result;
    }

    protected abstract String doUndeploy() throws Exception;

    public void setRegistryComponent(RegistryComponent component) {
        // nothing to do (yet?)
    }

}
