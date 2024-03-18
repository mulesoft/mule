/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

/**
 * A strategy object for applying lifecycle phases.
 *
 * @since 4.2
 */
public interface LifecycleStrategy {


  /**
   * Calls {@link Initialisable#initialise()} on the given {@code initialisable}
   *
   * @param initialisable an initialisable object
   * @throws InitialisationException if the initialisation fails
   */
  default void initialise(Initialisable initialisable) throws InitialisationException {

  }

  /**
   * Calls {@link Startable#start()} on the given {@code startable}
   *
   * @param startable a startable object
   * @throws MuleException if start fails
   */
  default void start(Startable startable) throws MuleException {

  }

  /**
   * Calls {@link Stoppable#stop()} on the given {@code stoppable}
   *
   * @param stoppable a stoppable object
   * @throws MuleException if stop fails
   */
  default void stop(Stoppable stoppable) throws MuleException {

  }

  /**
   * Calls {@link Disposable#dispose()} ()} on the given {@code disposable}
   *
   * @param disposable a disposable object
   */
  default void dispose(Disposable disposable) {

  }

}
