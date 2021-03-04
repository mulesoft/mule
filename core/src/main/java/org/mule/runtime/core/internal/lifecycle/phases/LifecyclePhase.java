/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.meta.NamedObject;

/**
 * Encapsulates the notion of a lifecycle phase i.e. 'stop'. Implementations of this class are responsible for invoking the
 * lifecycle phase on a set of objects and also for knowing which phases can proceed and go after it. This objects are
 * configurable so that lifecycles can be customised.
 * <p>
 * Note that users wouldn't normally customise the lifecycle of the server.
 */

public interface LifecyclePhase extends NamedObject {

  String ALL_PHASES = "all";

  void registerSupportedPhase(String phase);

  /**
   * Creates an returns a new {@link LifecycleObjectSorter} that is to be used to sort objects to which {@code this} phase is to
   * be applied
   *
   * @return a new and non-reusable {@link LifecycleObjectSorter}
   * @since 4.2.0
   */
  LifecycleObjectSorter newLifecycleObjectSorter();

  void applyLifecycle(Object o) throws LifecycleException;
}
