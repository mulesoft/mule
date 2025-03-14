/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformFailed;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.io.InputStream;
import java.nio.charset.Charset;

import jakarta.inject.Inject;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object representation
 */
public class ByteArrayToSerializable extends AbstractTransformer implements DiscoverableTransformer {

  private ObjectSerializer objectSerializer;

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
      throw new TransformerException(transformFailed("byte[]", "Object"), this, e);
    }
  }

  protected SerializationProtocol getSerializationProtocol() {
    return objectSerializer.getExternalProtocol();
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int priorityWeighting) {
    this.priorityWeighting = priorityWeighting;
  }

  public void setObjectSerializer(ObjectSerializer objectSerializer) {
    this.objectSerializer = objectSerializer;
  }

  @Inject
  public void setMuleContext(MuleContext context) {
    setObjectSerializer(context.getObjectSerializer());
  }

}
