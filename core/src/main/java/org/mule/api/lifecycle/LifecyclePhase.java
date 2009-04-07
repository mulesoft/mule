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
import org.mule.api.registry.Registry;
import org.mule.lifecycle.LifecycleObject;

import java.util.Set;

/**
 * Encapsulates the notion of a lifecycle phase i.e. 'stop'.  Implementations of this class are responsible
 * for invoking the lifecycle phase on a set of objects and also for knowing which phases can preceed and go
 * after it.  This objects are configurable so that lifecycles can be customised.
 *
 * Note that users wouldn't normally customise the lifecycle of the server.
 */

public interface LifecyclePhase
{
    
    String ALL_PHASES = "all";

    void applyLifecycle(Registry registry) throws MuleException;

    void addOrderedLifecycleObject(LifecycleObject lco);

    void removeOrderedLifecycleObject(LifecycleObject lco);

    Set getOrderedLifecycleObjects();

    void setOrderedLifecycleObjects(Set orderedLifecycleObjects);

    Class[] getIgnoredObjectTypes();

    void setIgnoredObjectTypes(Class[] ignorredObjectTypes);

    Class getLifecycleClass();

    void setLifecycleClass(Class lifecycleClass);

    String getName();

    Set getSupportedPhases();

    void setSupportedPhases(Set supportedPhases);

    void registerSupportedPhase(String phase);

    boolean isPhaseSupported(String phase);

    void applyLifecycle(Object o) throws LifecycleException;

    String getOppositeLifecyclePhase();

}
