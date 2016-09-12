/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.i18n;

import org.mule.runtime.core.config.i18n.I18nMessage;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;

public class DbMessages extends I18nMessageFactory {

  private static final DbMessages factory = new DbMessages();

  private static final String BUNDLE_PATH = getBundlePath("db");

  public static I18nMessage transactionSetAutoCommitFailed() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }
}
