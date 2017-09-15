/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link LockProvider} implementation for applications running in a single mule server
 */
public class SingleServerLockProvider implements LockProvider {

  @Override
  public Lock createLock(String lockId) {
    return new ReentrantLock(true);
  }

}
