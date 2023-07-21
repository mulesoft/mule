/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.lifecycle.Disposable;

/**
 * Implementations provide a hook for disposing this instance.
 * <p>
 * This is useful when a way to dispose an object is needed but without having a strong reference to the object to be disposed.
 *
 * @since 4.3, 4.2.3
 */
public interface DeferredDisposable {

  /**
   * @return a callback to dispose this object
   */
  Disposable deferredDispose();
}
