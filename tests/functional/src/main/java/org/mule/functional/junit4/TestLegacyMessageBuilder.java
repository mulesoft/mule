/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.functional.junit4.TestLegacyMessageUtils.LEGACY_MESSAGE_API_ERROR;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.message.ExceptionPayload;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Builds messages supporting the legacy message API
 *
 * @deprecated tests should not access properties, attachments or exception payload using the old API.
 */
@Deprecated
public class TestLegacyMessageBuilder implements Message.CollectionBuilder {

  private Message.Builder builder;

  public TestLegacyMessageBuilder() {
    builder = Message.builder().nullValue();
  }

  /**
   * Create a new builder initialized from an existent message
   *
   * @param message message to initialize the builder state. Non null.
   */
  public TestLegacyMessageBuilder(Message message) {
    builder = Message.builder(message);
  }

  @Override
  public TestLegacyMessageBuilder payload(TypedValue<?> typedValue) {
    builder.payload(typedValue);
    return this;
  }

  @Override
  public TestLegacyMessageBuilder nullValue() {
    builder.nullValue();

    return this;
  }

  @Override
  public TestLegacyMessageBuilder value(Object value) {
    builder.value(value);

    return this;
  }

  @Override
  public TestLegacyMessageBuilder streamValue(Iterator value, Class<?> itemType) {
    builder.streamValue(value, itemType);

    return this;
  }

  @Override
  public TestLegacyMessageBuilder collectionValue(Collection value, Class<?> itemType) {
    builder.collectionValue(value, itemType);

    return this;
  }

  @Override
  public TestLegacyMessageBuilder collectionValue(Object[] value) {
    builder.collectionValue(value);

    return this;
  }

  @Override
  public TestLegacyMessageBuilder itemMediaType(MediaType mediaType) {
    throw new UnsupportedOperationException("This method is not supported in TestLegacyMessageBuilder. Use org.mule.runtime.api.message.Message.builder()");
  }

  @Override
  public TestLegacyMessageBuilder mediaType(MediaType mediaType) {
    checkInternalState();
    builder.mediaType(mediaType);
    return this;
  }

  @Override
  public TestLegacyMessageBuilder attributes(TypedValue<?> typedValue) {
    builder.attributes(typedValue);
    return this;
  }

  @Override
  public TestLegacyMessageBuilder nullAttributesValue() {
    builder.nullAttributesValue();
    return this;
  }

  @Override
  public TestLegacyMessageBuilder attributesValue(Object value) {
    checkInternalState();
    builder.attributesValue(value);
    return this;
  }

  @Override
  public TestLegacyMessageBuilder attributesMediaType(MediaType mediaType) {
    checkInternalState();
    builder.attributesMediaType(mediaType);
    return this;
  }

  /**
   * @param key
   * @param value
   * @return
   * @throws {@link IllegalStateException} if there is any problem accessing the legacy message API using reflection
   */
  @Deprecated
  public TestLegacyMessageBuilder addOutboundProperty(String key, Serializable value) {
    checkInternalState();
    try {
      Method method = builder.getClass().getMethod("addOutboundProperty", String.class, Serializable.class);
      method.setAccessible(true);
      method.invoke(builder, key, value);
    } catch (Exception e) {
      throw new IllegalStateException(LEGACY_MESSAGE_API_ERROR, e);
    }

    return this;
  }

  /**
   * @param inboundProperties
   * @return
   * @throws {@link IllegalStateException} if there is any problem accessing the legacy message API using reflection
   */
  @Deprecated
  public TestLegacyMessageBuilder inboundProperties(Map<String, Serializable> inboundProperties) {
    checkInternalState();
    try {
      Method method = builder.getClass().getMethod("inboundProperties", Map.class);
      method.setAccessible(true);
      method.invoke(builder, inboundProperties);
    } catch (Exception e) {
      throw new IllegalStateException(LEGACY_MESSAGE_API_ERROR, e);
    }

    return this;
  }

  /**
   * @param outboundProperties
   * @return
   * @throws {@link IllegalStateException} if there is any problem accessing the legacy message API using reflection
   */
  @Deprecated
  public TestLegacyMessageBuilder outboundProperties(Map<String, Serializable> outboundProperties) {
    checkInternalState();
    try {
      Method method = builder.getClass().getMethod("outboundProperties", Map.class);
      method.setAccessible(true);
      method.invoke(builder, outboundProperties);
    } catch (Exception e) {
      throw new IllegalStateException(LEGACY_MESSAGE_API_ERROR, e);
    }

    return this;
  }

  /**
   * @param exceptionPayload
   * @return this builder.
   * @throws {@link IllegalStateException} if there is any problem accessing the legacy message API using reflection
   */
  public TestLegacyMessageBuilder exceptionPayload(ExceptionPayload exceptionPayload) {
    checkInternalState();
    try {
      Method method = builder.getClass().getMethod("exceptionPayload", ExceptionPayload.class);
      method.setAccessible(true);
      method.invoke(builder, exceptionPayload);
    } catch (Exception e) {
      throw new IllegalStateException(LEGACY_MESSAGE_API_ERROR, e);
    }
    return this;
  }

  @Override
  public Message build() {
    checkInternalState();
    return builder.build();
  }

  private void checkInternalState() {
    if (builder == null) {
      throw new IllegalStateException("Payload must be configured on the builder before calling any other method");
    }
  }
}
