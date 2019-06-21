/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;

import java.nio.charset.Charset;

/**
 * A ByteArray to InputStream transformer
 */
public class ByteArrayToInputStream extends AbstractTransformer implements DiscoverableTransformer {

  private int priorityWeighting = MAX_PRIORITY_WEIGHTING;
  private CompositeConverter delegate = new CompositeConverter(new ByteArrayToObject(), new ObjectToInputStream());

  public ByteArrayToInputStream() {
    registerSourceType(BYTE_ARRAY);
    setReturnDataType(INPUT_STREAM);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Object doTransform(Object src, Charset enc) throws TransformerException {
    return delegate.transform(src, enc);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPriorityWeighting(int weighting) {
    this.priorityWeighting = weighting;
  }
}
