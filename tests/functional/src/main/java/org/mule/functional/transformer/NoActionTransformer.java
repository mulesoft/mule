/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.transformer;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.nio.charset.Charset;

/**
 * <code>NoActionTransformer</code> doesn't do any transformation on the source object and returns the source as the result. This
 * can be used to overload the default transform for an endpoint.
 */
@Deprecated
public final class NoActionTransformer extends AbstractTransformer {

  public NoActionTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    return src;
  }

  @Override
  public boolean isAcceptNull() {
    return true;
  }

}
