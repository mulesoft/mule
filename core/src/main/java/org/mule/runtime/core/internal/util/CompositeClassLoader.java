/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.hash;
import static java.util.concurrent.TimeUnit.MINUTES;
import org.mule.runtime.core.api.util.CompoundEnumeration;

import com.github.benmanes.caffeine.cache.Cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

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

  private static final Cache<List<Integer>, CompositeClassLoader> cache = newBuilder()
      .maximumSize(1024).weakValues().expireAfterAccess(1, MINUTES).build();

  CompositeClassLoader(ClassLoader first, ClassLoader second) {
    if (first != null && second != null) {
      delegates = unmodifiableList(asList(first, second));
    } else if (first != null) {
      delegates = unmodifiableList(singletonList(first));
    } else if (second != null) {
      delegates = unmodifiableList(singletonList(second));
    } else {
      delegates = unmodifiableList(emptyList());
    }
  }

  CompositeClassLoader(ClassLoader... classLoaders) {
    delegates = new ArrayList<>(classLoaders.length);
    for (ClassLoader cl : classLoaders) {
      if (cl != null) {
        delegates.add(cl);
      }
    }
    delegates = unmodifiableList(delegates);
  }

  public static CompositeClassLoader from(ClassLoader... classLoaders) {
    List<Integer> key = getKey(classLoaders);
    return cache.get(key, id -> new CompositeClassLoader(classLoaders));
  }

  public static CompositeClassLoader from(ClassLoader first, ClassLoader second) {
    List<Integer> key = getKey(first, second);
    return cache.get(key, id -> new CompositeClassLoader(first, second));
  }

  private static List<Integer> getKey(ClassLoader... classLoaders) {
    List<Integer> key = new ArrayList<>(classLoaders.length);
    for (ClassLoader cl : classLoaders) {
      if (cl != null) {
        key.add(identityHashCode(cl));
      }
    }
    return key;
  }

  private static List<Integer> getKey(ClassLoader first, ClassLoader second) {
    return new ShortList<>(identityHashCode(first), identityHashCode(second));
  }

  private static class ShortList<T> extends AbstractList<T> {

      final T first;
      final T second;
      final T third;

      ShortList(T first) {
          this.first = first;
          this.second = null;
          this.third = null;
      }

      ShortList(T first, T second) {
          this.first = first;
          this.second = second;
          this.third = null;
      }

      ShortList(T first, T second, T third) {
          this.first = first;
          this.second = second;
          this.third = third;
      }

      @Override
      public T get(int index) {
          if (index >= size()) {
              throw new IndexOutOfBoundsException();
          }
          if (index == 0) {
              return first;
          }
          if (index == 1) {
              return second;
          }
          if (index == 2) {
              return third;
          }
          throw new IndexOutOfBoundsException();
      }

      @Override
      public int size() {
          if (third != null) {
              return 3;
          }
          if (second != null) {
              return 2;
          }
          return 1;
      }

      @Override
      public int hashCode() {
          return hash(first, second, third);
      }

      @Override
      public boolean equals(Object o) {
          if (!(o instanceof ShortList)) {
              return false;
          }

          ShortList<T> other = (ShortList) o;
          return Objects.equals(first, other.first) &&
                 Objects.equals(second, other.second) &&
                 Objects.equals(third, other.third);
      }
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
