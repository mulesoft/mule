/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object representation
 */
public class ByteArrayToSerializable extends AbstractTransformer implements DiscoverableTransformer {

  /**
   * Give core transformers a slightly higher priority
   */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  public ByteArrayToSerializable() {
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    SerializationProtocol externalProtocol = getSerializationProtocol();
    try {
      final Object result;
      if (src instanceof byte[]) {
        result = externalProtocol.deserialize((byte[]) src);
      } else if (src instanceof CursorStreamProvider) {
        result = externalProtocol.deserialize(((CursorStreamProvider) src).openCursor());
      } else {
        result = externalProtocol.deserialize((InputStream) src);
      }
      return result;
    } catch (Exception e) {
      throw new TransformerException(CoreMessages.transformFailed("byte[]", "Object"), this, e);
    }
  }

  protected SerializationProtocol getSerializationProtocol() {
    return muleContext.getObjectSerializer().getExternalProtocol();
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
