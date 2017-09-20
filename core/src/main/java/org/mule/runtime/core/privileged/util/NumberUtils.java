/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * <code>NumberUtils</code> contains useful methods for manipulating numbers.
 */
// @ThreadSafe
public class NumberUtils {

  public static final int INTEGER_ERROR = -999999999;

  public static int toInt(Object obj) {
    if (obj == null) {
      throw new IllegalArgumentException("Unable to convert null object to int");
    } else if (obj instanceof String) {
      return toInt((String) obj);
    } else if (obj instanceof Number) {
      return ((Number) obj).intValue();
    } else {
      throw new IllegalArgumentException("Unable to convert object of type: " + obj.getClass().getName() + " to int.");
    }
  }

  private static int toInt(String str) {
    return org.apache.commons.lang3.math.NumberUtils.toInt(str, INTEGER_ERROR);
  }

  /*
   * The following methods are from org.springframework.util.NumberUtils
   */

  @SuppressWarnings("unchecked")
  public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
    text = text.trim();

    if (targetClass.equals(Byte.class)) {
      return (T) Byte.valueOf(text);
    } else if (targetClass.equals(Short.class)) {
      return (T) Short.valueOf(text);
    } else if (targetClass.equals(Integer.class)) {
      return (T) Integer.valueOf(text);
    } else if (targetClass.equals(Long.class)) {
      return (T) Long.valueOf(text);
    } else if (targetClass.equals(BigInteger.class)) {
      return (T) new BigInteger(text);
    } else if (targetClass.equals(Float.class)) {
      return (T) Float.valueOf(text);
    } else if (targetClass.equals(Double.class)) {
      return (T) Double.valueOf(text);
    } else if (targetClass.equals(BigDecimal.class) || targetClass.equals(Number.class)) {
      return (T) new BigDecimal(text);
    } else {
      throw new IllegalArgumentException("Cannot convert String [" + text + "] to target class [" + targetClass.getName() + "]");
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends Number> T convertNumberToTargetClass(Number number, Class<T> targetClass)
      throws IllegalArgumentException {

    if (targetClass.isInstance(number)) {
      return (T) number;
    } else if (targetClass.equals(Byte.class)) {
      long value = number.longValue();
      if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
        raiseOverflowException(number, targetClass);
      }
      return (T) new Byte(number.byteValue());
    } else if (targetClass.equals(Short.class)) {
      long value = number.longValue();
      if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
        raiseOverflowException(number, targetClass);
      }
      return (T) new Short(number.shortValue());
    } else if (targetClass.equals(Integer.class)) {
      long value = number.longValue();
      if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
        raiseOverflowException(number, targetClass);
      }
      return (T) new Integer(number.intValue());
    } else if (targetClass.equals(Long.class)) {
      return (T) new Long(number.longValue());
    } else if (targetClass.equals(BigInteger.class)) {
      if (number instanceof BigDecimal) {
        // do not lose precision - use BigDecimal's own conversion
        return (T) ((BigDecimal) number).toBigInteger();
      } else {
        // original value is not a Big* number - use standard long conversion
        return (T) BigInteger.valueOf(number.longValue());
      }
    } else if (targetClass.equals(Float.class)) {
      return (T) new Float(number.floatValue());
    } else if (targetClass.equals(Double.class)) {
      return (T) new Double(number.doubleValue());
    } else if (targetClass.equals(BigDecimal.class)) {
      // always use BigDecimal(String) here to avoid unpredictability of
      // BigDecimal(double)
      // (see BigDecimal javadoc for details)
      return (T) new BigDecimal(number.toString());
    } else {
      throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" + number.getClass().getName()
          + "] to unknown target class [" + targetClass.getName() + "]");
    }
  }

  /**
   * Raise an overflow exception for the given number and target class.
   * 
   * @param number the number we tried to convert
   * @param targetClass the target class we tried to convert to
   */
  private static void raiseOverflowException(Number number, Class targetClass) {
    throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" + number.getClass().getName()
        + "] to target class [" + targetClass.getName() + "]: overflow");
  }

}
