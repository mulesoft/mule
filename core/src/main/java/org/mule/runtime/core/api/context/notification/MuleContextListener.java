/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
   * @param context  initialized context
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
