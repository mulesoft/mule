/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.spring;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.WEAK;

/**
 * This class offers instrumentation alternatives for Spring {@link org.springframework.util.ConcurrentReferenceHashMap} caches
 * cleanup. Spring caches of the type {@link org.springframework.util.ConcurrentReferenceHashMap} are scattered all over the
 * Spring framework code. Those caches are never explicitly cleaned up (the cleanup relies on the values being soft referenced)
 * and can cause mule artifact classloaders retention.
 */
public class ByteBuddySpringCacheInstrumentator {

  private static final String CONCURRENT_REFERENCE_HASH_MAP_CLASS = "org.mule.springframework.util.ConcurrentReferenceHashMap";
  private static final String BYTE_BUDDY_SPRING_CACHES_MANAGER_CLASS =
      "org.mule.runtime.module.extension.internal.spring.ByteBuddySpringCachesManager";
  private static final Logger LOGGER = getLogger(ByteBuddySpringCacheInstrumentator.class);

  /**
   * Instrument Spring ConcurrentReferenceHashMap cache instances in order to register them into a
   * {@link ByteBuddySpringCachesManager} that offers a cache cleanup feature.
   * 
   * @param targetClassloader Classloader where the Spring cache instrumentation will be injected.
   * @see ByteBuddySpringCachesManager#clearCaches()
   */
  public static void instrumentForCleanup(ClassLoader targetClassloader) {
    try {
      TypePool targetTypePool = TypePool.Default.of(targetClassloader);
      TypePool muleTypePool = TypePool.Default.of(ByteBuddySpringCacheInstrumentator.class.getClassLoader());
      ByteBuddy byteBuddy = new ByteBuddy();
      DynamicType byteBuddySpringCachesManager;
      try (DynamicType.Unloaded<?> unloaded = byteBuddy
          .redefine(muleTypePool.describe(BYTE_BUDDY_SPRING_CACHES_MANAGER_CLASS)
              .resolve(),
                    ClassFileLocator.ForClassLoader.of(ByteBuddySpringCacheInstrumentator.class.getClassLoader()))
          .make()) {
        byteBuddySpringCachesManager =
            unloaded.load(targetClassloader, ClassLoadingStrategy.Default.INJECTION);
      }
      try (DynamicType.Unloaded<?> unloaded = byteBuddy
          .rebase(targetTypePool.describe(CONCURRENT_REFERENCE_HASH_MAP_CLASS).resolve(),
                  ClassFileLocator.ForClassLoader.of(targetClassloader))
          .visit(Advice.to(byteBuddySpringCachesManager.getTypeDescription()).on(ElementMatchers.isConstructor()))
          .make()) {
        unloaded.load(targetClassloader, ClassLoadingStrategy.Default.INJECTION);
      }
      LOGGER.debug("Spring caches are now instrumented for cleanup.");
    } catch (Exception e) {
      LOGGER.error("Could not instrument Spring caches cleanup.", e);
    }
  }

  /**
   * Instrument Spring ConcurrentReferenceHashMap cache instances so that they are weakly (and not softly) referenced by default.
   * 
   * @param targetClassloader Classloader where the Spring cache instrumentation will be injected.
   */
  public static void instrumentForWeakness(ClassLoader targetClassloader) {
    TypePool typePool = TypePool.Default.of(targetClassloader);
    try (DynamicType.Unloaded<?> unloaded = new ByteBuddy()
        .redefine(typePool.describe(CONCURRENT_REFERENCE_HASH_MAP_CLASS).resolve(),
                  ClassFileLocator.ForClassLoader.of(IntrospectionUtils.class.getClassLoader()))
        // We redirect any other constructor to the fully parameterized one.
        .constructor(target -> target.isConstructor() && target.getParameters().size() < 4)
        .intercept(MethodCall
            .invoke((ElementMatcher<MethodDescription>) target -> target.isConstructor() && target.getParameters().size() == 4)
            // We use default arguments plus WEAK reference type, discarding the intercepted ones.
            .with(16, 0.75F, 16, WEAK))
        .make()) {
      unloaded.load(IntrospectionUtils.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION);
      LOGGER.debug("Spring caches are now instrumented for weakness.");
    } catch (Exception e) {
      LOGGER.error("Could not instrument Spring caches weakness.", e);
    }
  }

}
