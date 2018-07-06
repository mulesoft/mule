/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.util.Arrays.copyOfRange;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.api.util.IOUtils.copyLarge;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamConsumerProcessor extends AbstractComponent implements Processor {

  private static final Logger logger = LoggerFactory.getLogger(StreamConsumerProcessor.class);

  private boolean chunked = false;
  private byte[] bytes = new byte[1024];

  public StreamConsumerProcessor(boolean chunked) {
    this.chunked = chunked;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    InputStream stream;
    Object payload = event.getMessage().getPayload().getValue();
    if (payload instanceof CursorStreamProvider) {
      stream = ((CursorStreamProvider) payload).openCursor();
    } else if (payload instanceof InputStream) {
      stream = (InputStream) payload;
    } else {
      throw new DefaultMuleException("Cannot consume a non stream payload.");
    }
    byte[] result = new byte[] {};
    try {
      if (chunked) {
        int read = 0;
        int prevRead = 0;
        while (read != -1) {
          prevRead = read;
          read = stream.read(bytes);
        }
        if (prevRead > 0) {
          result = copyOfRange(bytes, 0, prevRead);
        }
      } else {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copyLarge(stream, byteArrayOutputStream);
        result = byteArrayOutputStream.toByteArray();
      }
    } catch (IOException e) {
      throw new DefaultMuleException("Error attempting to consume payload: " + e.getMessage());
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        logger.warn("Failure closing stream: " + e.getMessage());
      }
    }
    return CoreEvent.builder(event).message(Message.of(result)).build();
  }

  @Override
  public ProcessingType getProcessingType() {
    return IO_RW;
  }
}
