/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.lifecycle;

import org.mule.impl.lifecycle.LifecycleObject;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;

import java.util.Set;

/**
 * Encapsulates the notion of a lifecycle phase i.e. 'stop'.  Implementations of this class are responsible
 * for invoking the lifecycle phase on a set of objects and also for knowing which phases can preceed and go
 * after it.  This objects are configurable so that lifecycles can be customised.
 *
 * Note that users wouldn't normally customise the lifecycle of the server.
 */

public interface UMOLifecyclePhase
{
    String ALL_PHASES = "all";
    String NOT_IN_LIFECYCLE_PHASE = "not in lifecycle";

    void fireLifecycle(UMOManagementContext managementContext, String currentPhase) throws UMOException;

    void addOrderedLifecycleObject(LifecycleObject lco);

    void removeOrderedLifecycleObject(LifecycleObject lco);

    Set getOrderedLifecycleObjects();

    void setOrderedLifecycleObjects(Set orderedLifecycleObjects);

    Class[] getIgnorredObjectTypes();

    void setIgnorredObjectTypes(Class[] ignorredObjectTypes);

    Class getLifecycleClass();

    void setLifecycleClass(Class lifecycleClass);

    String getName();

    Set getSupportedPhases();

    void setSupportedPhases(Set supportedPhases);

    void registerSupportedPhase(String phase);

    void applyLifecycle(Object o) throws LifecycleException;

    public int getRegistryScope();

    public void setRegistryScope(int registryScope);
}
