/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.util.concurrent.locks.Lock;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

public class MuleLockFactory implements LockFactory, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(MuleLockFactory.class);

  private LockGroup lockGroup;
  private LockProvider lockProvider;

  @Inject
  private SchedulerService schedulerService;

  @Override
  public synchronized Lock createLock(String lockId) {
    return new LockAdapter(lockId, lockGroup, schedulerService);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(lockGroup, LOGGER);
  }

  @Override
  public void initialise() throws InitialisationException {
    lockGroup = new InstanceLockGroup(lockProvider);
  }

  @Inject
  @Named(OBJECT_LOCK_PROVIDER)
  public void setLockProvider(LockProvider lockProvider) {
    this.lockProvider = lockProvider;
  }
}
