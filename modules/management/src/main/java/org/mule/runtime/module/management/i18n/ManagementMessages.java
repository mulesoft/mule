/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class ManagementMessages extends I18nMessageFactory {

  private static final ManagementMessages factory = new ManagementMessages();

  private static final String BUNDLE_PATH = getBundlePath("management");

  public static I18nMessage createOrLocateShouldBeSet() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }

  public static I18nMessage cannotLocateOrCreateServer() {
    return factory.createMessage(BUNDLE_PATH, 2);
  }

  public static I18nMessage noMBeanServerAvailable() {
    return factory.createMessage(BUNDLE_PATH, 3);
  }

  public static I18nMessage forceGC(long[] heapSizes) {
    return factory.createMessage(BUNDLE_PATH, 4, String.valueOf(heapSizes[0]), String.valueOf(heapSizes[1]));
  }


}


