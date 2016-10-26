/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

/**
 * Internationalised messages for the Json module
 */
public class JsonMessages extends I18nMessageFactory {

  private static final JsonMessages factory = new JsonMessages();

  private static final String BUNDLE_PATH = getBundlePath("json");

  public static I18nMessage messageStringIsNotJson() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }
}
