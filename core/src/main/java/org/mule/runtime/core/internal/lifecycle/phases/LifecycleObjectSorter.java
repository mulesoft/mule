/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import org.mule.runtime.core.internal.registry.Registry;

import java.util.List;

/**
 * A non reusable object which determines the correct order in which a particular {@link LifecyclePhase} should be applied to a
 * provided list of objects.
 *
 * @since 4.2.0
 */
public interface LifecycleObjectSorter {

  /**
   * Adds the given {@code object} to the list to be sorted
   *
   * @param name   the name under which the object is registered in the {@link Registry}
   * @param object the object
   */
  void addObject(String name, Object object);

  /**
   * @return The sorted list of objects previously added through {@link #addObject(String, Object)}
   */
  List<Object> getSortedObjects();

  /**
   * Provides the order of objects as reference for initialise/dispose phases
   *
   * @param lookupObjects lifecycle object list which is ordered based on the type
   */
  default void setLifeCycleObjectNameOrder(List<String> lookupObjects) {};
}
