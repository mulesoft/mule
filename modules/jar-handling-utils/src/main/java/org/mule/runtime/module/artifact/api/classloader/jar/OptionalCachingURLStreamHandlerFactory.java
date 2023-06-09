/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.jar;

/**
 * This class implements a method to return a NonCachingURLStreamHandlerFactory when runtime is Java 8 and a null when running on
 * Java 11+ as we don't need a NonCachingURLStreamHandlerFactory for those JDKs as there is no file descriptor leaks
 */
public class OptionalCachingURLStreamHandlerFactory {

  public static NonCachingURLStreamHandlerFactory getCachingURLStreamHandlerFactory() {
    return new NonCachingURLStreamHandlerFactory();
  }
}
