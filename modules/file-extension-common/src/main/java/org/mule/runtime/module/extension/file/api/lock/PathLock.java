/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.lock;

/**
 * Use to generate a lock on a file reference by a path.
 * <p>
 * The extent of such lock depends on the actual implementation. For some implementations it will be possible to create a lock at
 * a file system level. For other implementations this might not be possible (mainly depending on the targeted file system) and
 * thus will use different locking mechanisms.
 * <p>
 * All implementations of this interface must be (naturally) thread-safe and reentrant, meaning that a lock can be re obtained on
 * {@code this} instance it's been released
 *
 * @since 4.0
 */
public interface PathLock {

  /**
   * Attempts to obtain a lock on the referenced path.
   * <p>
   * Just like with any locking API, any component invoking this method on {@code this} and obtaining {@code true} as a return
   * value <b>MUST</b> make sure that the {@link #release()} method is eventually invoked on {@code this} same instance.
   *
   * @return {@code true} if the lock could be obtained. {@code false} otherwise.
   */
  boolean tryLock();

  /**
   * @return whether the lock is currently owned by {@code this} instance
   */
  boolean isLocked();

  /**
   * Releases the lock that was previously obtained by invoking {@link #tryLock()} on {@code this} instance with a {@code true}
   * return value.
   * <p>
   * This method will not fail if no such lock was ever obtained or has already been released.
   */
  void release();

}
