/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import java.util.function.BiFunction;

/**
 * @since 4.4.0
 */
public class EntityResolverUtils {

  public static final String CORE_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  public static final String CORE_CURRENT_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core.xsd";
  public static final String CORE_DEPRECATED_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core-deprecated.xsd";
  public static final String COMPATIBILITY_XSD =
      "http://www.mulesoft.org/schema/mule/compatibility/current/mule-compatibility.xsd";

  private EntityResolverUtils() {}

  public static String resolveSystemIdForCompatibility(String publicId, String systemId,
                                                       BiFunction<String, String, Boolean> canResolveEntity) {
    return resolveSystemIdForCompatibility(publicId, systemId, false, canResolveEntity);
  }

  public static String resolveSystemIdForCompatibility(String publicId, String systemId, Boolean runningTests,
                                                       BiFunction<String, String, Boolean> canResolveEntity) {
    if (systemId.equals(CORE_XSD)) {
      Boolean useDeprecated = canResolveEntity.apply(publicId, CORE_DEPRECATED_XSD);
      Boolean usingCompatibility = canResolveEntity.apply(publicId, COMPATIBILITY_XSD);

      if (useDeprecated && (usingCompatibility || runningTests)) {
        return CORE_DEPRECATED_XSD;
      } else {
        return CORE_CURRENT_XSD;
      }
    } else if (systemId.contains("spring")) {
      systemId = systemId.replace("-current.xsd", ".xsd");
    }

    return systemId;
  }
}
