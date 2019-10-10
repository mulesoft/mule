/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.internal.lock.ServerLockFactory;

/**
 * Utility method for getting the runtime {@link LockFactory}.
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
