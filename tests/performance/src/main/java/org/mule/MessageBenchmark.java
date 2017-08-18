/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.message.InternalMessage.Builder;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class MessageBenchmark extends AbstractBenchmark {

  private Message message;
  private Message messageWith20Properties;
  private Message messageWith100Properties;

  @Setup
  public void setup() throws Exception {
    message = createMuleMessage();
    messageWith20Properties = createMuleMessageWithProperties(20);
    messageWith100Properties = createMuleMessageWithProperties(100);
  }

  @Benchmark
  public Message createMessage() {
    return of(PAYLOAD);
  }

  @Benchmark
  public Message createMessageWithDataType() {
    return Message.builder().value(PAYLOAD).mediaType(MediaType.TEXT).build();
  }

  @Benchmark
  public Message copyMessage() {
    return Message.builder(message).build();
  }

  @Benchmark
  public Message copyMessageWith20Properties() {
    return Message.builder(messageWith20Properties).build();
  }

  @Benchmark
  public Message copyMessageWith100Properties() {
    return Message.builder(messageWith100Properties).build();
  }

  @Benchmark
  public Message mutateMessagePayload() {
    return Message.builder(message).value(VALUE).build();
  }

  @Benchmark
  public Message mutateMessagePayloadWithDataType() {
    return Message.builder(message).value(VALUE).mediaType(MediaType.TEXT).build();
  }

  @Benchmark
  public Message addMessageProperty() {
    return InternalMessage.builder(message).addOutboundProperty(KEY, VALUE).build();
  }

  @Benchmark
  public Message addMessagePropertyMessageWith20Properties() {
    return InternalMessage.builder(messageWith20Properties).addOutboundProperty(KEY, VALUE).build();
  }

  @Benchmark
  public Message addMessagePropertyMessageWith100Properties() {
    return InternalMessage.builder(messageWith100Properties).addOutboundProperty(KEY, VALUE).build();
  }

  @Benchmark
  public Message addMessagePropertyWithDataType() {
    return InternalMessage.builder(message).addOutboundProperty(KEY, VALUE, DataType.STRING).build();
  }

  @Benchmark
  public Message addRemoveMessageProperty() {
    Message temp = InternalMessage.builder(message).addOutboundProperty(KEY, VALUE).build();
    return InternalMessage.builder(temp).removeOutboundProperty(KEY).build();
  }

  @Benchmark
  public Message addRemoveMessagePropertyMessageWith20Properties() {
    Message temp = InternalMessage.builder(messageWith20Properties).addOutboundProperty(KEY, VALUE).build();
    return InternalMessage.builder(temp).removeOutboundProperty(KEY).build();
  }

  @Benchmark
  public Message addRemoveMessagePropertyMessageWith100Properties() {
    Message temp = InternalMessage.builder(messageWith100Properties).addOutboundProperty(KEY, VALUE).build();
    return InternalMessage.builder(temp).removeOutboundProperty(KEY).build();
  }

  @Benchmark
  public Message addRemoveMessagePropertyWithDataType() {
    Message temp = InternalMessage.builder(message).addOutboundProperty(KEY, VALUE, DataType.STRING).build();
    return InternalMessage.builder(temp).removeOutboundProperty(KEY).build();
  }

  @Benchmark
  public Message copyWith20PropertiesWrite1Outbound() {
    Builder builder = InternalMessage.builder(messageWith20Properties);
    for (int j = 1; j <= 10; j++) {
      builder.addInboundProperty("newKey" + j, "val");
    }
    return builder.build();
  }

  @Benchmark
  public Message copyWith20PopertiesWrite100Outbound() {
    Builder builder = InternalMessage.builder(messageWith20Properties);
    for (int j = 1; j <= 100; j++) {
      builder.addInboundProperty("newKey" + j, "val");
    }
    return builder.build();
  }

  @Benchmark
  public Message copyWith100PropertiesWrite1Outbound() {
    Builder builder = InternalMessage.builder(messageWith100Properties);
    for (int j = 1; j <= 10; j++) {
      builder.addInboundProperty("newKey" + j, "val");
    }
    return builder.build();
  }

  @Benchmark
  public Message copyWith100PropertiesWrite50Outbound() {
    Builder builder = InternalMessage.builder(messageWith100Properties);
    for (int j = 1; j <= 50; j++) {
      builder.addInboundProperty("newKey" + j, "val");
    }
    return builder.build();
  }

  private Message createMuleMessage() {
    return of(PAYLOAD);
  }

  private Message createMuleMessageWithProperties(int numProperties) {
    Builder builder = InternalMessage.builder().value(PAYLOAD);
    for (int i = 1; i <= numProperties; i++) {
      builder.addInboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    for (int i = 1; i <= numProperties; i++) {
      builder.addOutboundProperty("OuTBoUnDpRoPeRtYkEy" + i, "val");
    }
    return builder.build();
  }

}
