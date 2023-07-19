/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
