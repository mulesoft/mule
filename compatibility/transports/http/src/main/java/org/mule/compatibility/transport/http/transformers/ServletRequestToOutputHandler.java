/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;

/**
 * Adds support for converting a {@link javax.servlet.http.HttpServletRequest} into an
 * {@link org.mule.api.transport.OutputHandler}
 */
public class ServletRequestToOutputHandler extends AbstractTransformer implements DiscoverableTransformer {

  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

  public ServletRequestToOutputHandler() {
    registerSourceType(DataType.fromType(HttpServletRequest.class));
    setReturnDataType(DataType.fromType(OutputHandler.class));
  }

  @Override
  public Object doTransform(final Object src, Charset encoding) throws TransformerException {
    return (OutputHandler) (event, out) -> {
      InputStream is = ((HttpServletRequest) src).getInputStream();
      try {
        IOUtils.copyLarge(is, out);
      } finally {
        is.close();
      }
    };
  }

  /**
   * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
   *
   * @return the priority weighting for this transformer. This is a value between {@link #MIN_PRIORITY_WEIGHTING} and
   *         {@link #MAX_PRIORITY_WEIGHTING}.
   */
  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  /**
   * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
   *
   * @param weighting the priority weighting for this transformer. This is a value between {@link #MIN_PRIORITY_WEIGHTING} and
   *        {@link #MAX_PRIORITY_WEIGHTING}.
   */
  @Override
  public void setPriorityWeighting(int weighting) {
    priorityWeighting = weighting;
  }
}
