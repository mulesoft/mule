/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.tanuki.internal;

import org.tanukisoftware.wrapper.WrapperListener;

/**
 * An implementation of {@link WrapperListener} that does nothing on the callbacks.
 * <p>
 * Used to support propagating error conditions to the native wrapper even before registering a real {@link WrapperListener}
 * implementation.
 *
 * @since 4.5
 * @see MuleContainerTanukiWrapper#haltAndCatchFire(int, String)
 */
class NoOpTanukiWrapperListener implements WrapperListener {

  @Override
  public Integer start(String[] args) {
    return null;
  }

  @Override
  public int stop(int exitCode) {
    return exitCode;
  }

  @Override
  public void controlEvent(int event) {}
}
