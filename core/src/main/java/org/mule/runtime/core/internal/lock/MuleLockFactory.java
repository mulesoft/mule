/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.lock.LockProvider;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;

import java.util.concurrent.locks.Lock;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class MuleLockFactory implements LockFactory, Initialisable, Disposable {

  private LockGroup lockGroup;
  private LockProvider lockProvider;

  @Inject
  private SchedulerService schedulerService;

  // TODO (W-11958289): This MuleContext is injected just to obtain the MuleConfiguration, so the MuleConfiguration
  // should be injected instead. However, when injecting that class here it makes the test
  // LazyInitConfigurationComponentLocatorTestCase fail because the MuleLockFactory is instantiated twice.
  @Inject
  private MuleContext muleContext;

  @Override
  public synchronized Lock createLock(String lockId) {
    return new LockAdapter(lockId, lockGroup, schedulerService);
  }

  @Override
  public void dispose() {
    if (lockGroup != null) {
      lockGroup.dispose();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    lockGroup = createLockGroup();
  }

  @Inject
  @Named(OBJECT_LOCK_PROVIDER)
  public void setLockProvider(LockProvider lockProvider) {
    this.lockProvider = lockProvider;
  }

  private LockGroup createLockGroup() {
    // This class is created programmatically, and in such case the mule context isn't injected.
    if (muleContext == null) {
      return new InstanceLockGroup(lockProvider);
    }

    MuleConfiguration muleConfiguration = muleContext.getConfiguration();
    if (muleConfiguration == null) {
      return new InstanceLockGroup(lockProvider);
    }

    return new InstanceLockGroup(lockProvider, muleConfiguration.getShutdownTimeout());
  }
}
