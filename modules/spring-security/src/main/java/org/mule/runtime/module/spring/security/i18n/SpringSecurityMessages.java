/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class SpringSecurityMessages extends I18nMessageFactory {

  private static final SpringSecurityMessages factory = new SpringSecurityMessages();

  private static final String BUNDLE_PATH = getBundlePath("spring-security");

  public static I18nMessage basicFilterCannotHandleHeader(String header) {
    return factory.createMessage(BUNDLE_PATH, 1, header);
  }

  public static I18nMessage authRealmMustBeSetOnFilter() {
    return factory.createMessage(BUNDLE_PATH, 2);
  }

  public static I18nMessage noGrantedAuthority(String authority) {
    return factory.createMessage(BUNDLE_PATH, 3, authority);
  }

  public static I18nMessage springAuthenticationRequired() {
    return factory.createMessage(BUNDLE_PATH, 4);
  }
}


