/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class PGPMessages extends I18nMessageFactory {

  private static final PGPMessages factory = new PGPMessages();

  private static final String BUNDLE_PATH = getBundlePath("pgp");

  public static I18nMessage noPublicKeyForUser(String userId) {
    return factory.createMessage(BUNDLE_PATH, 1, userId);
  }

  public static I18nMessage noSignedMessageFound() {
    return factory.createMessage(BUNDLE_PATH, 2);
  }

  public static I18nMessage invalidSignature() {
    return factory.createMessage(BUNDLE_PATH, 3);
  }

  public static I18nMessage errorVerifySignature() {
    return factory.createMessage(BUNDLE_PATH, 4);
  }

  public static I18nMessage encryptionStrategyNotSet() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }

  public static I18nMessage pgpPublicKeyExpired() {
    return factory.createMessage(BUNDLE_PATH, 6);
  }

  public static I18nMessage noSecretKeyFoundButAvailable(String availableKeys) {
    return factory.createMessage(BUNDLE_PATH, 7, availableKeys);
  }

}


