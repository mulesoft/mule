/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.core.api.exception.ResourceNotFoundException;

/**
 * Factory for creating exceptions related to class loading or resource loading issues.
 * <p/>
 * This must be the way to create all mule related class loading or resource loading when the resource to load it's dependant on
 * user configuration.
 *
 * @since 4.2
 */
public interface ClassLoaderResourceNotFoundExceptionFactory {

  /**
   * Creates a {@link ResourceNotFoundException}
   *
   * @param resourceName      the resource to load.
   * @param classLoader       the classloader to use for loading the resource.
   * @param triedAbsolutePath if the resource was tried to be loaded from an absolute path as well.
   * @return a new {@link ResourceNotFoundException}
   */
  ResourceNotFoundException createResourceNotFoundException(String resourceName, ClassLoader classLoader,
                                                            boolean triedAbsolutePath);

  /**
   * Creates a {@link ClassNotFoundException}
   *
   * @param className   the class to load
   * @param classLoader the classloader to use for loading the class.
   * @return a new {@link ClassNotFoundException}
   */
  ClassNotFoundException createClassNotFoundException(String className, ClassLoader classLoader);

  /**
   * @return the default {@link ClassLoaderResourceNotFoundExceptionFactory} to use when there's no other implementation found.
   */
  static ClassLoaderResourceNotFoundExceptionFactory getDefaultFactory() {
    return new DefaultClassLoaderResourceNotFoundExceptionFactory();
  }

  class DefaultClassLoaderResourceNotFoundExceptionFactory implements ClassLoaderResourceNotFoundExceptionFactory {

    @Override
    public ResourceNotFoundException createResourceNotFoundException(String resourceName, ClassLoader classLoader,
                                                                     boolean triedAbsolutePath) {
      return new ResourceNotFoundException(createStaticMessage(getResourceNotFoundErrorMessage(resourceName, triedAbsolutePath)));
    }

    @Override
    public ClassNotFoundException createClassNotFoundException(String className, ClassLoader classLoader) {
      return new ClassNotFoundException(getClassNotFoundErrorMessage(className));
    }

  }

  static String getResourceNotFoundErrorMessage(String resourceName, boolean triedAbsolutePath) {
    if (triedAbsolutePath) {
      return format("Couldn't find resource: %s neither on classpath or in file system", resourceName);
    }
    return format("Couldn't find resource: %s on classpath", resourceName);
  }

  static String getClassNotFoundErrorMessage(String className) {
    return "Couldn't find class: " + className;
  }

}
