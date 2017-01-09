/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

/**
 * A container-managed lifecycle phase is called by the container itself. This class acts as a marker to enable tracking of phases
 * even if Mule does not initiate the phase
 *
 * @deprecated as of 3.7.0 since it's no longer used
 */
@Deprecated
public class ContainerManagedLifecyclePhase extends DefaultLifecyclePhase {

  public ContainerManagedLifecyclePhase(String name, Class<?> lifecycleClass, String oppositePhaseName) {
    super(name, lifecycleClass, oppositePhaseName);
  }
}
