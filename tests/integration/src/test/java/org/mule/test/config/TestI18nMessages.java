/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class TestI18nMessages extends I18nMessageFactory {

  private static final TestI18nMessages factory = new TestI18nMessages();

  private static final String BUNDLE_PATH = getBundlePath("test");

  public static I18nMessage testMessage(String arg1, String arg2, String arg3) {
    return factory.createMessage(BUNDLE_PATH, 1, arg1, arg2, arg3);
  }
}


