/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static org.slf4j.LoggerFactory.getLogger;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.mule.runtime.config.internal.context.BaseMuleArtifactContext;
import org.slf4j.Logger;
import org.springframework.core.SpringVersion;

public class ByteBuddySpringCacheInstrumentator {

  private static final Logger LOGGER = getLogger(ByteBuddySpringCacheInstrumentator.class);

  public static void instrument() {
    try {
      TypePool springTypePool = TypePool.Default.of(SpringVersion.class.getClassLoader());
      TypePool muleTypePool = TypePool.Default.of(BaseMuleArtifactContext.class.getClassLoader());
      ByteBuddy byteBuddy = new ByteBuddy();
      DynamicType byteBuddySpringCachesManager;
      try (DynamicType.Unloaded<?> unloaded = byteBuddy
          .redefine(muleTypePool.describe("org.mule.runtime.config.internal.util.ByteBuddySpringCachesManager").resolve(),
                    ClassFileLocator.ForClassLoader.of(BaseMuleArtifactContext.class.getClassLoader()))
          .make()) {
        byteBuddySpringCachesManager =
            unloaded.load(SpringVersion.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION);
      }
      try (DynamicType.Unloaded<?> unloaded = byteBuddy
          .rebase(springTypePool.describe("org.springframework.util.ConcurrentReferenceHashMap").resolve(),
                  ClassFileLocator.ForClassLoader.of(SpringVersion.class.getClassLoader()))
          .visit(Advice.to(byteBuddySpringCachesManager.getTypeDescription()).on(ElementMatchers.isConstructor()))
          .make()) {
        unloaded.load(SpringVersion.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION);
      }
      LOGGER.debug("Spring caches are now instrumented.");
    } catch (Exception e) {
      LOGGER.error("Could not instrument Spring caches cleanup.", e);
    }
  }

}
