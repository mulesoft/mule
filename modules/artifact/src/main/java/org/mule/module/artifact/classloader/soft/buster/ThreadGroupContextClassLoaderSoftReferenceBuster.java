/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader.soft.buster;

import static java.lang.Class.forName;
import static org.apache.commons.lang3.reflect.FieldUtils.getField;
import static org.apache.commons.lang3.reflect.FieldUtils.readStaticField;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reference buster to remove soft reference in thread group context
 * 
 * @since 4.2.3
 */
public class ThreadGroupContextClassLoaderSoftReferenceBuster implements SoftReferenceBuster {

  private static final String VALUE_FIELD = "value";

  private static final String TABLE_FIELD = "table";

  private static final String BEANS_WEAK_IDENTITY_MAP_CLASS = "java.beans.WeakIdentityMap";

  private static final String CLEAR_BEAN_INFO_CACHE_METHOD = "clearBeanInfoCache";

  private static final String THREAD_GROUP_CONTEXT_FIELD = "contexts";

  private static final String THREAD_GROUP_CONTEXT_CLASS = "java.beans.ThreadGroupContext";

  private ClassLoader classLoader;

  public ThreadGroupContextClassLoaderSoftReferenceBuster(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void bustSoftReferences(ClassLoader classloader) {
    Class<?> threadGroupClass = getClass(THREAD_GROUP_CONTEXT_CLASS);

    if (threadGroupClass != null) {
      Object contexts;
      try {
        contexts = readStaticField(threadGroupClass, THREAD_GROUP_CONTEXT_FIELD, true);

        if (contexts != null) {
          final Field tableField = getTableField();
          if (tableField != null) {
            final WeakReference[] table =
                getFieldValue(tableField, contexts);
            if (table != null) {
              Method clearBeanInfoCache = null;
              for (WeakReference entry : table) {
                if (entry != null) {
                  Object context = getFieldValue(entry, VALUE_FIELD);
                  if (context != null) {
                    if (clearBeanInfoCache == null) {
                      clearBeanInfoCache = getMethod(context.getClass(), CLEAR_BEAN_INFO_CACHE_METHOD);
                    }

                    try {
                      clearBeanInfoCache.setAccessible(true);
                      clearBeanInfoCache.invoke(context);
                    } catch (Throwable e) {

                    }
                  }
                }
              }
            }
          }
        }
      } catch (Throwable e) {

      }
    }
  }

  private Field getTableField() {
    return getField(getClass(BEANS_WEAK_IDENTITY_MAP_CLASS), TABLE_FIELD, true);
  }

  private Class<?> getClass(String className) {
    Class<?> threadGroupClass = null;
    try {
      threadGroupClass = forName(className);
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
    }
    return threadGroupClass;
  }

  public static <T> T getFieldValue(Object obj, String fieldName) {
    final Field field = getField(obj.getClass(), fieldName, true);
    return (T) getFieldValue(field, obj);
  }

  public static <T> T getFieldValue(Field field, Object obj) {
    try {
      return (T) field.get(obj);
    } catch (Exception ex) {
      return null;
    }
  }

  public Method getMethod(Class<?> clazz, String methodName) {
    if (clazz != null) {
      try {
        return clazz.getDeclaredMethod(methodName);
      } catch (NoSuchMethodException | SecurityException e) {

      }
    }

    return null;
  }


}
