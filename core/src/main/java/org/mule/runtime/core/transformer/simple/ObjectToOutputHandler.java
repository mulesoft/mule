/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

/** <code>ObjectToOutputHandler</code> converts a byte array into a String. */
public class ObjectToOutputHandler extends AbstractTransformer implements DiscoverableTransformer {

  /** Give core transformers a slighty higher priority */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  public ObjectToOutputHandler() {
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.fromType(Serializable.class));
    setReturnDataType(DataType.fromType(OutputHandler.class));
  }

  @Override
  public Object doTransform(final Object src, final Charset encoding) throws TransformerException {
    if (src instanceof String) {
      return new OutputHandler() {

        @Override
        public void write(MuleEvent event, OutputStream out) throws IOException {
          out.write(((String) src).getBytes(encoding));
        }
      };
    } else if (src instanceof byte[]) {
      return new OutputHandler() {

        @Override
        public void write(MuleEvent event, OutputStream out) throws IOException {
          out.write((byte[]) src);
        }
      };
    } else if (src instanceof InputStream) {
      return new OutputHandler() {

        @Override
        public void write(MuleEvent event, OutputStream out) throws IOException {
          InputStream is = (InputStream) src;
          try {
            IOUtils.copyLarge(is, out);
          } finally {
            is.close();
          }
        }
      };
    } else if (src instanceof Serializable) {
      return new OutputHandler() {

        @Override
        public void write(MuleEvent event, OutputStream out) throws IOException {
          muleContext.getObjectSerializer().serialize(src, out);
        }
      };
    } else {
      throw new TransformerException(MessageFactory
          .createStaticMessage("Unable to convert " + src.getClass() + " to OutputHandler."));
    }
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
