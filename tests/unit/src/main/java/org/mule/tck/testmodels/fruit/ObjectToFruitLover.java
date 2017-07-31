/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.nio.charset.Charset;

public class ObjectToFruitLover extends AbstractTransformer {

  public ObjectToFruitLover() {
    this.setReturnDataType(DataType.fromType(FruitLover.class));
    this.registerSourceType(DataType.STRING);
    this.registerSourceType(DataType.fromType(FruitLover.class));
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    if (src instanceof FruitLover) {
      return src;
    } else {
      return new FruitLover((String) src);
    }
  }

}
