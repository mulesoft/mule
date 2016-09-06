/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
