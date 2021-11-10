/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;

import java.util.List;

/**
 * 
 * @param <T>
 * 
 * @since 4.5
 */
public class OnlyChildSpringContextLifecycleCallback<T> extends RegistryLifecycleCallback<T> {

  public OnlyChildSpringContextLifecycleCallback(RegistryLifecycleManager registryLifecycleManager) {
    super(registryLifecycleManager);
  }

  @Override
  protected List<Object> getObjectsForPhase(LifecyclePhase phase) {
    return registryLifecycleManager.getObjectsForPhase(phase);
  }

}
