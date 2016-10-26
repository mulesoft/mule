/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.i18n;

import org.mule.compatibility.transport.vm.VMConnector;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class VMMessages extends I18nMessageFactory {

  private static final VMMessages factory = new VMMessages();

  private static final String BUNDLE_PATH = getBundlePath(VMConnector.VM);

  public static I18nMessage noReceiverForEndpoint(String name, Object uri) {
    return factory.createMessage(BUNDLE_PATH, 1, name, uri);
  }

  public static I18nMessage queueIsFull(String queueName, int maxCapacity) {
    return factory.createMessage(BUNDLE_PATH, 2, queueName, maxCapacity);
  }
}


