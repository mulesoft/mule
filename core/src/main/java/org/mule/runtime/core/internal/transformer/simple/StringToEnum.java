/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.nio.charset.Charset;

/**
 * Transforms a {@link String} to an {@link Enum} of a class specified through at construction.
 *
 * @since 4.0
 */
public class StringToEnum extends AbstractTransformer implements DiscoverableTransformer {

  private final Class<? extends Enum> enumClass;
  private int weighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;


  /**
   * Creates a new instance
   *
   * @param enumClass the class of the transformed values
   */
  public StringToEnum(Class<? extends Enum> enumClass) {
    checkArgument(enumClass != null, "enumClass cannot be null");
    this.enumClass = enumClass;

    registerSourceType(DataType.fromType(String.class));
    setReturnDataType(DataType.fromType(enumClass));
    setName(format("StringTo%sTransformer", enumClass.getName()));
  }

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    try {
      return Enum.valueOf(enumClass, ((String) src));
    } catch (Exception e) {
      throw new TransformerException(createStaticMessage(format("Could not transform value '%s' to an enum of type %s", src,
                                                                enumClass.getName())),
                                     e);
    }
  }

  @Override
  public int getPriorityWeighting() {
    return weighting;
  }

  @Override
  public void setPriorityWeighting(int weighting) {
    this.weighting = weighting;
  }
}
