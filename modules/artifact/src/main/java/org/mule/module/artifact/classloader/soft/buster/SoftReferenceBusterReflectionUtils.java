/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader.soft.buster;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class SoftReferenceBusterReflectionUtils {

  public <E> E getStaticFieldValue(Class<?> clazz, String fieldName) {
    Field staticField = findField(clazz, fieldName);
    return (staticField != null) ? (E) getStaticFieldValue(staticField) : null;
  }

  public static <E> E getStaticFieldValue(String className, String fieldName) {
    return (E) getStaticFieldValue(className, fieldName, false);
  }

  public static <E> E getStaticFieldValue(String className, String fieldName, boolean trySystemCL) {
    Field staticField = findFieldOfClass(className, fieldName, trySystemCL);
    return (staticField != null) ? (E) SoftReferenceBusterReflectionUtils.getStaticFieldValue(staticField) : null;
  }

  public Field findFieldOfClass(String className, String fieldName) {
    return findFieldOfClass(className, fieldName, false);
  }

  public static Field findFieldOfClass(String className, String fieldName, boolean trySystemCL) {
    Class<?> clazz = findClass(className, trySystemCL);
    if (clazz != null) {
      return findField(clazz, fieldName);
    } else
      return null;
  }

  public static Class<?> findClass(String className) {
    return findClass(className, false);
  }

  public static Class<?> findClass(String className, boolean trySystemCL) {
    try {
      return Class.forName(className);
    }
    // catch (NoClassDefFoundError e) {
    // // Silently ignore
    // return null;
    // }
    catch (ClassNotFoundException e) {
      if (trySystemCL) {
        try {
          return Class.forName(className, true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e1) {
          // Silently ignore
          return null;
        }
      }
      // Silently ignore
      return null;
    } catch (Exception ex) { // Example SecurityException
      return null;
    }
  }

  public static Field findField(Class<?> clazz, String fieldName) {
    if (clazz == null)
      return null;

    try {
      final Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true); // (Field is probably private)
      return field;
    } catch (NoSuchFieldException ex) {
      // Silently ignore
      return null;
    } catch (Exception ex) { // Example SecurityException
      return null;
    }
  }

  public static <T> T getStaticFieldValue(Field field) {
    try {
      if (!Modifier.isStatic(field.getModifiers())) {
        return null;
      }

      return (T) field.get(null);
    } catch (Exception ex) {
      // Silently ignore
      return null;
    }
  }

  public static <T> T getFieldValue(Object obj, String fieldName) {
    final Field field = findField(obj.getClass(), fieldName);
    return (T) SoftReferenceBusterReflectionUtils.getFieldValue(field, obj);
  }

  public static <T> T getFieldValue(Field field, Object obj) {
    try {
      return (T) field.get(obj);
    } catch (Exception ex) {
      // Silently ignore
      return null;
    }
  }

  public void setFinalStaticField(Field field, Object newValue) {
    // Allow modification of final field
    try {
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    } catch (NoSuchFieldException e) {

    } catch (IllegalAccessException e) {

    } catch (Throwable t) {

    }

    // Update the field
    try {
      field.set(null, newValue);
    } catch (Throwable e) {

    }
  }

  public Method findMethod(String className, String methodName, Class... parameterTypes) {
    Class<?> clazz = findClass(className);
    if (clazz != null) {
      return findMethod(clazz, methodName, parameterTypes);
    } else
      return null;
  }

  public static Method findMethod(Class<?> clazz, String methodName, Class... parameterTypes) {
    if (clazz == null)
      return null;

    try {
      final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException ex) {
      // Silently ignore
      return null;
    }
  }

}
