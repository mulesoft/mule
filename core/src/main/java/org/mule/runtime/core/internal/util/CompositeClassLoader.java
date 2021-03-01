/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Stream.of;
import org.mule.runtime.core.api.util.CompoundEnumeration;

import com.github.benmanes.caffeine.cache.Cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Classloader implementation that, given a set of classloaders, will first search for a resource/class in the first one. If it is
 * not found, it will try the next and so on until the resource/class is found or all classloaders have been tried.
 * <p>
 * For {@link #getResources(String)}, all the classloaders will be queried to get the union of all found resources.
 *
 * @since 1.0
 */
public class CompositeClassLoader extends ClassLoader {

  static {
    registerAsParallelCapable();
  }

  private List<ClassLoader> delegates;

  private static final Cache<String, CompositeClassLoader> cache = newBuilder()
      .maximumSize(1024).expireAfterAccess(1, MINUTES).weakValues().build();

  private CompositeClassLoader(ClassLoader... classLoaders) {
    delegates = new ArrayList<>(classLoaders.length);
    for (ClassLoader cl : classLoaders) {
      if (cl != null) {
        delegates.add(cl);
      }
    }
    delegates = unmodifiableList(delegates);
  }

  public static CompositeClassLoader from(ClassLoader... classLoaders) {
    String compositeId = of(classLoaders).filter(Objects::nonNull).map(System::identityHashCode).map(Object::toString)
        .collect(Collectors.joining("#"));
    return cache.get(compositeId, id -> new CompositeClassLoader(classLoaders));
  }

  /**
   * Overrides the loadClass in order to support scenarios where a custom class loader is created in a plugin
   * and these calls to this method explicitly.
   *
   * @param name    The <a href="#name">binary name</a> of the class
   * @param resolve If <tt>true</tt> then resolve the class
   * @return The resulting <tt>Class</tt> object
   * @throws ClassNotFoundException If the class could not be found
   */
  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    ClassNotFoundException firstException = null;
    for (ClassLoader classLoader : delegates) {
      try {
        Class<?> result = classLoader.loadClass(name);

        if (resolve) {
          resolveClass(result);
        }

        return result;
      } catch (ClassNotFoundException e) {
        firstException = e;
      }
    }
    throw firstException;
  }

  @Override
  public URL getResource(String name) {
    URL resource;
    for (ClassLoader classLoader : delegates) {
      resource = classLoader.getResource(name);
      if (resource != null) {
        return resource;
      }
    }

    return null;
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    InputStream resourceAsStream;
    for (ClassLoader classLoader : delegates) {
      resourceAsStream = classLoader.getResourceAsStream(name);
      if (resourceAsStream != null) {
        return resourceAsStream;
      }
    }

    return null;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[delegates.size()];
    int i = 0;
    for (ClassLoader classLoader : delegates) {
      tmp[i++] = classLoader.getResources(name);
    }

    return new CompoundEnumeration<>(tmp);
  }

  public List<ClassLoader> getDelegates() {
    return delegates;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + delegates.toString();
  }
}
