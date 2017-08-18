/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;
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
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    registerSourceType(DataType.fromType(Serializable.class));
    setReturnDataType(DataType.fromType(OutputHandler.class));
  }

  @Override
  public Object doTransform(final Object src, final Charset encoding) throws TransformerException {
    if (src instanceof String) {
      return (OutputHandler) (event, out) -> out.write(((String) src).getBytes(encoding));
    } else if (src instanceof byte[]) {
      return (OutputHandler) (event, out) -> out.write((byte[]) src);
    } else if (src instanceof CursorStreamProvider) {
      return handleInputStream(((CursorStreamProvider) src).openCursor());
    } else if (src instanceof InputStream) {
      return handleInputStream((InputStream) src);
    } else if (src instanceof Serializable) {
      return (OutputHandler) (event, out) -> muleContext.getObjectSerializer().getExternalProtocol().serialize(src, out);
    } else {
      throw new TransformerException(I18nMessageFactory
          .createStaticMessage("Unable to convert " + src.getClass() + " to OutputHandler."));
    }
  }

  private OutputHandler handleInputStream(InputStream is) {
    return (event, out) -> {
      try {
        IOUtils.copyLarge(is, out);
      } finally {
        is.close();
      }
    };
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
