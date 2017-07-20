/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

/**
 * Implementations of this class should take care of resources initialization with System Class Loader
 * as if they are loaded with application's Class Loader it may leak memory after application is undeployment. Mule
 * ensures to create an instance of this class with the System Class Loader.
 */
public interface ResourceInitializer {

  /**
   * Attempts to initialize resources that should not be initialized from an application class loader.
   * System or parent classloader is the {@link Thread#contextClassLoader} of the current
   * thread when method is invoked.
   */
  void initialize();

}
