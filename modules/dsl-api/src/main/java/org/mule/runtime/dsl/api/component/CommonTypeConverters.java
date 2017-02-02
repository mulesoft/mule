/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import static java.lang.Enum.valueOf;
import static java.lang.Thread.currentThread;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

/**
 * Set of common {@link TypeConverter}s to be reused in different {@link ComponentBuildingDefinitionProvider}s
 *
 * @since 4.0
 */
public class CommonTypeConverters {

  /**
   * @return a converter that transforms class name to a {@code Class} instance.
   */
  public static TypeConverter<String, Class> stringToClassConverter() {
    return className -> {
      try {
        return currentThread().getContextClassLoader().loadClass(className);
      } catch (ClassNotFoundException e) {
        // TODO MULE-10835 use MuleRuntimeException once it's moved to the API.
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * @return a converter that transforms a String to an Enum.
   */
  public static TypeConverter<String, Enum> stringToEnumConverter(Class<? extends Enum> enumType) {
    checkArgument(enumType != null, "enumType cannot be null");
    return enumAsString -> {
      checkArgument(enumAsString != null, "enumAsString cannot be null");
      return valueOf(enumType, enumAsString);
    };
  }

}
