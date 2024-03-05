/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToConvertStringUsingEncoding;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Useful methods for formatting message strings for logging or exceptions.
 */
// @ThreadSafe
public final class StringMessageUtils {

  // The maximum number of Collection and Array elements used for messages
  public static final int MAX_ELEMENTS = 50;
  public static final int DEFAULT_MESSAGE_WIDTH = 80;

  /** Do not instanciate. */
  private StringMessageUtils() {
    // no-op
  }

  public static String getBoilerPlate(String message) {
    return getBoilerPlate(message, '*', DEFAULT_MESSAGE_WIDTH);
  }

  public static String getBoilerPlate(String message, char c, int maxlength) {
    List<String> messages = Arrays.asList(new String[] {message});
    messages = new ArrayList<>(messages);
    return getBoilerPlate(messages, c, maxlength);
  }

  public static String getBoilerPlate(List<String> messages, char c, int maxlength) {
    int size;
    StringBuilder buf = new StringBuilder(messages.size() * maxlength);
    int trimLength = maxlength - (c == ' ' ? 2 : 4);

    messages = messages.stream()
        .map(string -> string.split(lineSeparator()))
        .flatMap(Arrays::stream)
        .collect(Collectors.toList());

    for (int i = 0; i < messages.size(); i++) {
      size = messages.get(i).toString().length();
      if (size > trimLength) {
        String temp = messages.get(i).toString();
        int k = i;
        int x;
        int len;
        messages.remove(i);
        while (temp.length() > 0) {
          len = (trimLength <= temp.length() ? trimLength : temp.length());
          String msg = temp.substring(0, len);
          x = msg.indexOf(lineSeparator());

          if (x > -1) {
            msg = msg.substring(0, x);
            len = x + 1;
          } else {
            x = msg.lastIndexOf(' ');
            if (x > -1 && len == trimLength) {
              msg = msg.substring(0, x);
              len = x + 1;
            }
          }
          if (msg.startsWith(" ")) {
            msg = msg.substring(1);
          }

          temp = temp.substring(len);
          messages.add(k, msg);
          k++;
        }
      }
    }

    buf.append(lineSeparator());
    if (c != ' ') {
      buf.append(StringUtils.repeat(c, maxlength));
    }

    for (String message : messages) {
      buf.append(lineSeparator());
      if (c != ' ') {
        buf.append(c);
      }
      buf.append(" ");
      buf.append(message);

      String osEncoding = Charset.defaultCharset().name();
      int padding;
      try {
        padding = trimLength - message.toString().getBytes(osEncoding).length;
      } catch (UnsupportedEncodingException ueex) {
        throw new MuleRuntimeException(failedToConvertStringUsingEncoding(osEncoding), ueex);
      }
      if (padding > 0) {
        buf.append(StringUtils.repeat(' ', padding));
      }
      buf.append(' ');
      if (c != ' ') {
        buf.append(c);
      }
    }
    buf.append(lineSeparator());
    if (c != ' ') {
      buf.append(StringUtils.repeat(c, maxlength));
    }
    return buf.toString();
  }

  public static String truncate(String message, int length, boolean includeCount) {
    if (message == null) {
      return null;
    }
    if (message.length() <= length) {
      return message;
    }
    String result = message.substring(0, length) + "...";
    if (includeCount) {
      result += "[" + length + " of " + message.length() + "]";
    }
    return result;
  }

  public static byte[] getBytes(String string) {
    try {
      return string.getBytes(FileUtils.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      // We can ignore this as the encoding is validated on start up
      return null;
    }
  }

  public static String getString(byte[] bytes, String encoding) {
    try {
      return new String(bytes, encoding);
    } catch (UnsupportedEncodingException e) {
      // We can ignore this as the encoding is validated on start up
      return null;
    }
  }

  /**
   * @see ArrayUtils#toString(Object, int)
   * @see CollectionUtils#toString(Collection, int)
   */
  public static String toString(Object o) {
    if (o == null) {
      return "null";
    } else if (o instanceof Class<?>) {
      return ((Class<?>) o).getName();
    } else if (o instanceof Map) {
      return o.toString();
    } else if (o.getClass().isArray()) {
      return ArrayUtils.toString(o);
    } else if (o instanceof Collection) {
      return collectionToString((Collection<?>) o, MAX_ELEMENTS, false);
    } else {
      return o.toString();
    }
  }

  /**
   * Creates a String representation of the given Collection, with optional newlines between elements. Class objects are
   * represented by their full names. Considers at most <code>maxElements</code> values; overflow is indicated by an appended
   * "[..]" ellipsis.
   *
   * @param c           the Collection to format
   * @param maxElements the maximum number of elements to take into account
   * @param newline     indicates whether elements are to be split across lines
   * @return the formatted String
   */
  private static String collectionToString(Collection c, int maxElements, boolean newline) {
    if (c == null || c.isEmpty()) {
      return "[]";
    }

    int origNumElements = c.size();
    int numElements = Math.min(origNumElements, maxElements);
    boolean tooManyElements = (origNumElements > maxElements);

    StringBuilder buf = new StringBuilder(numElements * 32);
    buf.append('[');

    if (newline) {
      buf.append(lineSeparator());
    }

    Iterator items = c.iterator();
    for (int i = 0; i < numElements - 1; i++) {
      Object item = items.next();

      if (item instanceof Class) {
        buf.append(((Class) item).getName());
      } else {
        buf.append(item);
      }

      if (newline) {
        buf.append(lineSeparator());
      } else {
        buf.append(',').append(' ');
      }
    }

    // don't forget the last one
    Object lastItem = items.next();
    if (lastItem instanceof Class) {
      buf.append(((Class) lastItem).getName());
    } else {
      buf.append(lastItem);
    }

    if (newline) {
      buf.append(lineSeparator());
    }

    if (tooManyElements) {
      buf.append(" [..]");
    }

    buf.append(']');
    return buf.toString();
  }
}
