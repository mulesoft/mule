/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.lock;

import org.mule.runtime.core.util.lock.LockFactory;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

/**
 * A {@link PathLock} which is based on {@link Lock locks} obtained through a {@link #lockFactory}. The lock's keys are generated
 * through the external form of a {@link URL}
 *
 * @since 4.0
 */
public class URLPathLock implements PathLock {

  private final URL url;
  private final LockFactory lockFactory;
  private final AtomicReference<Lock> ownedLock = new AtomicReference<>();

  /**
   * Creates a new instance
   *
   * @param url the URL from which the lock's key is to be extracted
   * @param lockFactory a {@link LockFactory}
   */
  public URLPathLock(URL url, LockFactory lockFactory) {
    this.url = url;
    this.lockFactory = lockFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean tryLock() {
    Lock lock = getLock();
    if (lock.tryLock()) {
      ownedLock.set(lock);
      return true;
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLocked() {
    if (ownedLock.get() != null) {
      return true;
    }

    Lock lock = getLock();
    try {
      return !lock.tryLock();
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    Lock lock = ownedLock.getAndSet(null);
    if (lock != null) {
      lock.unlock();
    }
  }

  private Lock getLock() {
    return lockFactory.createLock(url.toExternalForm());
  }
}
