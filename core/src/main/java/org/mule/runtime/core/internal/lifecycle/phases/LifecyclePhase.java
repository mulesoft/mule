/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.lifecycle.LifecycleObject;

import java.util.Set;

/**
 * Encapsulates the notion of a lifecycle phase i.e. 'stop'. Implementations of this class are responsible for invoking the
 * lifecycle phase on a set of objects and also for knowing which phases can preceed and go after it. This objects are
 * configurable so that lifecycles can be customised.
 *
 * Note that users wouldn't normally customise the lifecycle of the server.
 */

public interface LifecyclePhase extends NamedObject {

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
