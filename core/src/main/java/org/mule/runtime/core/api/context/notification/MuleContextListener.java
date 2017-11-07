/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;

/**
 * Listens to public events raised from {@link MuleContext}
 */
public interface MuleContextListener {

  /**
   * Notifies the creation of a {@link MuleContext} instance right before the initialization.
   *
   * @param context created context
   */
  void onCreation(MuleContext context);

  /**
   * Notifies after initialization of a {@link MuleContext} instance.
   *
   * @param context initialized context
   * @param registry the registry of the initialized context
   */
  void onInitialization(MuleContext context, Registry registry);

  /**
   * Notifies the stopping of a {@link MuleContext} instance.
   *
   * @param context configured context
   */
  void onStart(MuleContext context, Registry registry);

  /**
   * Notifies the stopping of a {@link MuleContext} instance.
   *
   * @param context configured context
   */
  void onStop(MuleContext context, Registry registry);
}
