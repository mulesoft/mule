/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import org.mule.runtime.api.lock.LockFactory;

import java.util.concurrent.locks.Lock;

/**
 * Provides abstraction in the creation and destruction of Mule locks.
 *
 * {@link LockFactory} uses instances of this interface to create and destroy locks.
 *
 * Lock implementation can be changed by replacing the LockProvider in the mule registry.
 */
public interface LockProvider {

  /**
   * Returns an instance of a {@link Lock}.
   * 
   * @param lockId id that identifies the {@link Lock} instance
   * @return a {@link Lock} instance related to the lockId
   */
  Lock createLock(String lockId);

}
