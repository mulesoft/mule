/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class SpringMessages extends I18nMessageFactory {

  private static final SpringMessages factory = new SpringMessages();

  private static final String BUNDLE_PATH = getBundlePath("spring");

  public static I18nMessage failedToReinitMule() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }

  public static I18nMessage beanNotInstanceOfApplicationListener(String name) {
    return factory.createMessage(BUNDLE_PATH, 12, name);
  }
}


