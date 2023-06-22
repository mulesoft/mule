package org.mule.runtime.config.internal.util;

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.WEAK;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.springframework.core.ResolvableType;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Set of utility operations to get insights about objects and their components
 *
 * @since 3.7.0
 */
public final class IntrospectionUtils {

  private static final Logger LOGGER = getLogger(IntrospectionUtils.class);

  static {
    setWeakHashCaches();
  }

  private IntrospectionUtils() {}

  /**
   * Set caches in spring so that they are weakly (and not softly) referenced by default.
   * <p>
   * For example, {@link ResolvableType} or {@link CachedIntrospectionResults} may retain classloaders when introspection is used.
   */
  private static void setWeakHashCaches() {
    try {
      Field field = FieldUtils.getField(ConcurrentReferenceHashMap.class, "DEFAULT_REFERENCE_TYPE", true);
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
      field.set(null, WEAK);
    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Unable to set spring concurrent maps to WEAK default scopes.", e);
      }
    }
  }

}
