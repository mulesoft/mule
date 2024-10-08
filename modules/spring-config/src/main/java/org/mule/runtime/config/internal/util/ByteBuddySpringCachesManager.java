/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.springframework.core.SpringVersion;

public class ByteBuddySpringCachesManager {

  private static final Logger LOGGER = getLogger(ByteBuddySpringCachesManager.class);
  public static final Cache<Object, Optional<?>> springCaches = Caffeine.newBuilder().weakKeys().build();

  public static void clearCaches()
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    SpringVersion.class.getClassLoader().loadClass(ByteBuddySpringCachesManager.class.getName()).getMethod("doClearCaches")
        .invoke(null);
  }

  public static void doClearCaches() {
    springCaches.cleanUp();
    LOGGER.debug("Cleaning up {} Spring caches", springCaches.estimatedSize());
    springCaches.asMap().keySet().forEach(ByteBuddySpringCachesManager::clearCache);
  }

  private static void clearCache(Object springCache) {
    ((Map<?, ?>) springCache).clear();
  }

  @Advice.OnMethodExit(inline = false)
  public static void onMethodExit(@Advice.This Object springCache) {
    LOGGER.debug("Spring cache intercepted - Hash[{}]", springCache.hashCode());
    springCaches.put(springCache, empty());
  }

}
