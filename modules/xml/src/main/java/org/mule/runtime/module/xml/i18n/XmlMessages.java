/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class XmlMessages extends I18nMessageFactory {

  private static final XmlMessages factory = new XmlMessages();

  private static final String BUNDLE_PATH = getBundlePath("xml");

  public static I18nMessage failedToProcessXPath(String expression) {
    return factory.createMessage(BUNDLE_PATH, 1, expression);
  }

  public static I18nMessage domTypeNotSupported(Class type) {
    return factory.createMessage(BUNDLE_PATH, 2, type);
  }

  public static I18nMessage invalidReturnTypeForTransformer(Class resultCls) {
    return factory.createMessage(BUNDLE_PATH, 3, resultCls.getName());
  }

  public static I18nMessage failedToRegisterNamespace(String prefix, String uri) {
    return factory.createMessage(BUNDLE_PATH, 4, prefix, uri);
  }

  public static I18nMessage failedToCreateDocumentBuilder() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }

  public static I18nMessage streamNotAvailble(String transformerName) {
    return factory.createMessage(BUNDLE_PATH, 6, transformerName);
  }

  public static I18nMessage objectNotAvailble(String transformerName) {
    return factory.createMessage(BUNDLE_PATH, 7, transformerName);
  }

  public static I18nMessage converterClassDoesNotImplementInterface(Class converter) {
    return factory.createMessage(BUNDLE_PATH, 8, converter);
  }

  public static I18nMessage canOnlySetFileOrXslt() {
    return factory.createMessage(BUNDLE_PATH, 9);
  }

  public static I18nMessage canOnlySetFileOrXQuery() {
    return factory.createMessage(BUNDLE_PATH, 10);
  }

  public static I18nMessage xpathResultTypeNotSupported(Class<?> paramType) {
    return factory.createMessage(BUNDLE_PATH, 11, paramType);
  }

  public static I18nMessage contextPropertyValueIsNull(String propertyKey) {
    return factory.createMessage(BUNDLE_PATH, 12, propertyKey);
  }

}
