/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.environment.api;

/**
 * Represents a runtime environment. Some environments will be an application server, other will only involve an app. This is open
 * for extension.
 *
 * @since 4.7.0
 */
public interface RuntimeEnvironment {

  /**
   * @return the name of the runtime environment.
   */
  String getName();

  /**
   * @return the description of the runtime environment.
   */
  String getDescription();

  /**
   * returns whether it is single app
   */
  default boolean isSingleApp() {
    return false;
  }

  /**
   * Starts the runtime environment.
   */
  void start();
}
