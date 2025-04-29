/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.jar;

import java.net.URLStreamHandlerFactory;

/**
 * This class is intended to disable caching for URL connections. For Java 11+, we simply call the static method
 * {@link java.net.URLConnection#setDefaultUseCaches}, that was introduced in Java 9. Up to Java 8, we return a
 * {@link NonCachingURLStreamHandlerFactory} to disable it on connection creation.
 *
 * @since 4.5
 */
public class CachingURLStreamHandlerFactory {

  public static URLStreamHandlerFactory getCachingURLStreamHandlerFactory() {
    return new NonCachingURLStreamHandlerFactory();
  }

}
