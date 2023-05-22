/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import org.mule.runtime.api.component.Component;

import java.util.Set;

/**
 * We use ByteBuddy to enhance classes defined with the Java SDK, in order to make them implement the {@link Component} interface.
 * The classloader used to load such dynamic classes is being hold by a cache in Spring, and that cache can be cleared by calling
 * {@code CachedIntrospectionResults#clearClassLoader()}. Notice that this method can be called with the classloader of the class
 * itself, or any of the parents in its hierarchy.
 * <p>
 * This module doesn't depend on Spring, so the classes in this module can't call those methods. Instead, this interface is
 * intended to register the classloaders used to load the dynamic classes, and expose them to be cleared by someone else.
 * <p>
 * See also: {@code TypeAwareConfigurationFactory#addAnnotationsToClass}.
 * <p>
 * See also: {@code CachedIntrospectionResults#softClassCache}.
 */
public interface WithDynamicClassLoaders {

  /**
   * Registers a classloader.
   *
   * @param classLoader the {@link ClassLoader} to be registered.
   */
  void addDynamicClassLoader(ClassLoader classLoader);

  /**
   * @return a {@link Set} containing all the registered {@link ClassLoader} instances.
   */
  Set<ClassLoader> getDynamicClassLoaders();
}
