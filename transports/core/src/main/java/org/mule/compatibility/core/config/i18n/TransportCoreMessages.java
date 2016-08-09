/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.config.i18n;

import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.exception.AbstractExceptionListener;
import org.mule.runtime.core.util.StringMessageUtils;

import java.util.List;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class TransportCoreMessages extends MessageFactory {

  private static final TransportCoreMessages factory = new TransportCoreMessages();

  private static final String BUNDLE_PATH = getBundlePath("transport-core");

  public static Message lifecycleErrorCannotUseConnector(String name, String lifecyclePhase) {
    return factory.createMessage(BUNDLE_PATH, 32, name, lifecyclePhase);
  }

  public static Message connectorCausedError() {
    return connectorCausedError(null);
  }

  @Deprecated
  public static Message endpointIsNullForListener() {
    return factory.createMessage(BUNDLE_PATH, 34);
  }

  public static Message listenerAlreadyRegistered(EndpointURI endpointUri) {
    return factory.createMessage(BUNDLE_PATH, 35, endpointUri);
  }

  public static Message objectAlreadyInitialised(String name) {
    return factory.createMessage(BUNDLE_PATH, 37, name);
  }

  public static Message connectorCausedError(Object connector) {
    return factory.createMessage(BUNDLE_PATH, 33, connector);
  }

  public static Message endpointIsMalformed(String endpoint) {
    return factory.createMessage(BUNDLE_PATH, 51, endpoint);
  }

  public static Message transformOnObjectUnsupportedTypeOfEndpoint(String name, ImmutableEndpoint endpoint, Class<?> class1) {
    return factory.createMessage(BUNDLE_PATH, 54, name, StringMessageUtils.toString(class1),
                                 (endpoint != null ? endpoint.getEndpointURI() : null));
  }

  public static Message messageNotSupportedByMuleMessageFactory(Object message, Class<?> creator) {
    String messageClass = (message != null ? message.getClass().getName() : "null");
    String creatorClass = (creator != null ? creator.getName() : "null class");

    return factory.createMessage(BUNDLE_PATH, 59, messageClass, creatorClass);
  }

  public static Message cannotInstanciateFinder(String serviceFinder) {
    return factory.createMessage(BUNDLE_PATH, 73, serviceFinder);
  }

  public static Message failedToCreateObjectWith(String string, Object arg) {
    return factory.createMessage(BUNDLE_PATH, 74, string, arg);
  }

  public static Message objectNotSetInService(Object object, Object service) {
    return factory.createMessage(BUNDLE_PATH, 75, object, service);
  }

  public static Message objectNotFound(String type, String object) {
    return CoreMessages.objectNotFound(type + ": " + object);
  }

  public static Message failedToSetPropertiesOn(String string) {
    return factory.createMessage(BUNDLE_PATH, 83, string);
  }

  public static Message failedToCreateConnectorFromUri(EndpointURI uri) {
    return factory.createMessage(BUNDLE_PATH, 84, uri);
  }

  public static Message failedToRecevieWithTimeout(Object endpoint, long timeout) {
    return factory.createMessage(BUNDLE_PATH, 93, endpoint, String.valueOf(timeout));
  }

  public static Message schemeNotCompatibleWithConnector(String scheme, Class<?> expectedClass) {
    return factory.createMessage(BUNDLE_PATH, 115, scheme, expectedClass);
  }

  public static Message endpointNotFound(String endpoint) {
    return factory.createMessage(BUNDLE_PATH, 126, endpoint);
  }

  public static Message failedToDispatchToReplyto(ImmutableEndpoint endpoint) {
    return factory.createMessage(BUNDLE_PATH, 128, endpoint);
  }

  public static Message connectorSchemeIncompatibleWithEndpointScheme(Object expected, Object actual) {
    return factory.createMessage(BUNDLE_PATH, 206, expected, actual);
  }

  public static Message failedToSetProxyOnService(Object proxy, Class<?> routerClass) {
    return factory.createMessage(BUNDLE_PATH, 217, proxy, routerClass);
  }

  public static Message mustSetMethodNamesOnBinding() {
    return factory.createMessage(BUNDLE_PATH, 218);
  }

  public static Message cannotFindBindingForMethod(String name) {
    return factory.createMessage(BUNDLE_PATH, 219, name);
  }

  public static Message moreThanOneConnectorWithProtocol(String protocol, String connectors) {
    return factory.createMessage(BUNDLE_PATH, 221, protocol, connectors);
  }

  public static Message failedToGetOutputStream() {
    return factory.createMessage(BUNDLE_PATH, 223);
  }

  public static Message noDelegateClassAndMethodProvidedForNoArgsWrapper() {
    return factory.createMessage(BUNDLE_PATH, 225);
  }

  public static Message noDelegateClassIfDelegateInstanceSpecified() {
    return factory.createMessage(BUNDLE_PATH, 226);
  }

  public static Message noServiceTransportDescriptor(String protocol) {
    return factory.createMessage(BUNDLE_PATH, 227, protocol);
  }

  public static Message unrecognisedServiceType(ServiceType type) {
    return factory.createMessage(BUNDLE_PATH, 229, type);
  }

  public static Message serviceFinderCantFindService(String name) {
    return factory.createMessage(BUNDLE_PATH, 230, name);
  }

  public static Message outboundRouterMustUseOutboudEndpoints(OutboundRouter router, ImmutableEndpoint endpoint) {
    return factory.createMessage(BUNDLE_PATH, 233, endpoint, router);
  }

  public static Message exceptionListenerMustUseOutboundEndpoint(AbstractExceptionListener exceptionListener,
                                                                 ImmutableEndpoint endpoint) {
    return factory.createMessage(BUNDLE_PATH, 235, endpoint, exceptionListener);
  }

  public static Message failedToCreateProxyFor(Object target) {
    return factory.createMessage(BUNDLE_PATH, 270, target);
  }

  public static Message splitMessageNoEndpointMatch(List<?> endpoints, Object messagePart) {
    return factory.createMessage(BUNDLE_PATH, 275, StringMessageUtils.toString(endpoints), messagePart);
  }

  private static String getEndpointDescription(InboundEndpoint endpoint) {
    String endpointString = endpoint.getName();
    if (endpointString == null) {
      endpointString = endpoint.getEndpointURI().getUri().toString();
    }
    return endpointString;
  }

  public static Message failedToStartInboundEndpoint(InboundEndpoint endpoint) {
    return factory.createMessage(BUNDLE_PATH, 312, getEndpointDescription(endpoint));
  }

  public static Message failedToStopInboundEndpoint(InboundEndpoint endpoint) {
    return factory.createMessage(BUNDLE_PATH, 313, getEndpointDescription(endpoint));
  }

  public static Message exchangePatternForEndpointNotSupported(MessageExchangePattern mep, String direction,
                                                               EndpointURI endpointURI) {
    return factory.createMessage(BUNDLE_PATH, 323, mep.name(), direction, endpointURI);
  }

  public static Message dynamicEndpointURIsCannotBeUsedOnInbound() {
    return factory.createMessage(BUNDLE_PATH, 326);
  }

  public static Message dynamicEndpointsMustSpecifyAScheme() {
    return factory.createMessage(BUNDLE_PATH, 327);
  }

}
