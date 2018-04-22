/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.core.api.util.CompoundEnumeration;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Classloader implementation that, given a set of classloaders, will first search for a resource/class in the first one. If it is
 * not found, it will try the nextx and so on until the resource/class is found or all classloaders have been tried.
 * <p>
 * For {@link #getResources(String)}, all the classloders will be queried to get the union of all found resources.
 *
 * @since 1.0
 */
public class CompositeClassLoader extends ClassLoader {

  static {
    registerAsParallelCapable();
  }

  private List<ClassLoader> delegates;

  public CompositeClassLoader(ClassLoader first, ClassLoader... others) {
    delegates = new ArrayList<>();
    if (first != null) {
      delegates.add(first);
    }
    delegates.addAll(asList(others).stream().filter(o -> o != null).collect(toList()));
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    ClassNotFoundException firstException = null;
    for (ClassLoader classLoader : delegates) {
      try {
        return classLoader.loadClass(name);
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

}
