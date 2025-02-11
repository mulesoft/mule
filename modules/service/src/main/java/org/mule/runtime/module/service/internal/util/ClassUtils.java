/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.util;

import static org.mule.metadata.java.api.utils.ClassUtils.getInnerClassName;
import static org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory.getDefaultFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extend the Apache Commons ClassUtils to provide additional functionality.
 * <p/>
 * <p>
 * This class is useful for loading resources and classes in a fault tolerant manner that works across different applications
 * servers. The resource and classloading methods are SecurityManager friendly.
 * </p>
 */
public class ClassUtils {

  private static final Map<Class<?>, Class<?>> wrapperToPrimitiveMap = new HashMap<>();
  private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

  static {
    wrapperToPrimitiveMap.put(Boolean.class, Boolean.TYPE);
    wrapperToPrimitiveMap.put(Byte.class, Byte.TYPE);
    wrapperToPrimitiveMap.put(Character.class, Character.TYPE);
    wrapperToPrimitiveMap.put(Short.class, Short.TYPE);
    wrapperToPrimitiveMap.put(Integer.class, Integer.TYPE);
    wrapperToPrimitiveMap.put(Long.class, Long.TYPE);
    wrapperToPrimitiveMap.put(Double.class, Double.TYPE);
    wrapperToPrimitiveMap.put(Float.class, Float.TYPE);
    wrapperToPrimitiveMap.put(Void.TYPE, Void.TYPE);

    Set<Class<?>> primitiveTypes = new HashSet<>(32);
    primitiveTypes.addAll(wrapperToPrimitiveMap.values());
    for (Class<?> primitiveType : primitiveTypes) {
      primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
    }
  }

  /**
   * Load a class with a given name.
   * <p/>
   * It will try to load the class in the following order:
   * <ul>
   * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
   * <li>Using the basic {@link Class#forName(java.lang.String) }
   * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
   * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
   * </ul>
   *
   * @param className    The name of the class to load
   * @param callingClass The Class object of the calling object
   * @return The Class instance
   * @throws ClassNotFoundException If the class cannot be found anywhere.
   */
  public static Class loadClass(final String className, final Class<?> callingClass) throws ClassNotFoundException {
    return loadClass(className, callingClass, Object.class);
  }

  /**
   * Load a class with a given name.
   * <p/>
   * It will try to load the class in the following order:
   * <ul>
   * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
   * <li>Using the basic {@link Class#forName(java.lang.String) }
   * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
   * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
   * </ul>
   *
   * @param className    The name of the class to load
   * @param callingClass The Class object of the calling object
   * @param type         the class type to expect to load
   * @return The Class instance
   * @throws ClassNotFoundException If the class cannot be found anywhere.
   */
  public static <T extends Class> T loadClass(final String className, final Class<?> callingClass, T type)
      throws ClassNotFoundException {
    if (className.length() <= 8) {
      // Could be a primitive - likely.
      if (primitiveTypeNameMap.containsKey(className)) {
        return (T) primitiveTypeNameMap.get(className);
      }
    }

    Class<?> clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
      try {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl.loadClass(className) : null;

      } catch (ClassNotFoundException e) {
        return null;
      }
    });

    if (clazz == null) {
      clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
        try {
          return Class.forName(className);
        } catch (ClassNotFoundException e) {
          return null;
        }
      });
    }

    if (clazz == null) {
      clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
        try {
          return ClassUtils.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
          return null;
        }
      });
    }

    if (clazz == null) {
      clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
        try {
          return callingClass.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
          return null;
        }
      });
    }

    if (clazz == null) {
      throw new ClassNotFoundException(className);
    }

    if (type.isAssignableFrom(clazz)) {
      return (T) clazz;
    } else {
      throw new IllegalArgumentException(String.format("Loaded class '%s' is not assignable from type '%s'", clazz.getName(),
                                                       type.getName()));
    }
  }

  /**
   * Load a class with a given name from the given classloader.
   * <p/>
   * This method must be used when the resource to load it's dependant on user configuration.
   *
   * @param className   the name of the class to load
   * @param classLoader the loader to load it from
   * @return the instance of the class
   * @throws ClassNotFoundException if the class is not available in the class loader
   */
  public static Class loadClass(final String className, final ClassLoader classLoader) throws ClassNotFoundException {
    Class<?> clazz;
    try {
      clazz = classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      try {
        clazz = classLoader.loadClass(getInnerClassName(className));
      } catch (ClassNotFoundException e2) {
        throw getDefaultFactory().createClassNotFoundException(className, classLoader);
      }
    }
    return clazz;
  }

  public static <T> T instantiateClass(Class<? extends T> clazz, Object... constructorArgs) throws SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class<?>[] args;
    if (constructorArgs != null) {
      args = new Class[constructorArgs.length];
      for (int i = 0; i < constructorArgs.length; i++) {
        if (constructorArgs[i] == null) {
          args[i] = null;
        } else {
          args[i] = constructorArgs[i].getClass();
        }
      }
    } else {
      args = new Class[0];
    }

    // try the arguments as given
    Constructor<?> ctor = getConstructor(clazz, args);

    if (ctor == null) {
      // try again but adapt value classes to primitives
      ctor = getConstructor(clazz, wrappersToPrimitives(args));
    }

    if (ctor == null) {
      StringBuilder argsString = new StringBuilder(100);
      for (Class<?> arg : args) {
        argsString.append(arg.getName()).append(", ");
      }
      throw new NoSuchMethodException("could not find constructor on class: " + clazz + ", with matching arg params: "
          + argsString);
    }

    return (T) ctor.newInstance(constructorArgs);
  }

  public static Object instantiateClass(String name, Object... constructorArgs) throws ClassNotFoundException, SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    return instantiateClass(name, constructorArgs, (ClassLoader) null);
  }

  public static Object instantiateClass(String name, Object[] constructorArgs, ClassLoader classLoader)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Class<?> clazz;
    if (classLoader != null) {
      clazz = loadClass(name, classLoader);
    } else {
      clazz = loadClass(name, ClassUtils.class);
    }
    if (clazz == null) {
      throw new ClassNotFoundException(name);
    }
    return instantiateClass(clazz, constructorArgs);
  }

  public static Constructor getConstructor(Class clazz, Class[] paramTypes) {
    return getConstructor(clazz, paramTypes, false);
  }

  /**
   * Returns available constructor in the target class that as the parameters specified.
   *
   * @param clazz      the class to search
   * @param paramTypes the param types to match against
   * @param exactMatch should exact types be used (i.e. equals rather than isAssignableFrom.)
   * @return The matching constructor or null if no matching constructor is found
   */
  public static Constructor getConstructor(Class clazz, Class[] paramTypes, boolean exactMatch) {
    for (Constructor ctor : clazz.getConstructors()) {
      Class[] types = ctor.getParameterTypes();
      if (types.length == paramTypes.length) {
        int matchCount = 0;
        for (int x = 0; x < types.length; x++) {
          if (paramTypes[x] == null) {
            matchCount++;
          } else {
            if (exactMatch) {
              if (paramTypes[x].equals(types[x]) || types[x].equals(paramTypes[x])) {
                matchCount++;
              }
            } else {
              if (paramTypes[x].isAssignableFrom(types[x]) || types[x].isAssignableFrom(paramTypes[x])) {
                matchCount++;
              }
            }
          }
        }
        if (matchCount == types.length) {
          return ctor;
        }
      }
    }
    return null;
  }

  public static Class[] wrappersToPrimitives(Class[] wrappers) {
    if (wrappers == null) {
      return null;
    }

    if (wrappers.length == 0) {
      return wrappers;
    }

    Class[] primitives = new Class[wrappers.length];

    for (int i = 0; i < wrappers.length; i++) {
      primitives[i] = wrapperToPrimitiveMap.getOrDefault(wrappers[i], wrappers[i]);
    }

    return primitives;
  }

  private ClassUtils() {}
}
