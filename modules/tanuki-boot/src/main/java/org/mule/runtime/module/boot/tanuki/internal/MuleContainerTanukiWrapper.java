/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.tanuki.internal;

import static org.tanukisoftware.wrapper.WrapperManager.addWrapperEventListener;

import org.mule.runtime.module.boot.commons.internal.AbstractMuleContainerWrapper;
import org.mule.runtime.module.boot.commons.internal.MuleContainerFactory;
import org.mule.runtime.module.boot.commons.internal.MuleContainerWrapper;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Implementation of {@link MuleContainerWrapper} that uses Tanuki's {@link WrapperManager} and {@link WrapperListener} to
 * interact with the OS.
 *
 * @since 4.5
 */
public class MuleContainerTanukiWrapper extends AbstractMuleContainerWrapper implements MuleContainerWrapper {

  // Taken from org.tanukisoftware.wrapper.event.WrapperEventListener.EVENT_FLAG_LOGGING,
  // but we can't import it directly because Tanuki Wrapper version used for CE does not have it
  private static final long EVENT_FLAG_LOGGING = 4L;

  private boolean isStarted = false;

  @Override
  public void configureAndStart(MuleContainerFactory muleContainerFactory, CommandLine commandLine) {
    // Force early initialization of sun.util.locale.provider.LocaleProviderAdapter to prevent a deadlock between that class
    // initialization and the configuration of the logging system (that deadlock manifests only when using tanuki's wrapperDebug
    // option in Java 21).
    NumberFormat.getInstance(Locale.getDefault());

    super.configureAndStart(muleContainerFactory, commandLine);
  }

  @Override
  protected void start(MuleContainerFactory muleContainerFactory, String[] args) {
    WrapperListener wrapperListener =
        new MuleContainerTanukiWrapperListener(muleContainerFactory, getAllConfigurersReady(), this::dispose);
    WrapperManager.start(wrapperListener, args);
    isStarted = true;
  }

  @Override
  public void haltAndCatchFire(int exitCode, String message) {
    // If #start has not been called yet, we need to start a NoOp WrapperListener to notify the native wrapper and allow
    // events to be processed
    if (!isStarted) {
      WrapperManager.start(new NoOpTanukiWrapperListener(), new String[0]);
    }

    // Attaches a listener for the logging events and tells the native wrapper that the JVM wants to shut down
    addWrapperEventListener(new ErrorLoggingWrapperEventListener(message), EVENT_FLAG_LOGGING);
    WrapperManager.stop(exitCode);
  }

  @Override
  public void stop(int exitCode) {
    WrapperManager.stopAndReturn(exitCode);
  }

  @Override
  public void restart() {
    WrapperManager.restartAndReturn();
  }
}
