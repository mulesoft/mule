/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal;

import org.mule.runtime.api.util.LazyValue;

/**
 * Determines the execution environment, either Mule Framework or Mule Runtime.
 */
public class ExecutionEnvironment {

  private static final LazyValue<Boolean> MULE_FRAMEWORK_LOADED = new LazyValue<>(ExecutionEnvironment::isMuleFrameworkLoaded);

  private static boolean isMuleFrameworkLoaded() {
    try {
      Class.forName("org.mule.framework.api.MuleFramework");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * @return true if this is executed while using the Mule Framework.
   */
  public static boolean isMuleFramework() {
    return MULE_FRAMEWORK_LOADED.get();
  }

}
