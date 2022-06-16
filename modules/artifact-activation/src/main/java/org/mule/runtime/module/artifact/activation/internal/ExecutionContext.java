/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal;

/**
 * Determines the execution context, either Mule Framework or Mule Runtime.
 */
public class ExecutionContext {

  /**
   * @return true if this is executed while using the Mule Framework.
   */
  public static boolean isMuleFramework() {
    try {
      Class.forName("org.mule.runtime.module.fwk.api.MuleFramework");
    } catch (ClassNotFoundException e) {
      return false;
    }

    return true;
  }

}
