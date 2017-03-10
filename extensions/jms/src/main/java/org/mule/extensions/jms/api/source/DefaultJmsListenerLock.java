/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;

import java.util.concurrent.Semaphore;

/**
 * Default implementation of {@link JmsListenerLock} based in the usage of {@link Semaphore} to support
 * the capability of locking and unlocking the listener from different {@link Thread threads}
 *
 * @since 4.0
 * @see JmsListenerLock
 * @see JmsListener
 */
public class DefaultJmsListenerLock implements JmsListenerLock {

  private Semaphore semaphore = new Semaphore(0);
  private boolean isFailure = false;
  private Throwable cause;

  /**
   * {@inheritDoc}
   */
  @Override
  public void lock() {
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      throw new MuleRuntimeException(createStaticMessage("The JMS Listener Lock has been interrupted."), cause);
    }
    if (isFailure) {
      throw new MuleRuntimeException(cause);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlockWithFailure(Error error) {
    isFailure = true;
    cause = error.getCause();
    releaseIfNecessary();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlock() {
    isFailure = false;
    releaseIfNecessary();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLocked() {
    return semaphore.availablePermits() == 0;
  }

  private void releaseIfNecessary() {
    if (isLocked()) {
      semaphore.release();
    }
  }
}
