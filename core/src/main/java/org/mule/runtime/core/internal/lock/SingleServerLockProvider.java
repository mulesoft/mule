/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
