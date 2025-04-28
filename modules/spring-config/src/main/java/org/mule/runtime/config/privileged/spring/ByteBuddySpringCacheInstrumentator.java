/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.privileged.spring;

import static net.bytebuddy.asm.Advice.to;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.pool.TypePool.Default.of;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.WEAK;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.ClassFileLocator.Compound;
import net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import org.slf4j.Logger;
import org.springframework.core.SpringVersion;

/**
 * This class offers instrumentation alternatives for Spring {@link org.springframework.util.ConcurrentReferenceHashMap} caches
 * cleanup. Spring caches of the type {@link org.springframework.util.ConcurrentReferenceHashMap} are scattered all over the
 * Spring framework code. Those caches are never explicitly cleaned up (the cleanup relies on the values being soft referenced)
 * and can cause mule artifact classloaders retention.
 */
public class ByteBuddySpringCacheInstrumentator {

  private static final Logger LOGGER = getLogger(ByteBuddySpringCacheInstrumentator.class);
  private static final String CONCURRENT_REFERENCE_HASH_MAP_CLASS = "org.springframework.util.ConcurrentReferenceHashMap";
  private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();
  public static final ClassFileLocator BOOT_LOADER = ForClassLoader.ofBootLoader();

  /**
   * Instrument Spring ConcurrentReferenceHashMap cache instances in order to register them into a
   * {@link ByteBuddySpringCachesManager} that offers a cache cleanup feature.
   *
   * @see ByteBuddySpringCachesManager#clearCaches()
   */
  public static void instrumentSpringCachesForCleanup() {
    try {
      ClassLoader targetClassloader = SpringVersion.class.getClassLoader();
      ClassFileLocator classFileLocator = new Compound(ForClassLoader.of(targetClassloader),
                                                       BOOT_LOADER);
      TypePool targetTypePool = of(classFileLocator);
      try {
        targetClassloader.loadClass(ByteBuddySpringCachesManager.class.getName());
      } catch (ClassNotFoundException e) {
        try (Unloaded<?> unloaded = BYTE_BUDDY
            .decorate(ByteBuddySpringCachesManager.class)
            .make()) {
          unloaded.load(targetClassloader, INJECTION);
        }
      }
      try (Unloaded<?> unloaded = BYTE_BUDDY
          .rebase(targetTypePool.describe(CONCURRENT_REFERENCE_HASH_MAP_CLASS).resolve(),
                  classFileLocator)
          .visit(to(ByteBuddySpringCachesManager.class).on(isConstructor()))
          .make()) {
        unloaded.load(targetClassloader, INJECTION);
      }
      LOGGER.debug("Spring caches are now instrumented for cleanup.");
    } catch (Exception e) {
      LOGGER.error("Could not instrument Spring caches for cleanup.", e);
    }
  }

  /**
   * Instrument Spring ConcurrentReferenceHashMap cache instances so that they are weakly (and not softly) referenced by default.
   */
  public static void instrumentSpringCachesForWeakReferences() {
    ClassLoader targetClassloader = SpringVersion.class.getClassLoader();
    TypePool typePool = of(targetClassloader);
    try (Unloaded<?> unloaded = BYTE_BUDDY
        .redefine(typePool.describe(CONCURRENT_REFERENCE_HASH_MAP_CLASS).resolve(),
                  ForClassLoader.of(targetClassloader))
        // We redirect any other constructor to the fully parameterized one.
        .constructor(target -> target.isConstructor() && target.getParameters().size() < 4)
        .intercept(MethodCall
            .invoke((ElementMatcher<MethodDescription>) target -> target.isConstructor() && target.getParameters().size() == 4)
            // We use default arguments plus WEAK reference type, discarding the intercepted ones (an improvement would be to use
            // the original ones).
            .with(16, 0.75F, 16, WEAK))
        .make()) {
      unloaded.load(targetClassloader, INJECTION);
      LOGGER.debug("Spring caches are now instrumented for using weak keys.");
    } catch (Exception e) {
      LOGGER.error("Could not instrument Spring caches for using weak keys.", e);
    }
  }

}
