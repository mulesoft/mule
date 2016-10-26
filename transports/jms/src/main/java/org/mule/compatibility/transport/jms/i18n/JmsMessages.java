/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.i18n;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;

public class JmsMessages extends I18nMessageFactory {

  private static final JmsMessages factory = new JmsMessages();

  private static final String BUNDLE_PATH = getBundlePath("jms");

  public static I18nMessage connectorDoesNotSupportSyncReceiveWhenTransacted() {
    return factory.createMessage(BUNDLE_PATH, 2);
  }

  public static I18nMessage sessionShouldBeTransacted() {
    return factory.createMessage(BUNDLE_PATH, 4);
  }

  public static I18nMessage sessionShouldNotBeTransacted() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }

  public static I18nMessage noMessageBoundForAck() {
    return factory.createMessage(BUNDLE_PATH, 6);
  }

  public static I18nMessage messageMarkedForRedelivery(InternalMessage message) {
    String messageDescription = (message == null) ? "[null message]" : message.toString();
    return factory.createMessage(BUNDLE_PATH, 7, messageDescription);
  }

  public static I18nMessage messageMarkedForRedelivery(Event event) {
    return messageMarkedForRedelivery(event.getMessage());
  }

  public static I18nMessage failedToCreateAndDispatchResponse(Object object) {
    return factory.createMessage(BUNDLE_PATH, 8, ObjectUtils.toString(object, "null"));
  }

  public static I18nMessage tooManyRedeliveries(String messageId, int times, int maxRedelivery, ImmutableEndpoint endpoint) {
    return factory.createMessage(BUNDLE_PATH, 11, messageId, times, maxRedelivery, endpoint.getEndpointURI(),
                                 endpoint.getConnector().getName());
  }

  public static I18nMessage invalidResourceType(Class<?> expectedClass, Object object) {
    Class<?> actualClass = null;
    if (object != null) {
      actualClass = object.getClass();
    }

    return factory.createMessage(BUNDLE_PATH, 12, StringMessageUtils.toString(expectedClass),
                                 StringMessageUtils.toString(actualClass));
  }

  public static I18nMessage checkTransformer(String string, Class<?> class1, String name) {
    return factory.createMessage(BUNDLE_PATH, 13, string, ClassUtils.getSimpleName(class1.getClass()), name);
  }

  public static I18nMessage noConnectionFactoryConfigured() {
    return factory.createMessage(BUNDLE_PATH, 14);
  }

  public static I18nMessage errorInitializingJndi() {
    return factory.createMessage(BUNDLE_PATH, 15);
  }

  public static I18nMessage errorCreatingConnectionFactory() {
    return factory.createMessage(BUNDLE_PATH, 16);
  }

  public static I18nMessage errorMuleMqJmsSpecification() {
    return factory.createMessage(BUNDLE_PATH, 17);
  }
}
