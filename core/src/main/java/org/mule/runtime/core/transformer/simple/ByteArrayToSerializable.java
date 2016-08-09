/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.AbstractTransformer;

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
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    ObjectSerializer serializer = muleContext.getObjectSerializer();
    try {
      final Object result;
      if (src instanceof byte[]) {
        result = serializer.deserialize((byte[]) src);
      } else {
        result = serializer.deserialize((InputStream) src);
      }
      return result;
    } catch (Exception e) {
      throw new TransformerException(CoreMessages.transformFailed("byte[]", "Object"), this, e);
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
