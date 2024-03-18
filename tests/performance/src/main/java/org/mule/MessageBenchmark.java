/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.api.message.Message.of;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class MessageBenchmark extends AbstractBenchmark {

  private Message message;

  @Setup
  public void setup() throws Exception {
    message = createMuleMessage();
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
  public Message mutateMessagePayload() {
    return Message.builder(message).value(VALUE).build();
  }

  @Benchmark
  public Message mutateMessagePayloadWithDataType() {
    return Message.builder(message).value(VALUE).mediaType(MediaType.TEXT).build();
  }

  private Message createMuleMessage() {
    return of(PAYLOAD);
  }

}
