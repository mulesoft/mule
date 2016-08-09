/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.BeanUtils;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Conversts a simple bean object to a Map. every property on the bean will become an entry in the result {@link java.util.Map}.
 * Note that only exposed bean properties with getter and setter methods will be added to the map.
 */
public class BeanToMap extends AbstractTransformer implements DiscoverableTransformer {

  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

  public BeanToMap() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.fromType(Map.class));
  }

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    Map result = BeanUtils.describeBean(src);
    return result;
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int weighting) {
    priorityWeighting = weighting;
  }


}
