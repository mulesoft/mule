/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.spring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.springframework.core.SpringVersion;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <b>NOTE: Included because of a shading of the spring-core module.</b>
 * <p>
 * ByteBuddy aspect implementation that stores Spring {@link org.springframework.util.ConcurrentReferenceHashMap} caches, offering
 * a cleanup method. The use the cleanup feature, this aspect must be instrumented through
 * {@link ByteBuddySpringCacheInstrumentator#instrumentForCleanup(ClassLoader)} call.
 *
 * @see ByteBuddySpringCacheInstrumentator#instrumentForCleanup(ClassLoader)
 */
public class ByteBuddySpringCachesManager {

  private static final Logger LOGGER = getLogger(ByteBuddySpringCachesManager.class);
  public static final Cache<Object, Optional<?>> springCaches = Caffeine.newBuilder().weakKeys().build();

  /**
   * Clears all the registered Spring {@link org.springframework.util.ConcurrentReferenceHashMap} caches.
   *
   * @throws Exception When the cleanup could not be performed.
   */
  public static void clearCaches()
      throws Exception {
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

  /**
   * ByteBuddy aspect that will intercept a {@link org.springframework.util.ConcurrentReferenceHashMap} while it's being
   * constructed and register it at this {@link ByteBuddySpringCachesManager}
   *
   * @param springCache The intercepted {@link org.springframework.util.ConcurrentReferenceHashMap}
   */
  @Advice.OnMethodExit(inline = false)
  public static void onMethodExit(@Advice.This Object springCache) {
    LOGGER.debug("Spring cache intercepted - Hash[{}]", springCache.hashCode());
    springCaches.put(springCache, empty());
  }

}
