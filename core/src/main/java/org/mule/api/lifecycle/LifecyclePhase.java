/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.api.NamedObject;
import org.mule.lifecycle.LifecycleObject;

import java.util.Set;

/**
 * Encapsulates the notion of a lifecycle phase i.e. 'stop'.  Implementations of this class are responsible
 * for invoking the lifecycle phase on a set of objects and also for knowing which phases can preceed and go
 * after it.  This objects are configurable so that lifecycles can be customised.
 *
 * Note that users wouldn't normally customise the lifecycle of the server.
 */

public interface LifecyclePhase extends NamedObject
{
    
    String ALL_PHASES = "all";

    void addOrderedLifecycleObject(LifecycleObject lco);

    void removeOrderedLifecycleObject(LifecycleObject lco);

    Set<LifecycleObject> getOrderedLifecycleObjects();

    void setOrderedLifecycleObjects(Set<LifecycleObject> orderedLifecycleObjects);

    Class<?>[] getIgnoredObjectTypes();

    void setIgnoredObjectTypes(Class<?>[] ignorredObjectTypes);

    Class<?> getLifecycleClass();

    void setLifecycleClass(Class<?> lifecycleClass);

    Set<String> getSupportedPhases();

    void setSupportedPhases(Set<String> supportedPhases);

    void registerSupportedPhase(String phase);

    boolean isPhaseSupported(String phase);

    void applyLifecycle(Object o) throws LifecycleException;

    String getOppositeLifecyclePhase();

}
