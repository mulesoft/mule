/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.lifecycle;

import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;

import java.util.Set;

/**
 * The LifecycleManager is responsible for managing the different lifecycle phases of the server and managing the
 * transitions between lifecycle phases.
 */

public interface UMOLifecycleManager
{
    Set getLifecycles();

    void setLifecycles(Set lifecycles);

    void registerLifecycle(UMOLifecyclePhase lci);

    void firePhase(UMOManagementContext managementContext, String phase) throws UMOException;

    String getCurrentPhase();

    String getExecutingPhase();

    void reset();

    boolean isPhaseComplete(String phaseName);

    void applyLifecycle(UMOManagementContext managementContext, Object object) throws UMOException;

    void checkPhase(String name) throws IllegalStateException;
}
