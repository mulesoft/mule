/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.config.MuleConfiguration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * An implementation of {@link LockFactory} that is defined at the container level and can be shared between deployable artifacts
 * to synchronize access to shared resources.
 *
 * @since 4.3.0, 4.2.2
 */
public class ServerLockFactory implements LockFactory, Initialisable, Disposable {

  private static final long DEFAULT_LOCK_FACTORY_SHUTDOWN_TIMEOUT = 5000L;
  private LockGroup lockGroup;

  @Inject
  private MuleConfiguration muleConfiguration;

  @Override
  public synchronized Lock createLock(String lockId) {
    return new LockAdapter(lockId, lockGroup);
  }

  @Override
  public void dispose() {
    if (lockGroup != null) {
      lockGroup.dispose();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    lockGroup = new InstanceLockGroup(new SingleServerLockProvider(), getShutdownTimeout());
  }

  private long getShutdownTimeout() {
    if (muleConfiguration == null) {
      return DEFAULT_LOCK_FACTORY_SHUTDOWN_TIMEOUT;
    } else {
      return muleConfiguration.getShutdownTimeout();
    }
  }

  private static class LockAdapter implements Lock {

    private final String lockId;
    private final LockGroup lockGroup;

    public LockAdapter(String lockId, LockGroup lockGroup) {
      this.lockId = lockId;
      this.lockGroup = lockGroup;
    }

    @Override
    public void lock() {
      lockGroup.lock(lockId);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      lockGroup.lockInterruptibly(lockId);
    }

    @Override
    public boolean tryLock() {
      return lockGroup.tryLock(lockId);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
      return lockGroup.tryLock(lockId, timeout, timeUnit);
    }

    @Override
    public void unlock() {
      lockGroup.unlock(lockId);
    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException("Operation not supported by mule locks");
    }
  }

}
