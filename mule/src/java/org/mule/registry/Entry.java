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
package org.mule.registry;
/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Entry {

    /**
     * Name of this entry
     * @return
     */
    String getName();

    void setName(String name);

    /**
     * Retrieves the installation directory.
     * @return the installation directory
     */
    String getInstallRoot();

    /**
     * Sets the installation directory.
     *
     * @param installRoot the installation directory
     */
    void setInstallRoot(String installRoot);

    /**
     * Get the current state of this managed compononent.
     * 
     * @return the current state of this managed component (must be one of the 
     *         string constants defined by this interface)
     */
    String getCurrentState();

    /**
     * Status before JBI was shutdown.
     * @return
     */
    String getStateAtShutdown();

    /**
     * Set the state before the server was shutdown.
     * @param state
     */
    void setStateAtShutdown(String state) throws RegistryException;

    void setCurrentState(String state) throws RegistryException;

    /** Value returned by {@link #getCurrentState()} for a shutdown component. */
    final static String SHUTDOWN = "Shutdown";

    /** Value returned by {@link #getCurrentState()} for a stopped component. */
    final static String STOPPED  = "Stopped";

    /** Value returned by {@link #getCurrentState()} for a running component. */
    final static String RUNNING  = "Running";
    
    /** Value returned by {@link #getCurrentState()} for a component in an
     * unknown state. */
    final static String UNKNOWN  = "Unknown";

    /** Value returned by {@link #getCurrentState()} for a component in an
     * initialised state. */
    final static String INITIALIZED  = "Initialized";


}
