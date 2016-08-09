/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.lock;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

import java.util.concurrent.locks.Lock;

public class MuleLockFactory implements LockFactory, MuleContextAware, Initialisable, Disposable {

  private LockGroup lockGroup;
  private LockProvider lockProvider;
  private MuleContext muleContext;

  public synchronized Lock createLock(String lockId) {
    return new LockAdapter(lockId, lockGroup);
  }

  @Override
  public void dispose() {
    lockGroup.dispose();
  }

  @Override
  public void initialise() throws InitialisationException {
    if (lockProvider == null) {
      lockProvider = muleContext.getRegistry().get(MuleProperties.OBJECT_LOCK_PROVIDER);
    }
    lockGroup = new InstanceLockGroup(lockProvider);
  }

  public void setLockProvider(LockProvider lockProvider) {
    this.lockProvider = lockProvider;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
