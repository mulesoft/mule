// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * LifeCycleMBean.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.management;

import java.io.IOException;
import javax.jbi.JBIException;

/**
 * LifeCycleMBean is a base interface that defines standard life cycle controls 
 * for JBI implementation services (which are implementation-specific), and JBI
 * components (bindings and engines).
 *
 * @author JSR 208 Expert Group
 */
public interface LifeCycleMBean
{
    /**
     * Start the item.
     * 
     * @exception javax.jbi.JBIException if the item fails to start.
     * @throws IOException 
     */
    void start() throws JBIException, IOException;

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception javax.jbi.JBIException if the item fails to stop.
     * @throws IOException 
     */
    void stop() throws JBIException, IOException;

    /**
     * Shut down the item. The releases resources, preparatory to 
     * uninstallation.
     *
     * @exception javax.jbi.JBIException if the item fails to shut down.
     * @throws IOException 
     */
    void shutDown() throws JBIException, IOException;

    /**
     * Get the current state of this managed compononent.
     * 
     * @return the current state of this managed component (must be one of the 
     *         string constants defined by this interface)
     * @throws IOException 
     */
    String getCurrentState() throws IOException;

    /** Value returned by {@link #getCurrentState()} for a shutdown component. */
    final static String SHUTDOWN = "Shutdown";

    /** Value returned by {@link #getCurrentState()} for a stopped component. */
    final static String STOPPED  = "Stopped";

    /** Value returned by {@link #getCurrentState()} for a running component. */

    final static String RUNNING  = "Running";
    /** Value returned by {@link #getCurrentState()} for a component in an
     * unknown state. */
    final static String UNKNOWN  = "Unknown";
}
