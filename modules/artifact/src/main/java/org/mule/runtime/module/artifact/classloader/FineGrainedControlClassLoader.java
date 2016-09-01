/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.module.artifact.classloader.exception.CompositeClassNotFoundException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;

import sun.misc.CompoundEnumeration;
import sun.net.www.protocol.jar.Handler;

/**
 * Defines a {@link ClassLoader} which enables the control of the class loading lookup mode.
 * <p/>
 * By using a {@link ClassLoaderLookupPolicy} this classLoader can use parent-first, parent-only or child-first classloading
 * lookup mode per package.
 */
public class FineGrainedControlClassLoader extends URLClassLoader
    implements DisposableClassLoader, ClassLoaderLookupPolicyProvider {

  static {
    registerAsParallelCapable();
  }

  private final ClassLoaderLookupPolicy lookupPolicy;

  public FineGrainedControlClassLoader(URL[] urls, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy) {
    super(urls, parent, new NonCachingURLStreamHandlerFactory());
    checkArgument(lookupPolicy != null, "Lookup policy cannot be null");
    this.lookupPolicy = lookupPolicy;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> result = findLoadedClass(name);

    if (result != null) {
      return result;
    }

    final ClassLoaderLookupStrategy lookupStrategy = lookupPolicy.getLookupStrategy(name);
    if (lookupStrategy == null) {
      throw new NullPointerException(format("Unable to find a lookup strategy for '%s' from %s", name, this));
    }

    // Gather information about the exceptions in each of the searched classloaders to provide
    // troubleshooting information in case of throwing a ClassNotFoundException.
    ClassNotFoundException firstException = null;

    try {
      if (lookupStrategy == PARENT_ONLY) {
        result = findParentClass(name);
      } else if (lookupStrategy == PARENT_FIRST) {
        try {
          result = findParentClass(name);
        } catch (ClassNotFoundException e) {
          firstException = e;
          result = findLocalClass(name);
        }
      } else if (lookupStrategy == CHILD_FIRST) {
        try {
          result = findLocalClass(name);
        } catch (ClassNotFoundException e) {
          firstException = e;
          result = findParentClass(name);
        }
      } else {
        result = findLocalClass(name);
      }
    } catch (ClassNotFoundException e) {
      throw new CompositeClassNotFoundException(name, lookupStrategy,
                                                firstException != null ? asList(firstException, e) : asList(e));
    }

    if (resolve) {
      resolveClass(result);
    }

    return result;
  }

  protected Class<?> findParentClass(String name) throws ClassNotFoundException {
    if (getParent() != null) {
      return getParent().loadClass(name);
    } else {
      return findSystemClass(name);
    }
  }

  @Override
  public URL getResource(String name) {
    URL url = findResource(name);
    if (url == null && getParent() != null) {

      url = getParent().getResource(name);
    }
    return url;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[2];
    tmp[0] = findResources(name);
    if (getParent() != null) {
      tmp[1] = getParent().getResources(name);
    }

    return new CompoundEnumeration<>(tmp);
  }

  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> result = findLoadedClass(name);

      if (result != null) {
        return result;
      }

      return super.findClass(name);
    }
  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return lookupPolicy;
  }

  /**
   * Disposes the {@link ClassLoader} by closing all the resources opened by this {@link ClassLoader}. See
   * {@link URLClassLoader#close()}.
   */
  @Override
  public void dispose() {
    try {
      // Java 7 added support for closing a URLClassLoader, it will close any resources opened by this classloader
      close();
    } catch (IOException e) {
      // ignore
    }

    try {
      // fix groovy compiler leaks http://www.mulesoft.org/jira/browse/MULE-5125
      final Class clazz = ClassUtils.loadClass("org.codehaus.groovy.transform.ASTTransformationVisitor", getClass());
      final Field compUnit = clazz.getDeclaredField("compUnit");
      compUnit.setAccessible(true);
      // static field
      compUnit.set(null, null);
    } catch (Throwable t) {
      // ignore
    }
  }

  protected static class NonCachingURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
      return new NonCachingJarResourceURLStreamHandler();
    }
  }

  /**
   * Prevents jar caching for this classloader, mainly to fix the static ResourceBundle mess/cache that keeps connections open no
   * matter what.
   */
  private static class NonCachingJarResourceURLStreamHandler extends Handler {

    public NonCachingJarResourceURLStreamHandler() {
      super();
    }

    @Override
    protected java.net.URLConnection openConnection(URL u) throws IOException {
      JarURLConnection c = new sun.net.www.protocol.jar.JarURLConnection(u, this);
      c.setUseCaches(false);
      return c;
    }
  }
}
