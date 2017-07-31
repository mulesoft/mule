/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object representation
 */
public class StringToBoolean extends AbstractTransformer implements DiscoverableTransformer {

  private static Map<String, Boolean> MAPPING = ImmutableMap.<String, Boolean>builder().put("true", TRUE).put("false", FALSE)
      .put("yes", TRUE).put("no", FALSE).put("1", TRUE).put("0", FALSE).build();

  /**
   * Give core transformers a slightly higher priority
   */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  public StringToBoolean() {
    registerSourceType(DataType.STRING);
    setReturnDataType(DataType.BOOLEAN);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    if (src == null) {
      if (isAllowNullReturn()) {
        return null;
      } else {
        throw new TransformerException(createStaticMessage("Unable to transform null to a primitive"));
      }
    } else {
      String value = ((String) src).toLowerCase().trim();
      Boolean transformed = MAPPING.get(value);
      if (transformed != null) {
        return transformed;
      } else {
        throw new TransformerException(createStaticMessage(format("Cannot transform String '%s' to boolean. Valid types are: [%s]",
                                                                  value, Joiner.on(", ").join(MAPPING.keySet()))));
      }
    }
  }

  @Override
  public void setReturnDataType(DataType type) {
    if (!Boolean.class.isAssignableFrom(type.getType())) {
      throw new IllegalArgumentException("This transformer only supports Boolean return types.");
    } else {
      super.setReturnDataType(type);
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
