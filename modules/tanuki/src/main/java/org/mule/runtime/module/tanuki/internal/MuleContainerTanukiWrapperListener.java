/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tanuki.internal;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_ERROR;
import static org.tanukisoftware.wrapper.WrapperManager.getJavaPID;
import static org.tanukisoftware.wrapper.WrapperManager.getWrapperPID;
import static org.tanukisoftware.wrapper.WrapperManager.log;

import org.mule.runtime.module.reboot.internal.MuleContainer;
import org.mule.runtime.module.reboot.internal.MuleContainerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Implementation of Tanuki's {@link WrapperListener} that creates and starts a {@link MuleContainer} obtained from a given
 * {@link MuleContainerFactory}.
 *
 * @since 4.5. This is actually an adaptation of what used to be the MuleContainerWrapper back in 4.4.
 */
class MuleContainerTanukiWrapperListener implements WrapperListener {

  private final MuleContainerFactory muleContainerFactory;
  private final BooleanSupplier configurationsReadyBarrier;
  private final Runnable onStop;
  private MuleContainer muleContainer;

  public MuleContainerTanukiWrapperListener(MuleContainerFactory muleContainerFactory,
                                            BooleanSupplier configurationsReadyBarrier,
                                            Runnable onStop) {
    this.muleContainerFactory = muleContainerFactory;
    this.configurationsReadyBarrier = configurationsReadyBarrier;
    this.onStop = onStop;
  }

  /**
   * The start method is called when the WrapperManager is signaled by the native wrapper code that it can start its application.
   * This method call is expected to return, so a new thread should be launched if necessary.
   *
   * @param args List of arguments used to initialize the application.
   * @return Any error code if the application should exit on completion of the start method. If there were no problems then this
   *         method should return null.
   */
  @Override
  public Integer start(String[] args) {
    try {
      muleContainer = muleContainerFactory.create(args);
      startWithContainerClassLoader();
      return null;
    } catch (Exception e) {
      muleContainer = null;
      StringWriter stackWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stackWriter));

      log(WRAPPER_LOG_LEVEL_ERROR, stackWriter.toString());
      return 1;
    }
  }

  /**
   * Called when the application is shutting down. The Wrapper assumes that this method will return fairly quickly. If the
   * shutdown code code could potentially take a long time, then WrapperManager.signalStopping() should be called to extend the
   * timeout period. If for some reason, the stop method can not return, then it must call WrapperManager.stopped() to avoid
   * warning messages from the Wrapper.
   *
   * @param exitCode The suggested exit code that will be returned to the OS when the JVM exits.
   * @return The exit code to actually return to the OS. In most cases, this should just be the value of exitCode, however the
   *         user code has the option of changing the exit code if there are any problems during shutdown.
   */
  @Override
  public int stop(int exitCode) {
    try {
      onStop.run();
      if (muleContainer != null) {
        muleContainer.shutdown();
        muleContainer = null;
      }
    } catch (Throwable t) {
      // ignore
    }

    return exitCode;
  }

  /**
   * Called whenever the native wrapper code traps a system control signal against the Java process. It is up to the callback to
   * take any actions necessary. Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT, WRAPPER_CTRL_CLOSE_EVENT,
   * WRAPPER_CTRL_LOGOFF_EVENT, or WRAPPER_CTRL_SHUTDOWN_EVENT
   *
   * @param event The system control signal.
   */
  @Override
  public void controlEvent(int event) {
    if (WrapperManager.isControlledByNativeWrapper()) {
      // The Wrapper will take care of this event
    } else {
      // We are not being controlled by the Wrapper, so
      // handle the event ourselves.
      if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT)
          || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
        WrapperManager.stop(0);
      }
    }
  }

  private void startWithContainerClassLoader() throws Exception {
    List<String> additionalSplashEntries = asList(format("Wrapper PID: %d", getWrapperPID()),
                                                  format("Java PID: %d", getJavaPID()));
    ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(muleContainer.getClass().getClassLoader());
    try {
      muleContainer.start(configurationsReadyBarrier, additionalSplashEntries);
    } finally {
      currentThread().setContextClassLoader(originalClassLoader);
    }
  }
}
