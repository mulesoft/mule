/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading.api;

import java.util.HashMap;
import java.util.Map;

public class ClassLoadingHelper {

  public static Map<String, ClassLoader> createdClassLoaders = new HashMap<>();

  public static void addClassLoader(String element) {
    createdClassLoaders.put(element, Thread.currentThread().getContextClassLoader());
  }
}
