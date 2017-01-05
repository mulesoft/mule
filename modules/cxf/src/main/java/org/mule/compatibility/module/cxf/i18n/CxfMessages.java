/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import java.util.List;

import javax.xml.namespace.QName;

public class CxfMessages extends I18nMessageFactory {

  private static final CxfMessages factory = new CxfMessages();

  private static final String BUNDLE_PATH = getBundlePath("cxf");

  public static I18nMessage serviceIsNull(String serviceName) {
    return factory.createMessage(BUNDLE_PATH, 8, serviceName);
  }

  public static I18nMessage annotationsRequireJava5() {
    return factory.createMessage(BUNDLE_PATH, 9);
  }

  public static I18nMessage couldNotInitAnnotationProcessor(Object object) {
    return factory.createMessage(BUNDLE_PATH, 10, object);
  }

  public static I18nMessage unableToInitBindingProvider(String bindingProvider) {
    return factory.createMessage(BUNDLE_PATH, 11, bindingProvider);
  }

  public static I18nMessage unableToLoadServiceClass(String classname) {
    return factory.createMessage(BUNDLE_PATH, 12, classname);
  }

  public static I18nMessage unableToConstructAdapterForNullMessage() {
    return factory.createMessage(BUNDLE_PATH, 13);
  }

  public static I18nMessage inappropriateMessageTypeForAttachments(org.apache.cxf.message.Message message) {
    String className = message.getClass().getName();
    return factory.createMessage(BUNDLE_PATH, 14, className);
  }

  public static I18nMessage bothServiceClassAndWsdlUrlAreRequired() {
    return factory.createMessage(BUNDLE_PATH, 15);
  }

  public static I18nMessage incorrectlyFormattedEndpointUri(String uri) {
    return factory.createMessage(BUNDLE_PATH, 16, uri);
  }

  public static I18nMessage invalidFrontend(String frontend) {
    return factory.createMessage(BUNDLE_PATH, 17, frontend);
  }

  public static I18nMessage portNotFound(String port) {
    return factory.createMessage(BUNDLE_PATH, 18, port);
  }

  public static I18nMessage mustSpecifyPort() {
    return factory.createMessage(BUNDLE_PATH, 19);
  }

  public static I18nMessage wsdlNotFound(String loc) {
    return factory.createMessage(BUNDLE_PATH, 20, loc);
  }

  public static I18nMessage noOperationWasFoundOrSpecified() {
    return factory.createMessage(BUNDLE_PATH, 21);
  }

  public static I18nMessage javaComponentRequiredForInboundEndpoint() {
    return factory.createMessage(BUNDLE_PATH, 22);
  }

  public static I18nMessage serviceClassRequiredWithPassThrough() {
    return factory.createMessage(BUNDLE_PATH, 23);
  }

  public static I18nMessage invalidPayloadToArgumentsParameter(String nullPayloadParameterValue) {
    return factory.createMessage(BUNDLE_PATH, 24, nullPayloadParameterValue);
  }

  public static I18nMessage invalidOrMissingNamespace(QName serviceQName, List<QName> probableServices, List<QName> allServices) {
    return factory.createMessage(BUNDLE_PATH, 25, String.valueOf(serviceQName), String.valueOf(probableServices),
                                 String.valueOf(allServices));
  }

  public static I18nMessage onlyServiceOrClientClassIsValid() {
    return factory.createMessage(BUNDLE_PATH, 26);
  }

  public static I18nMessage couldNotFindEndpoint(QName endpointNameThatCannotBeFound, List<QName> availableEndpoingNames) {
    return factory.createMessage(BUNDLE_PATH, 27, String.valueOf(endpointNameThatCannotBeFound),
                                 String.valueOf(availableEndpoingNames));
  }
}


