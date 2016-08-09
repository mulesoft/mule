/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.lock;

/**
 * Implementation of the Null Object design pattern for the {@link PathLock} interface
 *
 * @since 4.0
 */
public final class NullPathLock implements PathLock {

  /**
   * Does nothing and always returns {@code true}
   *
   * @return {@code true}
   */
  @Override
  public boolean tryLock() {
    return true;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isLocked() {
    return false;
  }

  /**
   * Does nothing regardless of how many invokations the {@link #tryLock()} method has received
   */
  @Override
  public void release() {
    // no-op
  }
}
