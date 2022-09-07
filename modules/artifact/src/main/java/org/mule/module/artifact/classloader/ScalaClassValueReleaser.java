/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Addresses some known leaks in usages of Scala language from plugins/apps.
 * <p>
 * IMPORTANT: this class is on a different package than the rest of the classes in this module. The reason of that is that this
 * class must be loaded by each artifact class loader that is being disposed. So, it cannot contain any of the prefixes that force
 * a class to be loaded from the container.
 *
 * @since 4.3, 4.2.3
 */
public class ScalaClassValueReleaser implements ResourceReleaser {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScalaClassValueReleaser.class);

  private final Field classValueMapField;

  public ScalaClassValueReleaser() {
    Field classValueMapField = null;
    try {
      classValueMapField = Class.class.getDeclaredField("classValueMap");
    } catch (Throwable t) {
      LOGGER.warn("Unable to initialize ScalaClassValueReleaser", t);
    }

    this.classValueMapField = classValueMapField;
  }

  @Override
  public void release() {
    try {
      removeClassValueMap(String.class);
      removeClassValueMap(Object.class);
    } catch (Throwable t) {
      LOGGER.warn("Unable to clean Scala's ClassValues", t);
    }
  }

  private void removeClassValueMap(Class cls)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
    WeakHashMap classValueMap = getClassValueMap(cls);

    if (classValueMap != null) {
      Object[] cache = getCache(classValueMap);

      for (int i = 0; i < cache.length; i++) {
        Object object = cache[i];

        if (object != null) {
          final Object value = resolveValue(object);

          LOGGER.trace("Checking class value entry '{}' from '{}'...", value.getClass(), value.getClass().getClassLoader());

          if (value.getClass().getClassLoader() instanceof MuleDeployableArtifactClassLoader
              || (value.getClass().getClassLoader() != null
                  && value.getClass().getClassLoader().getParent() instanceof RegionClassLoader)) {
            LOGGER.debug("Removing class value entry '{}' from '{}'", value.getClass(), value.getClass().getClassLoader());

            Object key = null;
            final Set<Entry> entrySet = classValueMap.entrySet();
            for (Entry entry : entrySet) {
              if (resolveValue(entry.getValue()) == value) {
                key = entry.getKey();
                break;
              }
            }

            classValueMap.remove(key);
            cache[i] = null;
          } else {
            LOGGER.trace("NOT Removing class value entry '{}' from '{}'", value.getClass(), value.getClass().getClassLoader());
          }
        }
      }
    }
  }

  private WeakHashMap getClassValueMap(Class cls) throws IllegalAccessException {
    if (classValueMapField == null) {
      return null;
    }

    classValueMapField.setAccessible(true);
    try {
      return (WeakHashMap) classValueMapField.get(cls);
    } finally {
      classValueMapField.setAccessible(false);
    }
  }

  private Object[] getCache(WeakHashMap classValueMap)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method getCacheMethod = null;
    getCacheMethod = classValueMap.getClass().getDeclaredMethod("getCache");
    if (getCacheMethod != null) {
      getCacheMethod.setAccessible(true);
      try {
        return (Object[]) getCacheMethod.invoke(classValueMap);
      } finally {
        getCacheMethod.setAccessible(false);
      }
    } else {
      return new Object[] {};
    }
  }

  private Object resolveValue(Object object) throws NoSuchFieldException, IllegalAccessException {
    final Class<? extends Object> classValueEntryClass = object.getClass();
    final Field classValueEntryValueField = classValueEntryClass.getDeclaredField("value");

    classValueEntryValueField.setAccessible(true);
    try {
      return classValueEntryValueField.get(object);
    } finally {
      classValueEntryValueField.setAccessible(false);
    }
  }

}
