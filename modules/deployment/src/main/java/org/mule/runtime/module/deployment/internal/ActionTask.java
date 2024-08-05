/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

/**
 * An action task to be executed.
 */
public interface ActionTask {

  /**
   * Tries to execute the action task.
   *
   * @return a boolean to know if the action was done or not.
   */
  boolean tryAction();

}
