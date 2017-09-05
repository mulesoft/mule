/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

public class DataConversion extends org.mule.mvel2.DataConversion {

  @SuppressWarnings("unchecked")
  protected static <T> T handleTypeCoercion(Class<T> type, Object value) {
    if (type != null && value != null && value.getClass() != type) {
      if (!canConvert(type, value.getClass())) {
        throw new RuntimeException("cannot assign " + value.getClass().getName() + " to type: " + type.getName());
      }
      try {
        return convert(value, type);
      } catch (Exception e) {
        throw new RuntimeException("cannot convert value of " + value.getClass().getName() + " to: " + type.getName());
      }
    }
    return (T) value;
  }

}
