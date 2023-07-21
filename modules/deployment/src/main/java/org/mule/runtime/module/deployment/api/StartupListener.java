/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

/**
 * Notifies when all mule apps has been started
 */
public interface StartupListener {

  /**
   * Invoked after all apps have passed the deployment phase. Any exceptions thrown by implementations will be ignored.
   */
  void onAfterStartup();
}
