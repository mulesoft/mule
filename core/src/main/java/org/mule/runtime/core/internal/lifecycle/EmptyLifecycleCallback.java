/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;

/**
 * A lifecycle callback that does nothing. Can be used to transition a
 * {@link org.mule.runtime.core.api.lifecycle.LifecycleManager} to the next phase without executing logic.
 *
 * USers should never use this object themselves, it provides an internal Mule function.
 *
 * @since 3.0
 */
public class EmptyLifecycleCallback<O> implements LifecycleCallback<O> {

  public void onTransition(String phaseName, O object) throws MuleException {
    // do nothing
  }
}
