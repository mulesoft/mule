/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.MuleContext;

import java.util.Set;

/**
 * The LifecycleManager is responsible for managing the different lifecycle phases of the server and managing the
 * transitions between lifecycle phases.
 */

public interface LifecycleManager
{
    Set getLifecycles();

    void setLifecycles(Set lifecycles);

    void registerLifecycle(LifecyclePhase lci);

    void firePhase(MuleContext muleContext, String phase) throws MuleException;

    String getCurrentPhase();

    String getExecutingPhase();

    void reset();

    boolean isPhaseComplete(String phaseName);

    void applyLifecycle(MuleContext muleContext, Object object) throws MuleException;

    void checkPhase(String name) throws IllegalStateException;
}
