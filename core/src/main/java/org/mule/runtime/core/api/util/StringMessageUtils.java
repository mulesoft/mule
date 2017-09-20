/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.lang.System.lineSeparator;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToConvertStringUsingEncoding;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.util.ArrayUtils;
import org.mule.runtime.core.privileged.util.CollectionUtils;
import org.mule.runtime.core.privileged.util.MapUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

  public static String getFormattedMessage(String msg, Object[] arguments) {
    if (arguments != null) {
      for (int i = 0; i < arguments.length; i++) {
        arguments[i] = toString(arguments[i]);
      }
    }
    return MessageFormat.format(msg, arguments);
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

    for (int i = 0; i < messages.size(); i++) {
      buf.append(lineSeparator());
      if (c != ' ') {
        buf.append(c);
      }
      buf.append(" ");
      buf.append(messages.get(i));

      String osEncoding = Charset.defaultCharset().name();
      int padding;
      try {
        padding = trimLength - messages.get(i).toString().getBytes(osEncoding).length;
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
   * @see MapUtils#toString(Map, boolean)
   */
  public static String toString(Object o) {
    if (o == null) {
      return "null";
    } else if (o instanceof Class<?>) {
      return ((Class<?>) o).getName();
    } else if (o instanceof Map) {
      return MapUtils.toString((Map<?, ?>) o, false);
    } else if (o.getClass().isArray()) {
      return ArrayUtils.toString(o, MAX_ELEMENTS);
    } else if (o instanceof Collection) {
      return CollectionUtils.toString((Collection<?>) o, MAX_ELEMENTS);
    } else {
      return o.toString();
    }
  }
}
