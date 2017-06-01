/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.config.i18n.CoreMessages.errorReadingStream;
import static org.mule.runtime.core.api.util.IOUtils.copyLarge;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.StringMessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>ObjectToString</code> transformer is useful for debugging. It will return human-readable output for various kinds of
 * objects. Right now, it is just coded to handle Map and Collection objects. Others will be added.
 */
public class ObjectToString extends AbstractTransformer implements DiscoverableTransformer {

  /** Give core transformers a slighty higher priority */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  public ObjectToString() {
    registerSourceType(DataType.OBJECT);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    registerSourceType(DataType.fromType(OutputHandler.class));
    setReturnDataType(DataType.STRING);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    String output;

    if (src instanceof CursorStreamProvider) {
      output = createStringFromInputStream(((CursorStreamProvider) src).openCursor(), outputEncoding);
    } else if (src instanceof InputStream) {
      output = createStringFromInputStream((InputStream) src, outputEncoding);
    } else if (src instanceof OutputHandler) {
      output = createStringFromOutputHandler((OutputHandler) src, outputEncoding);
    } else if (src instanceof byte[]) {
      output = createStringFromByteArray((byte[]) src, outputEncoding);
    } else {
      output = StringMessageUtils.toString(src);
    }

    return output;
  }

  protected String createStringFromInputStream(InputStream input, Charset outputEncoding)
      throws TransformerException {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      copyLarge(input, outputStream);
      return outputStream.toString(outputEncoding.name());
    } catch (IOException e) {
      throw new TransformerException(errorReadingStream(), e);
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        logger.warn("Could not close stream", e);
      }
    }
  }

  protected String createStringFromOutputHandler(OutputHandler handler, Charset outputEncoding)
      throws TransformerException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      handler.write(getCurrentEvent(), bytes);
      return bytes.toString(outputEncoding.name());
    } catch (IOException e) {
      throw new TransformerException(this, e);
    }
  }

  protected String createStringFromByteArray(byte[] bytes, Charset outputEncoding)
      throws TransformerException {
    return new String(bytes, outputEncoding);
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int priorityWeighting) {
    this.priorityWeighting = priorityWeighting;
  }
}
