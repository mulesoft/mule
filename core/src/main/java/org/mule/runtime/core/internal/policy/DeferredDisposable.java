/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
