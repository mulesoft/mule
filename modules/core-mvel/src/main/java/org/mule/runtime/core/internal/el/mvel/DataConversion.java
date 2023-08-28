/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
