/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.internal.lock.ServerLockFactory;

/**
 * Utility method for getting the runtime {@link LockFactory}.
 *
 * @since 4.3, 4.2.2
 */
public class RuntimeLockFactoryUtil {

  private static LockFactory lockFactory;

  static {
    lockFactory = new ServerLockFactory();
  }

  public static LockFactory getRuntimeLockFactory() {
    return lockFactory;
  }

}
