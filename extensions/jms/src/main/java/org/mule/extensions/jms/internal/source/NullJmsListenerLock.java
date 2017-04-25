/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.source;

import org.mule.runtime.api.message.Error;

/**
 * Null pattern implementation for {@link JmsListenerLock} which doesn't lock the listener waiting for the message
 * processing.
 * The usage of this lock will led to the {@link JmsListenerLock} work asynchronously.
 *
 * @since 4.0
 */
public class NullJmsListenerLock implements JmsListenerLock {

  @Override
  public void lock() {

  }

  @Override
  public void unlock() {

  }

  @Override
  public void unlockWithFailure() {

  }

  @Override
  public void unlockWithFailure(Error error) {

  }

  @Override
  public boolean isLocked() {
    return false;
  }
}
