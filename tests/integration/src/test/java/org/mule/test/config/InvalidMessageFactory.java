/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class InvalidMessageFactory extends I18nMessageFactory {

  private static final InvalidMessageFactory factory = new InvalidMessageFactory();

  private static final String BUNDLE_PATH = getBundlePath("thisdoesnotexist");

  public static I18nMessage getInvalidMessage() {
    // the code can safely be ignored. MessageFactory must fail before when
    // trying to find the inexistent bundle.
    return factory.createMessage(BUNDLE_PATH, 42);
  }
}


