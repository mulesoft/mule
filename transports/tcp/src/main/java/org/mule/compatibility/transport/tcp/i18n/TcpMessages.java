/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.i18n;

import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.compatibility.transport.tcp.TcpPropertyHelper;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import java.net.URI;

public class TcpMessages extends I18nMessageFactory {

  private static final TcpMessages factory = new TcpMessages();

  private static final String BUNDLE_PATH = getBundlePath(TcpConnector.TCP);

  public static I18nMessage failedToBindToUri(URI uri) {
    return factory.createMessage(BUNDLE_PATH, 1, uri);
  }

  public static I18nMessage failedToCloseSocket() {
    return factory.createMessage(BUNDLE_PATH, 2);
  }

  public static I18nMessage failedToInitMessageReader() {
    return factory.createMessage(BUNDLE_PATH, 3);
  }

  public static I18nMessage invalidStreamingOutputType(Class c) {
    return factory.createMessage(BUNDLE_PATH, 4, c.getName());
  }

  public static I18nMessage pollingReceiverCannotbeUsed() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }

  public static I18nMessage localhostBoundToAllLocalInterfaces() {
    return factory.createMessage(BUNDLE_PATH, 6, TcpPropertyHelper.MULE_TCP_BIND_LOCALHOST_TO_ALL_LOCAL_INTERFACES_PROPERTY);
  }
}


