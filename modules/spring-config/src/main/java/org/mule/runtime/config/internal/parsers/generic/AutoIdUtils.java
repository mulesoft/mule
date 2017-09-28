/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.parsers.generic;

import org.mule.runtime.core.api.util.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

public class AutoIdUtils {

  public static final String ATTRIBUTE_ID = "id";
  public static final String ATTRIBUTE_NAME = "name";
  private static final AtomicInteger counter = new AtomicInteger(0);
  public static final String PREFIX = "org.mule.autogen.";

  public static boolean blankAttribute(Element element, String attribute) {
    return StringUtils.isBlank(element.getAttribute(attribute));
  }

  public static void ensureUniqueId(Element element, String type) {
    if (null != element && blankAttribute(element, ATTRIBUTE_ID)) {
      if (blankAttribute(element, ATTRIBUTE_NAME)) {
        element.setAttribute(ATTRIBUTE_ID, uniqueValue(PREFIX + type));
      } else {
        element.setAttribute(ATTRIBUTE_ID, element.getAttribute(ATTRIBUTE_NAME));
      }
    }
  }

  public static String getUniqueName(Element element, String type) {
    if (!blankAttribute(element, ATTRIBUTE_NAME)) {
      return element.getAttribute(ATTRIBUTE_NAME);
    } else if (!blankAttribute(element, ATTRIBUTE_ID)) {
      return element.getAttribute(ATTRIBUTE_ID);
    } else {
      return uniqueValue(PREFIX + type);
    }
  }

  public static String uniqueValue(String value) {
    return value + "." + counter.incrementAndGet();
  }

  public static void forceUniqueId(Element element, String type) {
    if (null != element) {
      String id = uniqueValue(PREFIX + type);
      element.setAttribute(ATTRIBUTE_ID, id);
      element.setAttribute(ATTRIBUTE_NAME, id);
    }
  }

}
