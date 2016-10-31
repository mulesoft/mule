/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import java.net.URI;

public class HttpMessages extends I18nMessageFactory {

  private static final HttpMessages factory = new HttpMessages();

  private static final String BUNDLE_PATH = getBundlePath("http");

  public static I18nMessage requestFailedWithStatus(String string) {
    return factory.createMessage(BUNDLE_PATH, 3, string);
  }

  public static I18nMessage unableToGetEndpointUri(String requestURI) {
    return factory.createMessage(BUNDLE_PATH, 4, requestURI);
  }

  public static I18nMessage receiverPropertyNotSet() {
    return factory.createMessage(BUNDLE_PATH, 7);
  }

  public static I18nMessage httpParameterNotSet(String string) {
    return factory.createMessage(BUNDLE_PATH, 8, string);
  }

  public static I18nMessage malformedSyntax() {
    return factory.createMessage(BUNDLE_PATH, 11);
  }

  public static I18nMessage methodNotAllowed(String method) {
    return factory.createMessage(BUNDLE_PATH, 12, method);
  }

  public static I18nMessage failedToConnect(URI uri) {
    return factory.createMessage(BUNDLE_PATH, 13, uri);
  }

  public static I18nMessage cannotBindToAddress(String path) {
    return factory.createMessage(BUNDLE_PATH, 14, path);
  }

  public static I18nMessage eventPropertyNotSetCannotProcessRequest(String property) {
    return factory.createMessage(BUNDLE_PATH, 15, property);
  }

  public static I18nMessage unsupportedMethod(String method) {
    return factory.createMessage(BUNDLE_PATH, 16, method);
  }

  public static I18nMessage couldNotSendExpect100() {
    return factory.createMessage(BUNDLE_PATH, 17);
  }

  public static I18nMessage requestLineIsMalformed(String line) {
    return factory.createMessage(BUNDLE_PATH, 18, line);
  }

  public static I18nMessage pollingReciverCannotbeUsed() {
    return factory.createMessage(BUNDLE_PATH, 19);
  }

  public static I18nMessage sslHandshakeDidNotComplete() {
    return factory.createMessage(BUNDLE_PATH, 20);
  }

  public static I18nMessage customHeaderMapDeprecated() {
    return factory.createMessage(BUNDLE_PATH, 21);
  }

  public static I18nMessage basicFilterCannotHandleHeader(String header) {
    return factory.createMessage(BUNDLE_PATH, 22, header);
  }

  public static I18nMessage authRealmMustBeSetOnFilter() {
    return factory.createMessage(BUNDLE_PATH, 23);
  }

  public static I18nMessage noResourceBaseDefined() {
    return factory.createMessage(BUNDLE_PATH, 24);
  }

  public static I18nMessage fileNotFound(String file) {
    return factory.createMessage(BUNDLE_PATH, 25, file);
  }

  public static I18nMessage noReceiverFoundForUrl(String url) {
    return factory.createMessage(BUNDLE_PATH, 26, url);
  }

}
